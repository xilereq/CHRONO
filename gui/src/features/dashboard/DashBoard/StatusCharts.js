import React from 'react';
import { Card } from '../../../components';

import '../../dashboard.css';

const StatusCharts = () => {
    return (
        <Card title="TWÓJ STATUS">
            <div className="charts-container">
                {/* Wykres 1 */}
                <div className="chart-wrapper">
                    <span className="chart-title">NADGODZINY</span>
                    <div className="chart-svg-container">
                        <svg viewBox="0 0 36 36" className="chart-svg">
                            <path className="text-blue-300" strokeWidth="6" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                            <path className="text-blue-600" strokeWidth="6" strokeDasharray="16, 100" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                        </svg>
                    </div>
                    <div className="chart-legend">
                        <div className="legend-item"><div className="legend-dot bg-blue-800"></div> Posiadane: 12h 30m</div>
                        <div className="legend-item"><div className="legend-dot bg-blue-600"></div> Odebrane: 2h 00m</div>
                        <div className="legend-item"><div className="legend-dot bg-blue-300"></div> Dostępne: 10h 30m</div>
                    </div>
                </div>

                {/* Wykres 2 */}
                <div className="chart-wrapper">
                    <span className="chart-title">CZAS PRACY (BIEŻĄCY MIESIĄC)</span>
                    <div className="chart-svg-container">
                        <svg viewBox="0 0 36 36" className="chart-svg">
                            <path className="text-blue-300" strokeWidth="6" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                            <path className="text-blue-800" strokeWidth="6" strokeDasharray="55, 100" stroke="currentColor" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                        </svg>
                    </div>
                    <div className="chart-legend">
                        <div className="legend-item"><div className="legend-dot bg-blue-800"></div> Przepracowano: 88h</div>
                        <div className="legend-item"><div className="legend-dot bg-blue-300"></div> Do przepracowania: 72h</div>
                    </div>
                </div>
            </div>
        </Card>
    );
}

export default StatusCharts;