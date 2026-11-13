import React, { useState, useEffect, useRef } from 'react';
import { Clock, Coffee, Square, Home, XCircle } from 'lucide-react';
import { Card, Button } from '../../components';
import { useLanguage } from '../../context/LanguageContext';
import '../../pages/main.css';
import './dashboard.css';
import { workLogService, leaveLogService } from '../../services';

const EIGHT_HOURS_MS = 8 * 60 * 60 * 1000;
const FIFTEEN_MINUTES_MS = 900000;
const ONE_HOUR_MS = 60 * 60 * 1000;
const TICK_WIDTH_PX = 5;

const formatDuration = (ms) => {
    if (!ms || ms < 0) return '00:00:00';
    const totalSeconds = Math.floor(ms / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
};

const formatHM = (ms) => {
    const totalMinutes = Math.max(0, Math.floor(ms / 60000));
    const h = Math.floor(totalMinutes / 60);
    const m = totalMinutes % 60;
    return `${h}h ${String(m).padStart(2, '0')}m`;
};

const parseDurationToMs = (timeString) => {
    if (!timeString || typeof timeString !== 'string') return 0;
    const parts = timeString.split(':');
    if (parts.length !== 3) return 0;
    const hours = parseInt(parts[0], 10) || 0;
    const minutes = parseInt(parts[1], 10) || 0;
    const seconds = parseInt(parts[2], 10) || 0;
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

    const boxRef = useRef(null);
    const activeRectRef = useRef(null);
    const [perimeter, setPerimeter] = useState(0);

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

    useEffect(() => {
        if (!isWorking || !activeRectRef.current) return undefined;
        const el = activeRectRef.current;
        const measure = () => {
            try {
                setPerimeter(el.getTotalLength());
            } catch {
                const rect = boxRef.current?.getBoundingClientRect();
                if (rect) setPerimeter(2 * (rect.width + rect.height) - 16);
            }
        };
        measure();
        if (typeof ResizeObserver === 'undefined' || !boxRef.current) return undefined;
        const observer = new ResizeObserver(measure);
        observer.observe(boxRef.current);
        return () => observer.disconnect();
    }, [isWorking]);

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

    const elapsedMs = startTime ? Math.max(0, now.getTime() - new Date(startTime).getTime()) : 0;
    const elapsedString = startTime ? formatDuration(elapsedMs) : '00:00:00';
    const isCancelable = isWorking && startTime && (elapsedMs <= FIFTEEN_MINUTES_MS);
    const remainingCancelTimeStr = isCancelable ? formatDuration(FIFTEEN_MINUTES_MS - elapsedMs).substring(3) : '';

    const ratio = EIGHT_HOURS_MS > 0 ? elapsedMs / EIGHT_HOURS_MS : 0;
    const percent = Math.min(100, Math.max(0, Math.round(ratio * 100))) || 0;
    const isComplete = elapsedMs >= EIGHT_HOURS_MS;
    const remainingMs = Math.max(0, EIGHT_HOURS_MS - elapsedMs);
    const strokeDashoffset = perimeter > 0 ? perimeter - (percent / 100) * perimeter : 0;

    const hourTicks = Math.round(EIGHT_HOURS_MS / ONE_HOUR_MS);

    const frameStateClass = isOnBreak ? 'paused' : isComplete ? 'overtime' : 'active';

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
                <div style={{ display: 'flex', flexDirection: 'column', width: '100%' }}>
                    <div className="work-frame-wrapper">
                        <div className={`work-frame-box ${frameStateClass}`} ref={boxRef}>
                            <svg className="work-frame-svg">
                                <rect
                                    className="work-frame-bg-stroke"
                                    x="2"
                                    y="2"
                                    rx="14"
                                    ry="14"
                                />
                                <rect
                                    ref={activeRectRef}
                                    className="work-frame-active-stroke"
                                    x="2"
                                    y="2"
                                    rx="14"
                                    ry="14"
                                    style={{
                                        strokeDasharray: perimeter || 800,
                                        strokeDashoffset: strokeDashoffset
                                    }}
                                />
                                {perimeter > 0 && hourTicks > 0 && (
                                    <rect
                                        className="work-frame-ticks"
                                        x="2"
                                        y="2"
                                        rx="14"
                                        ry="14"
                                        style={{
                                            strokeDasharray: `${TICK_WIDTH_PX} ${perimeter / hourTicks - TICK_WIDTH_PX}`,
                                            strokeDashoffset: TICK_WIDTH_PX / 2
                                        }}
                                    />
                                )}
                            </svg>

                            <div className="work-frame-content">
                                <div className={`work-time-display ${isComplete ? 'time-overtime' : ''}`}>
                                    {isOnBreak ? t('timeTracker.onBreak', 'W TRAKCIE PRZERWY') : elapsedString}
                                </div>

                                <span className={`work-progress-label ${isComplete ? 'label-overtime' : ''}`}>
                                    {isComplete
                                        ? `NADGODZINY (+${formatHM(elapsedMs - EIGHT_HOURS_MS)})`
                                        : `POSTĘP: ${percent}%`}
                                </span>

                                <button
                                    type="button"
                                    className="btn-end-work-compact"
                                    onClick={handleToggleWork}
                                    disabled={isLoading}
                                >
                                    <Square size={14} fill="currentColor" />
                                    <span>
                                        {isLoading ? t('common.loadingUpper', 'ŁADOWANIE...') : t('timeTracker.endWork', 'Zakończ pracę')}
                                    </span>
                                </button>
                            </div>
                        </div>

                        <div className="battery-info-row">
                            <div className="info-col">
                                <span className="label">Przepracowano:</span>
                                <span className="val">{formatHM(elapsedMs)}</span>
                            </div>
                            <div className="info-col">
                                <span className="label">{isComplete ? 'Nadgodziny:' : 'Do końca:'}</span>
                                <span className={`val ${isComplete ? 'complete' : ''}`}>
                                    {isComplete ? formatHM(elapsedMs - EIGHT_HOURS_MS) : formatHM(remainingMs)}
                                </span>
                            </div>
                        </div>
                    </div>

                    {isCancelable && (
                        <button
                            type="button"
                            onClick={handleCancelWork}
                            disabled={isLoading}
                            style={{
                                marginTop: '0.6rem',
                                backgroundColor: 'transparent',
                                border: 'none',
                                color: 'var(--text-muted, #888)',
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
};

export default TimeTracker;
