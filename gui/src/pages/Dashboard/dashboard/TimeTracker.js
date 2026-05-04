import React, { useState, useEffect } from 'react';
import { Clock, Coffee, Square } from 'lucide-react';
import { Card, Button } from '../../../components';
import '../../main.css';
import './dashboard.css';
import { workLogService } from '../../../services/workLogService';

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
    const [isLoading, setIsLoading] = useState(true);
    const [startTime, setStartTime] = useState(null);

    const userId = 1;

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 1000);
        return () => clearInterval(timer);
    }, []);

    useEffect(() => {
        const fetchStatus = async () => {
            try {
                const status = await workLogService.getStatus(userId);
                setIsWorking(status);

                if (status) {
                    try {
                        const timeStr = await workLogService.getActiveTime(userId);
                        const workedMs = parseDurationToMs(timeStr);
                        const calculatedStartTime = new Date(Date.now() - workedMs);
                        setStartTime(calculatedStartTime);
                    } catch {
                        setStartTime(new Date());
                    }
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
    }, [userId]);

    const handleToggleWork = async () => {
        if (isLoading) return;

        setIsLoading(true);
        try {
            if (isWorking) {
                await workLogService.endWork(userId);
                setIsWorking(false);
                setStartTime(null);
                alert("Zakończono czas pracy");
            } else {
                await workLogService.startWork(userId);
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

    const timeString = now.toLocaleTimeString('pl-PL', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });

    const elapsedString = startTime ? formatDuration(now - new Date(startTime)) : '00:00:00';

    return (
        <Card>
            <button
                className={`time-display ${isLoading ? 'loading' : ''} ${isWorking ? 'working' : 'stopped'}`}
                onClick={handleToggleWork}
                disabled={isLoading}
                style={{
                    width: '100%',
                    border: 'none',
                    cursor: isLoading ? 'wait' : 'pointer'
                }}
            >
                <div className="time-header">
                    {isWorking ? <Square size={20} /> : <Clock size={20} />}
                    <span className="time-header-text">
                        {isLoading ? "ŁADOWANIE..." : (isWorking ? "ZAKOŃCZ PRACĘ" : "ROZPOCZNIJ PRACĘ")}
                    </span>
                </div>

                <div className="time-clock">
                    {isWorking ? elapsedString : timeString}
                </div>
            </button>

            <div className="time-actions-grid" style={{ marginTop: '1rem' }}>
                <Button variant="outline" disabled={!isWorking}>
                    <Coffee size={18} /> PRZERWA
                </Button>
            </div>
        </Card>
    );
}

export default TimeTracker;
