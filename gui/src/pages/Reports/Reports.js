import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Clock, CalendarOff, TrendingUp, TrendingDown, Download, ChevronLeft, ChevronRight } from 'lucide-react';
import { Card } from '../../components';
import { userService, reportService } from '../../services';
import '../main.css';
import './reports.css';

// ── Helpers ──────────────────────────────────────────────────────────────────

const MONTHS = [
    'Styczeń','Luty','Marzec','Kwiecień','Maj','Czerwiec',
    'Lipiec','Sierpień','Wrzesień','Październik','Listopad','Grudzień',
];

const pad = (n) => String(n).padStart(2, '0');

const monthBounds = (year, month) => {
    const lastDay = new Date(year, month + 1, 0).getDate();
    return {
        startDate: `${year}-${pad(month + 1)}-01`,
        endDate:   `${year}-${pad(month + 1)}-${pad(lastDay)}`,
    };
};

const toH = (sec) => +(Math.abs(sec) / 3600).toFixed(1);

const ABSENCE_META = {
    HOLIDAY_LEAVE:  { label: 'Urlop wypoczynkowy',   color: '#FFDE42' },
    SICK_LEAVE:     { label: 'Zwolnienie lekarskie',  color: '#ef4444' },
    HOME_OFFICE:    { label: 'Praca zdalna (HO)',      color: '#3b82f6' },
    EARLY_EXIT:     { label: 'Wcześniejsze wyjście',  color: '#f97316' },
    OVERTIME_EARN:  { label: 'Nadgodziny zapl.',       color: '#16a34a' },
    OVERTIME_TAKE:  { label: 'Odbiór nadgodzin',       color: '#6b7280' },
};

// ── Bar chart ────────────────────────────────────────────────────────────────

const BarChart = ({ data }) => {
    if (!data || !data.length) return <div className="empty">Brak danych do wyświetlenia.</div>;
    const maxRaw = Math.max(...data.map(d => Math.max(d.plan, d.przepr, 0)));
    const roundedMax = Math.ceil(maxRaw / 2) * 2 || 8;
    const W = 560, H = 200, padL = 40, padB = 28, padT = 10, padR = 10;
    const chartW = W - padL - padR;
    const chartH = H - padT - padB;
    const n = data.length;
    const groupW = chartW / n;
    const barW = Math.min(groupW * 0.35, 22);
    const gap = 3;
    const ticks = 4;

    return (
        <svg viewBox={`0 0 ${W} ${H}`} width="100%" style={{ display: 'block' }}>
            {[...Array(ticks + 1)].map((_, i) => {
                const val = +(roundedMax * i / ticks).toFixed(1);
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
                const przeprH = (d.przepr / roundedMax) * chartH;
                return (
                    <g key={i}>
                        <rect x={cx - barW - gap / 2} y={padT + chartH - planH}  width={barW} height={planH}  fill="#e5e7eb" rx="3" />
                        {przeprH > 0 && (
                            <rect x={cx + gap / 2} y={padT + chartH - przeprH} width={barW} height={przeprH} fill="#FFDE42" rx="3" />
                        )}
                        <text x={cx} y={padT + chartH + 16} textAnchor="middle" fontSize="9" fill="#9ca3af">{d.label}</text>
                    </g>
                );
            })}
            <line x1={padL} y1={padT} x2={padL} y2={padT + chartH} stroke="#e5e7eb" strokeWidth="1" />
            <line x1={padL} y1={padT + chartH} x2={padL + chartW} y2={padT + chartH} stroke="#e5e7eb" strokeWidth="1" />
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
            setError('Nie udało się załadować danych. Sprawdź, czy backend jest uruchomiony.');
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
            if (!weeks[key]) weeks[key] = { przepr: 0, plan: 0, label: `${mon.getDate()}.${pad(mon.getMonth() + 1)}` };
            weeks[key].przepr += e.workSeconds;
            weeks[key].plan   += e.requiredSeconds;
        });
        return Object.values(weeks).map(w => ({ ...w, przepr: toH(w.przepr), plan: toH(w.plan) }));
    }, [timesheetData]);

    const absenceBreakdown = useMemo(() => {
        if (!timesheetData?.dailyEntries) return [];
        const counts = {};
        timesheetData.dailyEntries.forEach(e => {
            if (e.applicationType) counts[e.applicationType] = (counts[e.applicationType] || 0) + 1;
        });
        return Object.entries(counts).map(([type, days]) => ({
            label: ABSENCE_META[type]?.label || type,
            color: ABSENCE_META[type]?.color || '#d1d5db',
            days,
        }));
    }, [timesheetData]);

    const overtimeByWeek = useMemo(() => {
        if (!timesheetData?.dailyEntries?.length) return [];
        const weeks = {};
        timesheetData.dailyEntries.forEach(e => {
            const d = new Date(e.date);
            const mon = new Date(d);
            mon.setDate(d.getDate() - ((d.getDay() + 6) % 7));
            const key = mon.toISOString().slice(0, 10);
            if (!weeks[key]) weeks[key] = { val: 0, label: `${mon.getDate()}.${pad(mon.getMonth() + 1)}` };
            weeks[key].val += e.balanceSeconds;
        });
        return Object.values(weeks).map(w => ({ ...w, val: +(w.val / 3600).toFixed(1) }));
    }, [timesheetData]);

    const maxOT  = Math.max(...overtimeByWeek.map(w => Math.abs(w.val)), 1);
    const td     = timesheetData;
    const workH  = td ? toH(td.totalWork)     : null;
    const planH  = td ? toH(td.totalRequired) : null;
    const balH   = td ? +(td.totalBalance / 3600).toFixed(1) : null;
    const pct    = workH != null && planH > 0 ? Math.min(Math.round((workH / planH) * 100), 100) : 0;
    const absTotal = absenceBreakdown.reduce((s, a) => s + a.days, 0);

    const isManager       = userRole === 'MANAGER';
    const canViewEmployee = userRole === 'MANAGER' || userRole === 'HR' || userRole === 'ADMIN';

    // ── Download handler ──────────────────────────────────────────────────────

    const handleDownload = async () => {
        if (!dlStartDate || !dlEndDate) { alert('Wybierz zakres dat.'); return; }
        if (dlStartDate > dlEndDate)    { alert('Data "Od" musi być wcześniejsza niż "Do".'); return; }
        setDownloading(true);
        try {
            if (dlTab === 'own') {
                if (dlType === 'timesheet') await reportService.downloadMyTimesheet(dlStartDate, dlEndDate, dlFormat);
                else                        await reportService.downloadMyOvertime(dlStartDate, dlEndDate, dlFormat);
            } else if (dlTab === 'employee') {
                if (!dlEmployeeId) { alert('Wybierz pracownika.'); return; }
                await reportService.downloadEmployeeTimesheet(dlEmployeeId, dlStartDate, dlEndDate, dlFormat);
            } else if (dlTab === 'team') {
                if (dlType === 'workload')  await reportService.downloadTeamWorkload(dlStartDate, dlEndDate, dlFormat);
                else if (dlType === 'absences') await reportService.downloadTeamAbsences(dlStartDate, dlEndDate, dlFormat);
                else                        await reportService.downloadTeamOvertime(dlStartDate, dlEndDate, dlFormat);
            }
        } catch (e) {
            alert(e.message || 'Błąd podczas pobierania raportu.');
        } finally {
            setDownloading(false);
        }
    };

    // ── Render ────────────────────────────────────────────────────────────────

    return (
        <div className="dashboard">

            {/* Nagłówek */}
            <div className="raporty-header">
                <h2 className="dashboard-title">Raporty</h2>
                <div className="month-selector">
                    <button className="month-nav" onClick={prevMonth}><ChevronLeft size={16} /></button>
                    <span className="month-label">{MONTHS[month]} {year}</span>
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
                        <span className="tile-label">Przepracowane</span>
                        <span className="tile-sub">Plan: {loading ? '…' : planH}h</span>
                    </div>
                    {!loading && planH > 0 && (
                        <>
                            <div className="tile-progress-track">
                                <div className="tile-progress-fill" style={{ width: `${pct}%` }} />
                            </div>
                            <span className="tile-pct">{pct}% planu</span>
                        </>
                    )}
                </div>

                <div className="raporty-tile">
                    <div className={`tile-icon ${!loading && balH < 0 ? 'tile-icon--red' : 'tile-icon--green'}`}>
                        {!loading && balH < 0 ? <TrendingDown size={20} /> : <TrendingUp size={20} />}
                    </div>
                    <div className="tile-content">
                        <span className="tile-value">
                            {loading ? '…' : `${balH > 0 ? '+' : ''}${balH}`}
                            <span className="tile-unit">h</span>
                        </span>
                        <span className="tile-label">Bilans</span>
                        <span className="tile-sub">nadgodziny / niedobór</span>
                    </div>
                </div>

                <div className="raporty-tile">
                    <div className="tile-icon tile-icon--red"><CalendarOff size={20} /></div>
                    <div className="tile-content">
                        <span className="tile-value">{loading ? '…' : absTotal}<span className="tile-unit">dni</span></span>
                        <span className="tile-label">Nieobecności</span>
                        <span className="tile-sub">w tym miesiącu</span>
                    </div>
                </div>
            </div>

            {/* Wykres */}
            <Card title="Godziny pracy — tygodniowo">
                <div className="chart-controls">
                    <div className="raporty-legend">
                        <span className="raporty-legend-item">
                            <span className="raporty-legend-box raporty-legend-box--gray" /> Plan
                        </span>
                        <span className="raporty-legend-item">
                            <span className="raporty-legend-box raporty-legend-box--yellow" /> Przepracowane
                        </span>
                    </div>
                </div>
                {loading ? <div className="empty">Ładowanie danych…</div> : <BarChart data={chartData} />}
            </Card>

            {/* Dolny rząd */}
            <div className="raporty-bottom">
                <Card title="Nieobecności w miesiącu">
                    {loading ? <div className="empty">Ładowanie…</div>
                        : absenceBreakdown.length === 0
                            ? <div className="empty">Brak nieobecności w tym okresie.</div>
                            : (
                                <div className="absence-list">
                                    {absenceBreakdown.map(item => (
                                        <div key={item.label} className="absence-row">
                                            <div className="absence-info">
                                                <span className="absence-dot" style={{ background: item.color }} />
                                                <span className="absence-name">{item.label}</span>
                                            </div>
                                            <span className="absence-days">{item.days} dni</span>
                                        </div>
                                    ))}
                                </div>
                            )
                    }
                </Card>

                <Card title="Bilans tygodniowy">
                    {loading ? <div className="empty">Ładowanie…</div>
                        : overtimeByWeek.length === 0
                            ? <div className="empty">Brak danych.</div>
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
            <Card title="Pobierz raport">
                <div className="dl-section">

                    {/* Zakładki: Moje / Pracownika / Zespołu */}
                    <div className="dl-tabs">
                        <button className={`period-tab ${dlTab === 'own' ? 'active' : ''}`}
                            onClick={() => { setDlTab('own'); setDlType('timesheet'); }}>
                            Moje raporty
                        </button>
                        {canViewEmployee && (
                            <button className={`period-tab ${dlTab === 'employee' ? 'active' : ''}`}
                                onClick={() => { setDlTab('employee'); setDlType('timesheet'); }}>
                                Raport pracownika
                            </button>
                        )}
                        {isManager && (
                            <button className={`period-tab ${dlTab === 'team' ? 'active' : ''}`}
                                onClick={() => { setDlTab('team'); setDlType('workload'); }}>
                                Raporty zespołu
                            </button>
                        )}
                    </div>

                    {/* Typ raportu */}
                    <div className="dl-type-row">
                        {dlTab === 'own' && (
                            <>
                                <button className={`period-tab ${dlType === 'timesheet' ? 'active' : ''}`} onClick={() => setDlType('timesheet')}>Karta pracy</button>
                                <button className={`period-tab ${dlType === 'overtime'  ? 'active' : ''}`} onClick={() => setDlType('overtime')}>Nadgodziny</button>
                            </>
                        )}
                        {dlTab === 'employee' && (
                            <button className="period-tab active">Karta pracy</button>
                        )}
                        {dlTab === 'team' && (
                            <>
                                <button className={`period-tab ${dlType === 'workload'  ? 'active' : ''}`} onClick={() => setDlType('workload')}>Obciążenie</button>
                                <button className={`period-tab ${dlType === 'absences'  ? 'active' : ''}`} onClick={() => setDlType('absences')}>Absencje</button>
                                <button className={`period-tab ${dlType === 'overtime'  ? 'active' : ''}`} onClick={() => setDlType('overtime')}>Nadgodziny</button>
                            </>
                        )}
                    </div>

                    {/* Wybór pracownika */}
                    {dlTab === 'employee' && (
                        <select className="filter-select" value={dlEmployeeId} onChange={e => setDlEmployeeId(e.target.value)}>
                            <option value="">-- Wybierz pracownika --</option>
                            {subordinates.map(s => (
                                <option key={s.userID} value={s.userID}>{s.name} {s.surname} ({s.email})</option>
                            ))}
                        </select>
                    )}

                    {/* Zakres dat */}
                    <div className="dl-date-row">
                        <span className="dl-label">Od</span>
                        <input type="date" className="filter-input" value={dlStartDate}
                            onChange={e => { setDlStartDate(e.target.value); if (dlEndDate && e.target.value > dlEndDate) setDlEndDate(''); }} />
                        <span className="dl-label">Do</span>
                        <input type="date" className="filter-input" value={dlEndDate}
                            min={dlStartDate || undefined}
                            onChange={e => setDlEndDate(e.target.value)} />
                    </div>

                    {/* Format + przycisk */}
                    <div className="dl-footer">
                        <FormatPicker value={dlFormat} onChange={setDlFormat} />
                        <button className="report-dl-btn" onClick={handleDownload} disabled={downloading}>
                            <Download size={16} />
                            {downloading ? 'Generowanie…' : 'Pobierz raport'}
                        </button>
                    </div>
                </div>
            </Card>
            </div>

        </div>
    );
};

export default Reports;
