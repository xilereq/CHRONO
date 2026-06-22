import React, { useState, useEffect } from 'react';
import { Card } from '../index';
import { workLogService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import '../../pages/main.css';

const formatHours = (seconds) => {
    if (!seconds) return '0h 0m';

    const isNegative = seconds < 0;
    const absSeconds = Math.abs(seconds);
    const h = Math.floor(absSeconds / 3600);
    const m = Math.floor((absSeconds % 3600) / 60);

    return `${isNegative ? '-' : ''}${h}h ${m}m`;
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
        return (
            <Card title={t('statusCharts.yourStatusTitle', 'TWÓJ STATUS')}>
                <div style={{ padding: '20px', textAlign: 'center', color: 'var(--text-muted)' }}>
                    {t('statusCharts.loadingStats', 'Ładowanie statystyk...')}
                </div>
            </Card>
        );
    }

    const workedHours = stats.totalWorkTime || 0;
    const excusedHours = stats.excusedHours || 0;
    const plannedHours = stats.totalMonthlyHours || 0;

    const safePlannedHours = plannedHours > 0 ? plannedHours : 1;
    const workedPercent = Math.max(0, Math.min((workedHours / safePlannedHours) * 100, 100));
    const excusedPercent = Math.max(0, Math.min((excusedHours / safePlannedHours) * 100, 100));

    const normalizedExcusedPercent = Math.min(excusedPercent, 100 - workedPercent);

    return (
        <Card title={t('statusCharts.yourStatusTitle', 'TWÓJ STATUS')}>
            <div className="charts-container">

                <div className="chart-wrapper">
                    <span className="chart-title">{t('statusCharts.overtimeTitle', 'NADGODZINY')}</span>
                    <div className="chart-svg-container">
                        <svg viewBox="0 0 36 36" className="chart-svg">
                            <path
                                className="text-blue-300"
                                strokeWidth="6"
                                stroke="currentColor"
                                fill="none"
                                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                            />
                            <path
                                className="text-blue-600"
                                strokeWidth="6"
                                strokeDasharray="100, 100"
                                stroke="currentColor"
                                fill="none"
                                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                            />
                        </svg>
                    </div>
                    <div className="chart-legend">
                        <div className="legend-item">
                            <div className="legend-dot bg-blue-600"></div>
                            {t('statusCharts.balance', 'Saldo:')} {formatHours(stats.overtimeSeconds)}
                        </div>
                    </div>
                </div>

                <div className="chart-wrapper">
                    <span className="chart-title">{t('statusCharts.workTimeTitle', 'CZAS PRACY (BIEŻĄCY MIESIĄC)')}</span>
                    <div className="chart-svg-container">
                        <svg viewBox="0 0 36 36" className="chart-svg">
                            <path
                                className="text-blue-300"
                                strokeWidth="6"
                                stroke="currentColor"
                                fill="none"
                                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                            />
                            <path
                                className="text-blue-800"
                                strokeWidth="6"
                                strokeDasharray={`${workedPercent}, 100`}
                                stroke="currentColor"
                                fill="none"
                                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                            />
                            <path
                                style={{ color: '#22c55e' }}
                                strokeWidth="6"
                                strokeDasharray={`${normalizedExcusedPercent}, 100`}
                                strokeDashoffset={`-${workedPercent}`}
                                stroke="currentColor"
                                fill="none"
                                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                            />
                        </svg>
                    </div>
                    <div className="chart-legend">
                        <div className="legend-item">
                            <div className="legend-dot bg-blue-800"></div>
                            {t('statusCharts.worked', 'Przepracowano:')} {workedHours}h
                        </div>
                        <div className="legend-item">
                            <div className="legend-dot" style={{ backgroundColor: '#22c55e' }}></div>
                            {t('statusCharts.excused', 'Usprawiedliwione:')} {excusedHours}h
                        </div>
                        <div className="legend-item">
                            <div className="legend-dot bg-blue-300"></div>
                            {t('statusCharts.plan', 'Plan:')} {plannedHours}h
                        </div>
                    </div>
                </div>

            </div>
        </Card>
    );
};

export default StatusCharts;
