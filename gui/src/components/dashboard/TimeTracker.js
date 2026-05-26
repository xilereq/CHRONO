import React, { useState, useEffect } from 'react';
import { Clock, Coffee, Square, Hospital, CircleEllipsis, UserLock, Microchip } from 'lucide-react';
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
    const [showBreakSelector, setShowBreakSelector] = useState(false);

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 1000);
        return () => clearInterval(timer);
    }, []);

    // Nowa funkcja pobierająca zsynchronizowany czas z serwera (bez czasu spędzonego na przerwie)
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
        setShowBreakSelector(false);
        try {
            if (isWorking) {
                if (isOnBreak) {
                    await leaveLogService.endLeave();
                    setIsOnBreak(false);
                }
                await workLogService.endWork();
                setIsWorking(false);
                setStartTime(null);
                alert("Zakończono czas pracy");
            } else {
                await workLogService.startWork();
                const nowStart = new Date();
                setIsWorking(true);
                setStartTime(nowStart);
            }
        } catch (error) {
            console.error(error);
            alert("Wystąpił błąd komunikacji z serwerem.");
        } finally {
            setIsLoading(false);
        }
    };

    const handleToggleBreak = async () => {
        if (isLoading || !isWorking) return;

        if (isOnBreak) {
            setIsLoading(true);
            try {
                await leaveLogService.endLeave();
                setIsOnBreak(false);

                // POPRAWKA: Po powrocie z przerwy synchronizujemy czas z backendem.
                // Dzięki temu "startTime" przesunie się do przodu, wymuszając na stoperze pominięcie przerwy.
                await syncActiveTime();

            } catch (error) {
                console.error(error);
                alert("Błąd podczas kończenia przerwy.");
            } finally {
                setIsLoading(false);
            }
        } else {
            setShowBreakSelector(!showBreakSelector);
        }
    };

    const startSpecificBreak = async (leaveType) => {
        setIsLoading(true);
        setShowBreakSelector(false);
        try {
            await leaveLogService.startLeave(leaveType);
            setIsOnBreak(true);
        } catch (error) {
            console.error(error);
            alert("Błąd podczas rozpoczynania przerwy.");
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
            <button
                className={`time-display time-display-btn ${isLoading ? 'loading' : ''} ${isWorking ? 'working' : 'stopped'}`}
                onClick={handleToggleWork}
                disabled={isLoading}
            >
                <div className="time-header">
                    {isWorking ? <Square size={20} /> : <Clock size={20} />}
                    <span className="time-header-text">
                        {isLoading ? "ŁADOWANIE..." : (isWorking ? "ZAKOŃCZ PRACĘ" : "ROZPOCZNIJ PRACĘ")}
                    </span>
                </div>

                <div className="time-clock">
                    {isOnBreak ? "W TRAKCIE PRZERWY" : (isWorking ? elapsedString : timeString)}
                </div>
            </button>

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

                {showBreakSelector && !isOnBreak && (
                    <div className="break-selector-panel">
                        <div className="break-selector-title">
                            Wybierz rodzaj przerwy:
                        </div>
                        <div className="break-selector-grid">
                            <Button
                                variant="outline"
                                className="btn-break-type"
                                onClick={() => startSpecificBreak('LUNCH')}
                            >
                                <Coffee size={16}/> LUNCH
                            </Button>
                            <Button
                                variant="outline"
                                className="btn-break-type"
                                onClick={() => startSpecificBreak('PRIVATE_LEAVE')}
                            >
                                <UserLock size={16}/> PRIVATE_LEAVE
                            </Button>
                            <Button
                                variant="outline"
                                className="btn-break-type"
                                onClick={() => startSpecificBreak('MEDICAL')}
                            >
                                <Hospital size={16}/> MEDICAL
                            </Button>
                            <Button
                                variant="outline"
                                className="btn-break-type"
                                onClick={() => startSpecificBreak('TECHNICAL')}
                            >
                                <Microchip size={16}/> TECHNICAL
                            </Button>
                            <Button
                                variant="outline"
                                className="btn-break-type full-width"
                                onClick={() => startSpecificBreak('OTHER')}
                            >
                                <CircleEllipsis size={16}/> OTHER
                            </Button>
                        </div>
                    </div>
                )}
            </div>
        </Card>
    );
}

export default TimeTracker;
