import React, { useState, useEffect } from 'react';
import { Clock, Coffee, Square, Home } from 'lucide-react';
import { Card, Button } from '../../components';
import '../../pages/main.css';
import './dashboard.css';
import { workLogService, leaveLogService } from '../../services';

const formatDuration = (ms) => {
    if (!ms || ms < 0) return '00:00:00';
    const totalSeconds = Math.floor(ms / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
};

const parseDurationToMs = (timeString) => {
    if (!timeString || typeof timeString !== 'string') return 0;
    const parts = timeString.split(':');
    if (parts.length !== 3) return 0;
    const hours = parseInt(parts[0], 10);
    const minutes = parseInt(parts[1], 10);
    const seconds = parseInt(parts[2], 10);
    return ((hours * 3600) + (minutes * 60) + seconds) * 1000;
};

const TimeTracker = () => {
    const [now, setNow] = useState(new Date());
    const [isWorking, setIsWorking] = useState(false);
    const [isOnBreak, setIsOnBreak] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [startTime, setStartTime] = useState(null);

    const [isHO, setIsHO] = useState(false);

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 1000);
        return () => clearInterval(timer);
    }, []);

    const syncActiveTime = async () => {
        try {
            const timeStr = await workLogService.getActiveTime();
            const workedMs = parseDurationToMs(timeStr);
            const calculatedStartTime = new Date(Date.now() - workedMs);
            setStartTime(calculatedStartTime);
        } catch {
            setStartTime(new Date());
        }
    };

    useEffect(() => {
        const fetchStatus = async () => {
            try {
                const [workStatus, breakStatus] = await Promise.all([
                    workLogService.getStatus(),
                    leaveLogService.getStatus()
                ]);

                setIsWorking(workStatus);
                setIsOnBreak(breakStatus);
                setIsHO(false);

                if (workStatus) {
                    await syncActiveTime();
                } else {
                    setStartTime(null);
                }
            } catch (error) {
                console.error(error);
            } finally {
                setIsLoading(false);
            }
        };
        fetchStatus();
    }, []);

    const handleToggleWork = async () => {
        if (isLoading) return;
        setIsLoading(true);
        try {
            if (isWorking) {
                if (isOnBreak) {
                    await leaveLogService.endLeave();
                    setIsOnBreak(false);
                }

                await workLogService.endWork();

                setIsWorking(false);
                setIsHO(false);
                setStartTime(null);
                alert("Zakończono czas pracy");
            } else {
                const request = { locationType: "OFFICE" };
                await workLogService.startWork(request);

                const nowStart = new Date();
                setIsWorking(true);
                setIsHO(false);
                setStartTime(nowStart);
            }
        } catch (error) {
            console.error(error);
            alert(error.message);
        } finally {
            setIsLoading(false);
        }
    };

    const handleToggleWorkHO = async () => {
        if (isLoading || isWorking) return;
        setIsLoading(true);

        try {
            const request = { locationType: "HOME_OFFICE" };
            await workLogService.startWork(request);

            setIsWorking(true);
            setIsHO(true);
            setStartTime(new Date());
        } catch (error) {
            console.error(error);
            alert(error.message);
        } finally {
            setIsLoading(false);
        }
    };

    const handleToggleBreak = async () => {
        if (isLoading || !isWorking) return;
        setIsLoading(true);

        try {
            if (isHO) {
                setIsOnBreak(!isOnBreak);
            } else {
                if (isOnBreak) {
                    await leaveLogService.endLeave();
                    setIsOnBreak(false);
                    await syncActiveTime();
                } else {
                    await leaveLogService.startLeave();
                    setIsOnBreak(true);
                }
            }
        } catch (error) {
            console.error(error);
            alert(error.message);
        } finally {
            setIsLoading(false);
        }
    };

    const timeString = now.toLocaleTimeString('pl-PL', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });

    const elapsedString = startTime ? formatDuration(now - new Date(startTime)) : '00:00:00';

    return (
        <Card>
            {!isWorking ? (
                <div style={{ display: 'flex', gap: '15px' }}>
                    <button
                        className={`time-display time-display-btn ${isLoading ? 'loading' : 'stopped'}`}
                        style={{ flex: 1 }}
                        onClick={handleToggleWork}
                        disabled={isLoading}
                    >
                        <div className="time-header">
                            <Clock size={20} />
                            <span className="time-header-text">
                                {isLoading ? "ŁADOWANIE..." : "ROZPOCZNIJ PRACĘ"}
                            </span>
                        </div>
                        <div className="time-clock">{timeString}</div>
                    </button>

                    <button
                        className={`time-display time-display-btn ${isLoading ? 'loading' : 'stopped'}`}
                        style={{ flex: 1 }}
                        onClick={handleToggleWorkHO}
                        disabled={isLoading}
                    >
                        <div className="time-header">
                            <Home size={20} />
                            <span className="time-header-text">
                                {isLoading ? "ŁADOWANIE..." : "HO Start"}
                            </span>
                        </div>
                        <div className="time-clock">{timeString}</div>
                    </button>
                </div>
            ) : (
                <button
                    className={`time-display time-display-btn ${isLoading ? 'loading' : 'working'}`}
                    onClick={handleToggleWork}
                    disabled={isLoading}
                >
                    <div className="time-header">
                        <Square size={20} />
                        <span className="time-header-text">
                            {isLoading ? "ŁADOWANIE..." : "ZAKOŃCZ PRACĘ"}
                        </span>
                    </div>

                    <div className="time-clock">
                        {isOnBreak ? "W TRAKCIE PRZERWY" : elapsedString}
                    </div>
                </button>
            )}

            <div className="time-actions-wrapper">
                <Button
                    className={`btn-break-toggle ${isOnBreak ? 'btn-break-active' : ''}`}
                    variant={isOnBreak ? "primary" : "outline"}
                    disabled={!isWorking || isLoading}
                    onClick={handleToggleBreak}
                >
                    <Coffee size={18} />
                    {isOnBreak ? "ZAKOŃCZ PRZERWĘ" : "PRZERWA"}
                </Button>
            </div>
        </Card>
    );
}

export default TimeTracker;
