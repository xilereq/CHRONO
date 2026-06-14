import React, { useState, useEffect } from 'react';
import { Card } from '../index';
import { workLogService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import '../../pages/main.css';

const formatHours = (seconds) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    return `${h}h ${m}m`;
};

const StatusCharts = () => {
    const { t } = useLanguage();
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
        return <Card title={t('statusCharts.yourStatusTitle', 'TWÓJ STATUS')}>{t('statusCharts.loadingStats', 'Ładowanie statystyk...')}</Card>;
    }

    const workedHours = Math.floor(stats.totalWorkTime);
    const plannedHours = Math.floor(stats.totalMonthlyHours);
    const progressPercent = Math.min((stats.totalWorkTime / stats.totalMonthlyHours) * 100, 100);

    return (
        <Card title={t('statusCharts.yourStatusTitle', 'TWÓJ STATUS')}>
            <div className="charts-container">
                <div className="chart-wrapper">
                    <span className="chart-title">{t('statusCharts.overtimeTitle', 'NADGODZINY')}</span>
                    <div className="chart-svg-container">
                        <svg viewBox="0 0 36 36" className="chart-svg">
                            <path className="text-blue-300" strokeWidth="6" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                            <path className="text-blue-600" strokeWidth="6" strokeDasharray="100, 100" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                        </svg>
                    </div>
                    <div className="chart-legend">
                        <div className="legend-item"><div className="legend-dot bg-blue-600"></div> {t('statusCharts.balance', 'Saldo:')} {formatHours(stats.overtimeSeconds)}</div>
                    </div>
                </div>

                <div className="chart-wrapper">
                    <span className="chart-title">{t('statusCharts.workTimeTitle', 'CZAS PRACY (BIEŻĄCY MIESIĄC)')}</span>
                    <div className="chart-svg-container">
                        <svg viewBox="0 0 36 36" className="chart-svg">
                            <path className="text-blue-300" strokeWidth="6" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                            <path className="text-blue-800" strokeWidth="6" strokeDasharray={`${progressPercent}, 100`} stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                        </svg>
                    </div>
                    <div className="chart-legend">
                        <div className="legend-item"><div className="legend-dot bg-blue-800"></div> {t('statusCharts.worked', 'Przepracowano:')} {workedHours}h</div>
                        <div className="legend-item"><div className="legend-dot bg-blue-300"></div> {t('statusCharts.plan', 'Plan:')} {plannedHours}h</div>
                    </div>
                </div>
            </div>
        </Card>
    );
};

export default StatusCharts;
