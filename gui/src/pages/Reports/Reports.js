import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Clock, CalendarOff, TrendingUp, Timer, Download, ChevronLeft, ChevronRight, AlertTriangle } from 'lucide-react';
import { Card } from '../../components';
import { userService, reportService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import '../main.css';
import './reports.css';

// ── Helpers ──────────────────────────────────────────────────────────────────

const pad = (n) => String(n).padStart(2, '0');

const monthBounds = (year, month) => {
    const lastDay = new Date(year, month + 1, 0).getDate();
    return {
        startDate: `${year}-${pad(month + 1)}-01`,
        endDate:   `${year}-${pad(month + 1)}-${pad(lastDay)}`,
    };
};

const toH = (sec) => +(Math.abs(sec) / 3600).toFixed(1);

const ABSENCE_COLORS = {
    HOLIDAY_LEAVE: '#FFDE42',
    SICK_LEAVE:    '#ef4444',
    OVERTIME_TAKE: '#6b7280',
    UNEXCUSED:     '#9ca3af',
};

// ── Bar chart ────────────────────────────────────────────────────────────────

const BarChart = ({ data, emptyLabel }) => {
    if (!data || !data.length) return <div className="empty">{emptyLabel || 'Brak danych.'}</div>;
    const maxRaw = Math.max(...data.map(d => Math.max(d.plan, d.przepr, 0)));
    const roundedMax = Math.max(Math.ceil(maxRaw / 10) * 10, 50);
    const W = 560, H = 210, padL = 48, padB = 32, padT = 10, padR = 10;
    const chartW = W - padL - padR;
    const chartH = H - padT - padB;
    const n = data.length;
    const groupW = chartW / n;
    const barW = Math.min(groupW * 0.35, 22);
    const gap = 3;
    const ticks = roundedMax / 10;
    const ref40Y = padT + chartH - (chartH * 40 / roundedMax);

    const minorSteps = roundedMax / 2;

    return (
        <svg viewBox={`0 0 ${W} ${H}`} width="100%" style={{ display: 'block' }}>
            {/* Minor grid lines every 2h — dashed, very thin, skip 10h marks */}
            {[...Array(minorSteps + 1)].map((_, i) => {
                if (i % 5 === 0) return null;
                const y = padT + chartH - (chartH * i / minorSteps);
                return <line key={`m${i}`} x1={padL} y1={y} x2={padL + chartW} y2={y} stroke="#f3f4f6" strokeWidth="0.4" strokeDasharray="2 4" />;
            })}

            {/* Major grid lines every 10h + Y-axis labels */}
            {[...Array(ticks + 1)].map((_, i) => {
                const val = Math.round(roundedMax * i / ticks);
                const y = padT + chartH - (chartH * i / ticks);
                return (
                    <g key={i}>
                        <line x1={padL} y1={y} x2={padL + chartW} y2={y} stroke="#e5e7eb" strokeWidth="1.5" />
                        <text x={padL - 6} y={y + 4} textAnchor="end" fontSize="10" fill="#9ca3af">{val}h</text>
                    </g>
                );
            })}

            {/* 40h reference line */}
            <line
                x1={padL} y1={ref40Y}
                x2={padL + chartW} y2={ref40Y}
                stroke="#ef4444" strokeWidth="4" strokeDasharray="6 4" opacity="0.65"
            />
            {/* Bars */}
            {data.map((d, i) => {
                const cx = padL + groupW * i + groupW / 2;
                const planH  = (d.plan   / roundedMax) * chartH;
                const przeprH = (d.przepr / roundedMax) * chartH;
                return (
                    <g key={i}>
                        <rect x={cx - barW - gap / 2} y={padT + chartH - planH}  width={barW} height={planH}  fill="#e5e7eb" rx="3" />
                        {przeprH > 0 && (
                            <rect x={cx + gap / 2} y={padT + chartH - przeprH} width={barW} height={przeprH} fill="#FFDE42" rx="3" />
                        )}
                        <text x={cx} y={padT + chartH + 18} textAnchor="middle" fontSize="8" fill="#9ca3af">{d.label}</text>
                    </g>
                );
            })}

            {/* Chart border — full rectangle */}
            <line x1={padL}          y1={padT}          x2={padL}          y2={padT + chartH} stroke="#e5e7eb" strokeWidth="1" />
            <line x1={padL}          y1={padT + chartH} x2={padL + chartW} y2={padT + chartH} stroke="#e5e7eb" strokeWidth="1" />
            <line x1={padL + chartW} y1={padT}          x2={padL + chartW} y2={padT + chartH} stroke="#e5e7eb" strokeWidth="1" />
        </svg>
    );
};

// ── Format picker ─────────────────────────────────────────────────────────────

const FORMATS = ['CSV', 'PDF', 'XLSX'];

const FormatPicker = ({ value, onChange }) => (
    <div className="period-tabs">
        {FORMATS.map(f => (
            <button key={f} className={`period-tab ${value === f ? 'active' : ''}`} onClick={() => onChange(f)}>
                {f}
            </button>
        ))}
    </div>
);

// ── Main component ────────────────────────────────────────────────────────────

const today = new Date();

const Reports = () => {
    const { t, language } = useLanguage();
    const [month, setMonth] = useState(today.getMonth());
    const [year,  setYear]  = useState(today.getFullYear());

    const [timesheetData, setTimesheetData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error,   setError]   = useState('');

    const [userRole,     setUserRole]     = useState(null);
    const [subordinates, setSubordinates] = useState([]);

    // Download section
    const [dlTab,        setDlTab]        = useState('own');
    const [dlType,       setDlType]       = useState('timesheet');
    const [dlFormat,     setDlFormat]     = useState('CSV');
    const [dlStartDate,  setDlStartDate]  = useState(`${today.getFullYear()}-${pad(today.getMonth() + 1)}-01`);
    const [dlEndDate,    setDlEndDate]    = useState(`${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`);
    const [dlEmployeeId, setDlEmployeeId] = useState('');
    const [dlAnomalies,  setDlAnomalies]  = useState(false);
    const [downloading,  setDownloading]  = useState(false);

    useEffect(() => {
        userService.getMyRole().then(r => {
            const role = r.role?.replace('ROLE_', '') || 'EMPLOYEE';
            setUserRole(role);
            if (role === 'MANAGER') {
                userService.getSubordinates().then(setSubordinates).catch(() => {});
            } else if (role === 'HR' || role === 'ADMIN') {
                userService.getAllUsers().then(setSubordinates).catch(() => {});
            }
        }).catch(() => setUserRole('EMPLOYEE'));
    }, []);

    const loadData = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const { startDate, endDate } = monthBounds(year, month);
            setTimesheetData(await reportService.fetchMyTimesheetData(startDate, endDate));
        } catch {
            setError(t('reports.errorLoading', 'Nie udało się załadować danych. Sprawdź, czy backend jest uruchomiony.'));
            setTimesheetData(null);
        } finally {
            setLoading(false);
        }
    }, [year, month]);

    useEffect(() => { loadData(); }, [loadData]);

    const prevMonth = () => { if (month === 0) { setMonth(11); setYear(y => y - 1); } else setMonth(m => m - 1); };
    const nextMonth = () => { if (month === 11) { setMonth(0); setYear(y => y + 1); } else setMonth(m => m + 1); };

    // ── Derived data ──────────────────────────────────────────────────────────

    const chartData = useMemo(() => {
        if (!timesheetData?.dailyEntries?.length) return [];
        const weeks = {};
        timesheetData.dailyEntries.forEach(e => {
            const d = new Date(e.date);
            const mon = new Date(d);
            mon.setDate(d.getDate() - ((d.getDay() + 6) % 7));
            const key = mon.toISOString().slice(0, 10);
            if (!weeks[key]) {
                const fri = new Date(mon);
                fri.setDate(mon.getDate() + 4);
                const lastDayOfMonth = new Date(mon.getFullYear(), mon.getMonth() + 1, 0).getDate();
                const label = mon.getMonth() === fri.getMonth()
                    ? `${mon.getDate()}-${fri.getDate()}.${pad(mon.getMonth() + 1)}`
                    : mon.getDate() === lastDayOfMonth
                        ? `${mon.getDate()}.${pad(mon.getMonth() + 1)}`
                        : `${mon.getDate()}-${lastDayOfMonth}.${pad(mon.getMonth() + 1)}`;
                weeks[key] = { przepr: 0, plan: 0, label };
            }
            weeks[key].przepr += e.workSeconds;
            weeks[key].plan   += e.requiredSeconds;
        });
        return Object.values(weeks).map(w => ({ ...w, przepr: toH(w.przepr), plan: toH(w.plan) }));
    }, [timesheetData]);

    const absenceBreakdown = useMemo(() => {
        if (!timesheetData?.dailyEntries) return [];
        const absenceTypeLabels = {
            HOLIDAY_LEAVE: t('requests.types.holiday', 'Urlop wypoczynkowy'),
            SICK_LEAVE:    t('requests.types.sick', 'Zwolnienie lekarskie'),
            OVERTIME_TAKE: t('requests.types.overtimeTake', 'Odbiór nadgodzin'),
            UNEXCUSED:     t('reports.unexcused', 'Nieobecność bez wniosku'),
        };
        const todayStr = new Date().toISOString().slice(0, 10);
        const counts = {};
        timesheetData.dailyEntries.forEach(e => {
            if (e.date > todayStr) return;
            if (e.requiredSeconds === 0) return;
            const fullDayOff = e.workSeconds === 0 && e.requiredSeconds > 0;
            if (e.applicationType === 'HOLIDAY_LEAVE' || e.applicationType === 'SICK_LEAVE') {
                counts[e.applicationType] = (counts[e.applicationType] || 0) + 1;
            } else if (e.applicationType === 'OVERTIME_TAKE' && e.workSeconds === 0 && e.requiredSeconds > 0) {
                counts['OVERTIME_TAKE'] = (counts['OVERTIME_TAKE'] || 0) + 1;
            } else if (fullDayOff) {
                counts['UNEXCUSED'] = (counts['UNEXCUSED'] || 0) + 1;
            }
        });
        return Object.entries(counts).map(([type, days]) => ({
            label: absenceTypeLabels[type] || type,
            color: ABSENCE_COLORS[type] || '#d1d5db',
            days,
        }));
    }, [timesheetData, t]);

    const overtimeByWeek = useMemo(() => {
        if (!timesheetData?.dailyEntries?.length) return [];
        const weeks = {};
        timesheetData.dailyEntries.forEach(e => {
            const d = new Date(e.date);
            const mon = new Date(d);
            mon.setDate(d.getDate() - ((d.getDay() + 6) % 7));
            const key = mon.toISOString().slice(0, 10);
            if (!weeks[key]) {
                const fri = new Date(mon);
                fri.setDate(mon.getDate() + 4);
                const lastDayOfMonth = new Date(mon.getFullYear(), mon.getMonth() + 1, 0).getDate();
                const label = mon.getMonth() === fri.getMonth()
                    ? `${mon.getDate()}-${fri.getDate()}.${pad(mon.getMonth() + 1)}`
                    : mon.getDate() === lastDayOfMonth
                        ? `${mon.getDate()}.${pad(mon.getMonth() + 1)}`
                        : `${mon.getDate()}-${lastDayOfMonth}.${pad(mon.getMonth() + 1)}`;
                weeks[key] = { val: 0, label };
            }
            weeks[key].val += e.balanceSeconds;
        });
        return Object.values(weeks).map(w => ({ ...w, val: +(w.val / 3600).toFixed(1) }));
    }, [timesheetData]);

    const maxOT  = Math.max(...overtimeByWeek.map(w => Math.abs(w.val)), 1);
    const td     = timesheetData;
    const workH  = td ? toH(td.totalWork)     : null;
    const planH  = td ? toH(td.totalRequired) : null;
    const overtimeH = td ? +(Math.max(td.totalBalance,  0) / 3600).toFixed(1) : null;
    const deficitH  = td ? +(Math.max(-td.totalBalance, 0) / 3600).toFixed(1) : null;
    const pct    = workH != null && planH > 0 ? Math.min(Math.round((workH / planH) * 100), 100) : 0;
    const absTotal = absenceBreakdown.reduce((s, a) => s + a.days, 0);

    const isManager       = userRole === 'MANAGER';
    const canViewEmployee = userRole === 'MANAGER' || userRole === 'HR' || userRole === 'ADMIN';

    // ── Download handler ──────────────────────────────────────────────────────

    const handleDownload = async () => {
        if (!dlStartDate || !dlEndDate) { alert(t('reports.errorDateRange', 'Wybierz zakres dat.')); return; }
        if (dlStartDate > dlEndDate)    { alert(t('reports.errorDateOrder', 'Data "Od" musi być wcześniejsza niż "Do".')); return; }
        setDownloading(true);
        try {
            if (dlTab === 'own') {
                if (dlType === 'timesheet') await reportService.downloadMyTimesheet(dlStartDate, dlEndDate, dlFormat);
                else                        await reportService.downloadMyOvertime(dlStartDate, dlEndDate, dlFormat);
            } else if (dlTab === 'employee') {
                if (!dlEmployeeId) { alert(t('reports.errorNoEmployee', 'Wybierz pracownika.')); return; }
                await reportService.downloadEmployeeTimesheet(dlEmployeeId, dlStartDate, dlEndDate, dlFormat);
            } else if (dlTab === 'team') {
                if (dlAnomalies) {
                    await reportService.downloadTeamAnomalies(dlStartDate, dlEndDate, dlFormat);
                } else if (dlType === 'workload') {
                    await reportService.downloadTeamWorkload(dlStartDate, dlEndDate, dlFormat);
                } else if (dlType === 'absences') {
                    await reportService.downloadTeamAbsences(dlStartDate, dlEndDate, dlFormat);
                } else {
                    await reportService.downloadTeamOvertime(dlStartDate, dlEndDate, dlFormat);
                }
            }
        } catch (e) {
            alert(e.message || t('reports.errorDownloading', 'Błąd podczas pobierania raportu.'));
        } finally {
            setDownloading(false);
        }
    };

    // ── Render ────────────────────────────────────────────────────────────────

    return (
        <div className="dashboard">

            {/* Nagłówek */}
            <div className="raporty-header">
                <h2 className="dashboard-title">{t('reports.title', 'Raporty')}</h2>
                <div className="month-selector">
                    <button className="month-nav" onClick={prevMonth}><ChevronLeft size={16} /></button>
                    <span className="month-label">
                        {new Date(year, month, 1).toLocaleDateString(language === 'pl' ? 'pl-PL' : 'en-US', { month: 'long', year: 'numeric' })}
                    </span>
                    <button className="month-nav" onClick={nextMonth}><ChevronRight size={16} /></button>
                </div>
            </div>

            {error && <div className="login-error" style={{ marginBottom: '1rem' }}>{error}</div>}

            {/* Kafelki */}
            <div className="raporty-tiles">
                <div className="raporty-tile">
                    <div className="tile-icon tile-icon--yellow"><Clock size={20} /></div>
                    <div className="tile-content">
                        <span className="tile-value">{loading ? '…' : workH}<span className="tile-unit">h</span></span>
                        <span className="tile-label">{t('reports.workedHours', 'Przepracowane')}</span>
                        <span className="tile-sub">{t('reports.planPrefix', 'Plan:')} {loading ? '…' : planH}h</span>
                    </div>
                    {!loading && planH > 0 && (
                        <>
                            <div className="tile-progress-track">
                                <div className="tile-progress-fill" style={{ width: `${pct}%` }} />
                            </div>
                            <span className="tile-pct">{pct}{t('reports.percentPlan', '% planu')}</span>
                        </>
                    )}
                </div>

                <div className="raporty-tile">
                    <div className="tile-icon tile-icon--green"><TrendingUp size={20} /></div>
                    <div className="tile-content">
                        <span className="tile-value">{loading ? '…' : overtimeH}<span className="tile-unit">h</span></span>
                        <span className="tile-label">{t('reports.overtime', 'Nadgodziny')}</span>
                        <span className="tile-sub">{t('reports.overtimeNote', 'nadgodziny w miesiącu')}</span>
                    </div>
                </div>

                <div className="raporty-tile">
                    <div className="tile-icon tile-icon--blue"><Timer size={20} /></div>
                    <div className="tile-content">
                        <span className="tile-value">{loading ? '…' : deficitH}<span className="tile-unit">h</span></span>
                        <span className="tile-label">{t('reports.weekRemaining', 'Do wyrobienia')}</span>
                        <span className="tile-sub">{t('reports.weekRemainingNote', 'niedobór w miesiącu')}</span>
                    </div>
                </div>

                <div className="raporty-tile">
                    <div className="tile-icon tile-icon--red"><CalendarOff size={20} /></div>
                    <div className="tile-content">
                        <span className="tile-value">{loading ? '…' : absTotal}<span className="tile-unit">{t('reports.daysUnit', 'dni')}</span></span>
                        <span className="tile-label">{t('reports.absencesCount', 'Nieobecności')}</span>
                        <span className="tile-sub">{t('reports.absencesThisMonth', 'w tym miesiącu')}</span>
                    </div>
                </div>
            </div>

            {/* Wykres */}
            <Card title={t('reports.workHoursChart', 'Godziny pracy — tygodniowo')}>
                <div className="chart-controls">
                    <div className="raporty-legend">
                        <span className="raporty-legend-item">
                            <span className="raporty-legend-box raporty-legend-box--gray" /> {t('reports.legendPlan', 'Plan')}
                        </span>
                        <span className="raporty-legend-item">
                            <span className="raporty-legend-box raporty-legend-box--yellow" /> {t('reports.legendWorked', 'Przepracowane')}
                        </span>
                    </div>
                </div>
                {loading ? <div className="empty">{t('reports.loading', 'Ładowanie danych…')}</div> : <BarChart data={chartData} emptyLabel={t('reports.noDataToShow', 'Brak danych do wyświetlenia.')} />}
            </Card>

            {/* Dolny rząd */}
            <div className="raporty-bottom">
                <Card title={t('reports.absencesCardTitle', 'Nieobecności w miesiącu')}>
                    {loading ? <div className="empty">{t('reports.loadingShort', 'Ładowanie…')}</div>
                        : absenceBreakdown.length === 0
                            ? <div className="empty">{t('reports.noAbsences', 'Brak nieobecności w tym okresie.')}</div>
                            : (
                                <div className="absence-list">
                                    {absenceBreakdown.map(item => (
                                        <div key={item.label} className="absence-row">
                                            <div className="absence-info">
                                                <span className="absence-dot" style={{ background: item.color }} />
                                                <span className="absence-name">{item.label}</span>
                                            </div>
                                            <span className="absence-days">{item.days} {t('reports.daysUnit', 'dni')}</span>
                                        </div>
                                    ))}
                                </div>
                            )
                    }
                </Card>

                <Card title={t('reports.weeklyBalanceTitle', 'Bilans tygodniowy')}>
                    {loading ? <div className="empty">{t('reports.loadingShort', 'Ładowanie…')}</div>
                        : overtimeByWeek.length === 0
                            ? <div className="empty">{t('reports.noData', 'Brak danych.')}</div>
                            : (
                                <div className="overtime-list">
                                    {overtimeByWeek.map(item => (
                                        <div key={item.label} className="overtime-row">
                                            <span className="overtime-label">{item.label}</span>
                                            <div className="overtime-track">
                                                {item.val !== 0 && (
                                                    <div className="overtime-fill" style={{
                                                        width: `${(Math.abs(item.val) / maxOT) * 100}%`,
                                                        background: item.val >= 0 ? '#FFDE42' : '#ef4444',
                                                    }} />
                                                )}
                                            </div>
                                            <span className={`overtime-val ${item.val > 0 ? 'pos' : item.val < 0 ? 'neg' : ''}`}>
                                                {item.val > 0 ? '+' : ''}{item.val}h
                                            </span>
                                        </div>
                                    ))}
                                </div>
                            )
                    }
                </Card>
            </div>

            {/* Sekcja pobierania */}
            <div style={{ marginTop: '1.5rem' }}>
            <Card title={t('reports.downloadSection', 'Pobierz raport')}>
                <div className="dl-section">

                    {/* Zakładki: Moje / Pracownika / Zespołu */}
                    <div className="dl-tabs">
                        <button className={`period-tab ${dlTab === 'own' ? 'active' : ''}`}
                            onClick={() => { setDlTab('own'); setDlType('timesheet'); setDlAnomalies(false); }}>
                            {t('reports.myReports', 'Moje raporty')}
                        </button>
                        {canViewEmployee && (
                            <button className={`period-tab ${dlTab === 'employee' ? 'active' : ''}`}
                                onClick={() => { setDlTab('employee'); setDlType('timesheet'); setDlAnomalies(false); }}>
                                {t('reports.employeeReport', 'Raport pracownika')}
                            </button>
                        )}
                        {isManager && (
                            <button className={`period-tab ${dlTab === 'team' ? 'active' : ''}`}
                                onClick={() => { setDlTab('team'); setDlType('workload'); }}>
                                {t('reports.teamReports', 'Raporty zespołu')}
                            </button>
                        )}
                    </div>

                    {/* Typ raportu */}
                    <div className="dl-type-row">
                        {dlTab === 'own' && (
                            <>
                                <button className={`period-tab ${dlType === 'timesheet' ? 'active' : ''}`} onClick={() => setDlType('timesheet')}>{t('reports.timesheet', 'Karta pracy')}</button>
                                <button className={`period-tab ${dlType === 'overtime'  ? 'active' : ''}`} onClick={() => setDlType('overtime')}>{t('reports.overtime', 'Nadgodziny')}</button>
                            </>
                        )}
                        {dlTab === 'employee' && (
                            <button className="period-tab active">{t('reports.timesheet', 'Karta pracy')}</button>
                        )}
                        {dlTab === 'team' && (
                            <>
                                <button className={`period-tab ${!dlAnomalies && dlType === 'workload'  ? 'active' : ''}`} onClick={() => setDlType('workload')}  disabled={dlAnomalies} style={{ opacity: dlAnomalies ? 0.4 : 1 }}>{t('reports.workload', 'Obciążenie')}</button>
                                <button className={`period-tab ${!dlAnomalies && dlType === 'absences'  ? 'active' : ''}`} onClick={() => setDlType('absences')} disabled={dlAnomalies} style={{ opacity: dlAnomalies ? 0.4 : 1 }}>{t('reports.absencesType', 'Absencje')}</button>
                                <button className={`period-tab ${!dlAnomalies && dlType === 'overtime'  ? 'active' : ''}`} onClick={() => setDlType('overtime')}  disabled={dlAnomalies} style={{ opacity: dlAnomalies ? 0.4 : 1 }}>{t('reports.overtime', 'Nadgodziny')}</button>
                            </>
                        )}
                    </div>

                    {/* Wykrywanie anomalii — tylko MANAGER, tylko zakładka team */}
                    {dlTab === 'team' && isManager && (
                        <label className={`anomaly-toggle${dlAnomalies ? ' anomaly-toggle--active' : ''}`}>
                            <input
                                type="checkbox"
                                checked={dlAnomalies}
                                onChange={e => setDlAnomalies(e.target.checked)}
                            />
                            <AlertTriangle size={14} />
                            {t('reports.anomalyDetection', 'Wykrywanie anomalii')}
                            <span style={{ fontWeight: 400, fontSize: '0.78rem', marginLeft: '0.25rem' }}>
                                — {t('reports.anomalyHint', 'Raport pracowników z odchyleniem bilansu >10% od planu')}
                            </span>
                        </label>
                    )}

                    {/* Wybór pracownika */}
                    {dlTab === 'employee' && (
                        <select className="filter-select" value={dlEmployeeId} onChange={e => setDlEmployeeId(e.target.value)}>
                            <option value="">{t('reports.selectEmployee', '-- Wybierz pracownika --')}</option>
                            {subordinates.map(s => (
                                <option key={s.userID} value={s.userID}>{s.name} {s.surname} ({s.email})</option>
                            ))}
                        </select>
                    )}

                    {/* Zakres dat */}
                    <div className="dl-date-row">
                        <span className="dl-label">{t('reports.dateFrom', 'Od')}</span>
                        <input type="date" className="filter-input" value={dlStartDate}
                            onChange={e => { setDlStartDate(e.target.value); if (dlEndDate && e.target.value > dlEndDate) setDlEndDate(''); }} />
                        <span className="dl-label">{t('reports.dateTo', 'Do')}</span>
                        <input type="date" className="filter-input" value={dlEndDate}
                            min={dlStartDate || undefined}
                            onChange={e => setDlEndDate(e.target.value)} />
                    </div>

                    {/* Format + przycisk */}
                    <div className="dl-footer">
                        <FormatPicker value={dlFormat} onChange={setDlFormat} />
                        <button className="report-dl-btn" onClick={handleDownload} disabled={downloading}>
                            <Download size={16} />
                            {downloading ? t('reports.generating', 'Generowanie…') : t('reports.downloadBtn', 'Pobierz raport')}
                        </button>
                    </div>
                </div>
            </Card>
            </div>

        </div>
    );
};

export default Reports;
