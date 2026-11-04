// WorkProgressBattery.jsx (lub WorkProgressFrame.jsx)
import React, { useRef, useState, useEffect } from 'react';
import { Square } from 'lucide-react';
import './dashboard.css';

const EIGHT_HOURS_MS = 8 * 60 * 60 * 1000;

const formatHM = (ms) => {
    const totalMinutes = Math.max(0, Math.floor(ms / 60000));
    const h = Math.floor(totalMinutes / 60);
    const m = totalMinutes % 60;
    return `${h}h ${String(m).padStart(2, '0')}m`;
};

const WorkProgressBattery = ({
                                 elapsedMs = 0,
                                 targetMs = EIGHT_HOURS_MS,
                                 isOnBreak = false,
                                 elapsedString = '00:00:00',
                                 onEndWork,
                                 isLoading = false,
                                 t = (k, def) => def
                             }) => {
    const boxRef = useRef(null);
    const [perimeter, setPerimeter] = useState(0);

    const safeTargetMs = targetMs > 0 ? targetMs : EIGHT_HOURS_MS;
    const ratio = safeTargetMs > 0 ? elapsedMs / safeTargetMs : 0;
    const percent = Math.min(100, Math.max(0, Math.round(ratio * 100))) || 0;
    const isComplete = elapsedMs >= safeTargetMs;
    const remainingMs = Math.max(0, safeTargetMs - elapsedMs);

    // Mierzymy obwód prostokąta, by idealnie dopasować animację kreski
    useEffect(() => {
        if (boxRef.current) {
            const rect = boxRef.current.getBoundingClientRect();
            // Przybliżony obwód prostokąta z zaokrągleniami
            const p = 2 * (rect.width + rect.height) - 16;
            setPerimeter(p);
        }
    }, []);

    const strokeDashoffset = perimeter > 0 ? perimeter - (percent / 100) * perimeter : 0;
    const stateClass = isOnBreak ? 'paused' : isComplete ? 'complete' : 'active';

    return (
        <div className="work-frame-wrapper">
            {/* Główny kontener otoczony dopełniającym się prostokątem */}
            <div className={`work-frame-box ${stateClass}`} ref={boxRef}>
                {/* SVG z dynamicznie dopełniającą się ramką */}
                <svg className="work-frame-svg">
                    <rect
                        className="work-frame-bg-stroke"
                        x="2"
                        y="2"
                        rx="14"
                        ry="14"
                    />
                    <rect
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
                </svg>

                {/* Zawartość wewnątrz ramki */}
                <div className="work-frame-content">
                    <div className="work-time-display">
                        {isOnBreak ? t('timeTracker.onBreak', 'W TRAKCIE PRZERWY') : elapsedString}
                    </div>

                    <span className="work-progress-label">
                        POSTĘP: {percent}%
                    </span>

                    {/* Mniejszy, dyskretny przycisk zakończenia pracy */}
                    <button
                        type="button"
                        className="btn-end-work-compact"
                        onClick={onEndWork}
                        disabled={isLoading}
                    >
                        <Square size={14} className="fill-current" />
                        <span>{isLoading ? t('common.loadingUpper', 'ŁADOWANIE...') : t('timeTracker.endWork', 'Zakończ pracę')}</span>
                    </button>
                </div>
            </div>

            {/* Informacje pod ramką */}
            <div className="battery-info-row">
                <div className="info-col">
                    <span className="label">Przepracowano:</span>
                    <span className="val">{formatHM(elapsedMs)}</span>
                </div>
                <div className="info-col">
                    <span className="label">{isComplete ? 'Nadgodziny:' : 'Do końca:'}</span>
                    <span className={`val ${isComplete ? 'complete' : ''}`}>
                        {isComplete ? formatHM(elapsedMs - safeTargetMs) : formatHM(remainingMs)}
                    </span>
                </div>
            </div>
        </div>
    );
};

export default WorkProgressBattery;
