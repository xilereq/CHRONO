import React, { useState } from 'react';
import { Card } from '../../components';
import { Clock, CalendarOff, TrendingUp, ChevronLeft, ChevronRight } from 'lucide-react';
import '../main.css';
import './raporty.css';

const MONTHS = [
    'Styczeń','Luty','Marzec','Kwiecień','Maj','Czerwiec',
    'Lipiec','Sierpień','Wrzesień','Październik','Listopad','Grudzień',
];

const PERIODS = ['Tygodnie', 'Miesiąc', 'Kwartał', 'Rok'];

const CHART_DATA = {
    'Tygodnie': [
        { label: '1–7 kwi',  przepr: 38, plan: 40 },
        { label: '8–14 kwi', przepr: 40, plan: 40 },
        { label: '15–21 kwi',przepr: 36, plan: 40 },
        { label: '22–30 kwi',przepr: 38, plan: 48 },
    ],
    'Miesiąc': [
        { label: 'Sty', przepr: 160, plan: 168 },
        { label: 'Lut', przepr: 152, plan: 160 },
        { label: 'Mar', przepr: 170, plan: 176 },
        { label: 'Kwi', przepr: 152, plan: 168 },
        { label: 'Maj', przepr: 0,   plan: 168 },
        { label: 'Cze', przepr: 0,   plan: 160 },
        { label: 'Lip', przepr: 0,   plan: 184 },
        { label: 'Sie', przepr: 0,   plan: 176 },
        { label: 'Wrz', przepr: 0,   plan: 168 },
        { label: 'Paź', przepr: 0,   plan: 184 },
        { label: 'Lis', przepr: 0,   plan: 168 },
        { label: 'Gru', przepr: 0,   plan: 176 },
    ],
    'Kwartał': [
        { label: 'Q1 2026', przepr: 482, plan: 504 },
        { label: 'Q2 2026', przepr: 152, plan: 496 },
        { label: 'Q3 2026', przepr: 0,   plan: 528 },
        { label: 'Q4 2026', przepr: 0,   plan: 528 },
    ],
    'Rok': [
        { label: '2022', przepr: 1920, plan: 1960 },
        { label: '2023', przepr: 1980, plan: 2008 },
        { label: '2024', przepr: 1948, plan: 1968 },
        { label: '2025', przepr: 1960, plan: 1984 },
        { label: '2026', przepr: 634,  plan: 2056 },
    ],
};

const ABSENCE_BREAKDOWN = [
    { label: 'Urlop wypoczynkowy',  days: 2, color: '#FFDE42' },
    { label: 'Zwolnienie lekarskie',days: 0, color: '#ef4444' },
    { label: 'Urlop na żądanie',    days: 0, color: '#6b7280' },
    { label: 'Inne',                days: 0, color: '#d1d5db' },
];

const OVERTIME_WEEKS = [
    { label: '1–7 kwi',  val: 0  },
    { label: '8–14 kwi', val: 0  },
    { label: '15–21 kwi',val: -4 },
    { label: '22–30 kwi',val: 8  },
];

/* ---------- Bar Chart ---------- */
const BarChart = ({ data }) => {
    const maxRaw = Math.max(...data.map(d => Math.max(d.plan, d.przepr)));
    const roundedMax = Math.ceil(maxRaw / 50) * 50 || 50;

    const W = 560, H = 200;
    const padL = 40, padB = 28, padT = 10, padR = 10;
    const chartW = W - padL - padR;
    const chartH = H - padT - padB;
    const n = data.length;
    const groupW = chartW / n;
    const barW = Math.min(groupW * 0.28, 28);
    const gap = 4;
    const ticks = 4;

    return (
        <svg viewBox={`0 0 ${W} ${H}`} width="100%" style={{ display: 'block' }}>
            {[...Array(ticks + 1)].map((_, i) => {
                const val = Math.round(roundedMax * i / ticks);
                const y = padT + chartH - (chartH * i / ticks);
                return (
                    <g key={i}>
                        <line x1={padL} y1={y} x2={padL + chartW} y2={y} stroke="#f3f4f6" strokeWidth="1" />
                        <text x={padL - 6} y={y + 4} textAnchor="end" fontSize="10" fill="#9ca3af">{val}</text>
                    </g>
                );
            })}

            {data.map((d, i) => {
                const cx = padL + groupW * i + groupW / 2;
                const planH  = (d.plan   / roundedMax) * chartH;
                const przeprH= (d.przepr / roundedMax) * chartH;
                return (
                    <g key={i}>
                        <rect
                            x={cx - barW - gap / 2}
                            y={padT + chartH - planH}
                            width={barW} height={planH}
                            fill="#e5e7eb" rx="3"
                        />
                        {przeprH > 0 && (
                            <rect
                                x={cx + gap / 2}
                                y={padT + chartH - przeprH}
                                width={barW} height={przeprH}
                                fill="#FFDE42" rx="3"
                            />
                        )}
                        <text x={cx} y={padT + chartH + 16} textAnchor="middle" fontSize="10" fill="#9ca3af">
                            {d.label}
                        </text>
                    </g>
                );
            })}

            <line x1={padL} y1={padT} x2={padL} y2={padT + chartH} stroke="#e5e7eb" strokeWidth="1" />
            <line x1={padL} y1={padT + chartH} x2={padL + chartW} y2={padT + chartH} stroke="#e5e7eb" strokeWidth="1" />
        </svg>
    );
};

/* ---------- Main page ---------- */
const Raporty = () => {
    const [month, setMonth] = useState(3);
    const [year, setYear]   = useState(2026);
    const [period, setPeriod] = useState('Tygodnie');

    const prevMonth = () => {
        if (month === 0) { setMonth(11); setYear(y => y - 1); }
        else setMonth(m => m - 1);
    };
    const nextMonth = () => {
        if (month === 11) { setMonth(0); setYear(y => y + 1); }
        else setMonth(m => m + 1);
    };

    const summary = { godziny: 152, plan: 168, nieobecnosci: 2, nadgodziny: 4 };
    const pct = Math.round((summary.godziny / summary.plan) * 100);
    const maxOT = Math.max(...OVERTIME_WEEKS.map(w => Math.abs(w.val)), 1);

    return (
        <div className="dashboard">

            {/* ── Nagłówek ── */}
            <div className="raporty-header">
                <h2 className="dashboard-title">Raporty</h2>
                <div className="month-selector">
                    <button className="month-nav" onClick={prevMonth}><ChevronLeft size={16} /></button>
                    <span className="month-label">{MONTHS[month]} {year}</span>
                    <button className="month-nav" onClick={nextMonth}><ChevronRight size={16} /></button>
                </div>
            </div>

            {/* ── Kafelki ── */}
            <div className="raporty-tiles">

                <div className="raporty-tile">
                    <div className="tile-icon tile-icon--yellow"><Clock size={20} /></div>
                    <div className="tile-content">
                        <span className="tile-value">{summary.godziny}<span className="tile-unit">h</span></span>
                        <span className="tile-label">Przepracowane</span>
                        <span className="tile-sub">Plan: {summary.plan}h</span>
                    </div>
                    <div className="tile-progress-track">
                        <div className="tile-progress-fill" style={{ width: `${pct}%` }} />
                    </div>
                    <span className="tile-pct">{pct}% planu</span>
                </div>

                <div className="raporty-tile">
                    <div className="tile-icon tile-icon--red"><CalendarOff size={20} /></div>
                    <div className="tile-content">
                        <span className="tile-value">{summary.nieobecnosci}<span className="tile-unit">dni</span></span>
                        <span className="tile-label">Nieobecności</span>
                        <span className="tile-sub">w tym miesiącu</span>
                    </div>
                </div>

                <div className="raporty-tile">
                    <div className={`tile-icon ${summary.nadgodziny >= 0 ? 'tile-icon--green' : 'tile-icon--red'}`}>
                        <TrendingUp size={20} />
                    </div>
                    <div className="tile-content">
                        <span className="tile-value">
                            {summary.nadgodziny > 0 ? '+' : ''}{summary.nadgodziny}<span className="tile-unit">h</span>
                        </span>
                        <span className="tile-label">Nadgodziny</span>
                        <span className="tile-sub">bilans miesięczny</span>
                    </div>
                </div>

            </div>

            {/* ── Wykres słupkowy ── */}
            <Card title="Godziny pracy">
                <div className="chart-controls">
                    <div className="period-tabs">
                        {PERIODS.map(p => (
                            <button
                                key={p}
                                className={`period-tab ${period === p ? 'active' : ''}`}
                                onClick={() => setPeriod(p)}
                            >
                                {p}
                            </button>
                        ))}
                    </div>
                    <div className="raporty-legend">
                        <span className="raporty-legend-item">
                            <span className="raporty-legend-box raporty-legend-box--gray" />
                            Plan
                        </span>
                        <span className="raporty-legend-item">
                            <span className="raporty-legend-box raporty-legend-box--yellow" />
                            Przepracowane
                        </span>
                    </div>
                </div>
                <BarChart data={CHART_DATA[period]} />
            </Card>

            {/* ── Dolny rząd ── */}
            <div className="raporty-bottom">

                <Card title="Nieobecności">
                    <div className="absence-list">
                        {ABSENCE_BREAKDOWN.map(item => (
                            <div key={item.label} className="absence-row">
                                <div className="absence-info">
                                    <span className="absence-dot" style={{ background: item.color }} />
                                    <span className="absence-name">{item.label}</span>
                                </div>
                                <span className="absence-days">{item.days} dni</span>
                            </div>
                        ))}
                    </div>
                </Card>

                <Card title="Nadgodziny w tygodniach">
                    <div className="overtime-list">
                        {OVERTIME_WEEKS.map(item => (
                            <div key={item.label} className="overtime-row">
                                <span className="overtime-label">{item.label}</span>
                                <div className="overtime-track">
                                    {item.val !== 0 && (
                                        <div
                                            className="overtime-fill"
                                            style={{
                                                width: `${(Math.abs(item.val) / maxOT) * 100}%`,
                                                background: item.val >= 0 ? '#FFDE42' : '#ef4444',
                                            }}
                                        />
                                    )}
                                </div>
                                <span className={`overtime-val ${item.val > 0 ? 'pos' : item.val < 0 ? 'neg' : ''}`}>
                                    {item.val > 0 ? '+' : ''}{item.val}h
                                </span>
                            </div>
                        ))}
                    </div>
                </Card>

            </div>
        </div>
    );
};

export default Raporty;
