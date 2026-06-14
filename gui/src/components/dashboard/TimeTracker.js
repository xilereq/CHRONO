import React, { useState, useEffect } from 'react';
import { Clock, Coffee, Square, Home, XCircle } from 'lucide-react';
import { Card, Button } from '../../components';
import { useLanguage } from '../../context/LanguageContext';
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
    const { t } = useLanguage();
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
                alert(t('timeTracker.workEndedAlert', 'Zakończono czas pracy'));
            } else {
                const currentStatus = await workLogService.getStatus();
                if (currentStatus === true) {
                    setIsWorking(true);
                    await syncActiveTime();
                    throw new Error(t('timeTracker.workAlreadyStarted', 'Praca jest już rozpoczęta w systemie.'));
                }

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
            const currentStatus = await workLogService.getStatus();
            if (currentStatus === true) {
                setIsWorking(true);
                await syncActiveTime();
                throw new Error(t('timeTracker.workAlreadyStarted', 'Praca jest już rozpoczęta w systemie.'));
            }

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

    const handleCancelWork = async () => {
        if (isLoading) return;

        if (!window.confirm(t('timeTracker.cancelConfirm', 'Czy na pewno chcesz anulować to rozpoczęcie pracy? Ten wpis zostanie usunięty.'))) {
            return;
        }

        setIsLoading(true);
        try {
            await workLogService.cancelWork();
            setIsWorking(false);
            setIsHO(false);
            setIsOnBreak(false);
            setStartTime(null);
            alert(t('timeTracker.entryCancelledAlert', 'Anulowano wpis.'));
        } catch (error) {
            console.error(error);
            alert(error.message);
        } finally {
            setIsLoading(false);
        }
    };

    const timeString = now.toLocaleTimeString(t('common.locale', 'pl-PL'), {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });

    const elapsedMs = startTime ? now - new Date(startTime) : 0;
    const elapsedString = startTime ? formatDuration(elapsedMs) : '00:00:00';

    const FIFTEEN_MINUTES_MS = 900000;
    const isCancelable = isWorking && startTime && (elapsedMs <= FIFTEEN_MINUTES_MS);

    const remainingCancelTimeStr = isCancelable ? formatDuration(FIFTEEN_MINUTES_MS - elapsedMs).substring(3) : '';

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
                                {isLoading ? t('common.loadingUpper', 'ŁADOWANIE...') : t('timeTracker.startWork', 'ROZPOCZNIJ PRACĘ')}
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
                                {isLoading ? t('common.loadingUpper', 'ŁADOWANIE...') : t('timeTracker.hoStart', 'HO Start')}
                            </span>
                        </div>
                        <div className="time-clock">{timeString}</div>
                    </button>
                </div>
            ) : (
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                    <button
                        className={`time-display time-display-btn ${isLoading ? 'loading' : 'working'}`}
                        onClick={handleToggleWork}
                        disabled={isLoading}
                    >
                        <div className="time-header">
                            <Square size={20} />
                            <span className="time-header-text">
                                {isLoading ? t('common.loadingUpper', 'ŁADOWANIE...') : t('timeTracker.endWork', 'ZAKOŃCZ PRACĘ')}
                            </span>
                        </div>

                        <div className="time-clock">
                            {isOnBreak ? t('timeTracker.onBreak', 'W TRAKCIE PRZERWY') : elapsedString}
                        </div>
                    </button>

                    {isCancelable && (
                        <button
                            type="button"
                            onClick={handleCancelWork}
                            disabled={isLoading}
                            style={{
                                marginTop: '0.8rem',
                                backgroundColor: 'transparent',
                                border: 'none',
                                color: 'var(--text-muted)',
                                textDecoration: 'underline',
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: '5px',
                                fontSize: '0.85rem'
                            }}
                        >
                            <XCircle size={14} />
                            {t('timeTracker.mistakeCancel', 'Pomyłka? Anuluj wpis (')} {remainingCancelTimeStr} )
                        </button>
                    )}
                </div>
            )}

            <div className="time-actions-wrapper">
                <Button
                    className={`btn-break-toggle ${isOnBreak ? 'btn-break-active' : ''}`}
                    variant={isOnBreak ? "primary" : "outline"}
                    disabled={!isWorking || isLoading}
                    onClick={handleToggleBreak}
                >
                    <Coffee size={18} />
                    {isOnBreak ? t('timeTracker.endBreak', 'ZAKOŃCZ PRZERWĘ') : t('timeTracker.break', 'PRZERWA')}
                </Button>
            </div>
        </Card>
    );
}

export default TimeTracker;
