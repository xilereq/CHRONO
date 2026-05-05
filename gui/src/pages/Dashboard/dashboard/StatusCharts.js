import React, { useState, useEffect } from 'react';
import { Card } from '../../../components';
import { workLogService } from '../../../services/workLogService';
import '../../main.css';

const formatHours = (seconds) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    return `${h}h ${m}m`;
};

const StatusCharts = () => {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const data = await workLogService.getMonthlyStats();
                setStats(data);
            } catch (error) {
                console.error(error);
            } finally {
                setLoading(false);
            }
        };
        fetchStats();
    }, []);

    if (loading || !stats) {
        return <Card title="TWÓJ STATUS">Ładowanie statystyk...</Card>;
    }

    const workedHours = Math.floor(stats.workedSeconds / 3600);
    const plannedHours = Math.floor(stats.plannedSeconds / 3600);
    const progressPercent = Math.min((stats.workedSeconds / stats.plannedSeconds) * 100, 100);

    return (
        <Card title="TWÓJ STATUS">
            <div className="charts-container">
                <div className="chart-wrapper">
                    <span className="chart-title">NADGODZINY</span>
                    <div className="chart-svg-container">
                        <svg viewBox="0 0 36 36" className="chart-svg">
                            <path className="text-blue-300" strokeWidth="6" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                            <path className="text-blue-600" strokeWidth="6" strokeDasharray="100, 100" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                        </svg>
                    </div>
                    <div className="chart-legend">
                        <div className="legend-item"><div className="legend-dot bg-blue-600"></div> Saldo: {formatHours(stats.overtimeSeconds)}</div>
                    </div>
                </div>

                <div className="chart-wrapper">
                    <span className="chart-title">CZAS PRACY (BIEŻĄCY MIESIĄC)</span>
                    <div className="chart-svg-container">
                        <svg viewBox="0 0 36 36" className="chart-svg">
                            <path className="text-blue-300" strokeWidth="6" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                            <path className="text-blue-800" strokeWidth="6" strokeDasharray={`${progressPercent}, 100`} stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                        </svg>
                    </div>
                    <div className="chart-legend">
                        <div className="legend-item"><div className="legend-dot bg-blue-800"></div> Przepracowano: {workedHours}h</div>
                        <div className="legend-item"><div className="legend-dot bg-blue-300"></div> Plan: {plannedHours}h</div>
                    </div>
                </div>
            </div>
        </Card>
    );
};

export default StatusCharts;
