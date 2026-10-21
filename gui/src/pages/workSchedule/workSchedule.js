import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { PlusCircle } from 'lucide-react';
import { policiesService, applicationService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import '../main.css';
import './workSchedule.css';

const HOURLY_TYPES = ['EARLY_EXIT', 'OVERTIME_EARN', 'OVERTIME_TAKE'];
const VISIBLE_STATUSES = ['APPROVED', 'PENDING'];

function generateMonth(year, month) {
    const first = new Date(year, month, 1);
    const last = new Date(year, month + 1, 0);
    const days = [];

    const startDay = first.getDay();
    for (let i = 0; i < startDay; i++) {
        days.push(null);
    }

    for (let d = 1; d <= last.getDate(); d++) {
        days.push(new Date(year, month, d));
    }

    while (days.length % 7 !== 0) days.push(null);
    return days;
}

const formatKey = (d) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

const parseKey = (key) => {
    const [y, m, d] = key.split('-').map(Number);
    return new Date(y, m - 1, d);
};

const WorkSchedule = () => {
    const { t } = useLanguage();
    const navigate = useNavigate();
    const today = new Date();
    const [view, setView] = useState({ year: today.getFullYear(), month: today.getMonth() });
    const [selectedDate, setSelectedDate] = useState(null);
    const [holidays, setHolidays] = useState([]);
    const [applications, setApplications] = useState([]);

    useEffect(() => {
        let cancelled = false;

        // Endpoint świąt dostępny jest tylko dla ról MANAGER/ADMIN - dla pozostałych
        // kalendarz po prostu nie pokaże świąt (brak błędu dla użytkownika).
        policiesService.getAllHolidays()
            .then(data => { if (!cancelled) setHolidays(Array.isArray(data) ? data : []); })
            .catch(() => { if (!cancelled) setHolidays([]); });

        applicationService.getUserApplications()
            .then(data => { if (!cancelled) setApplications(Array.isArray(data) ? data : []); })
            .catch(() => { if (!cancelled) setApplications([]); });

        return () => { cancelled = true; };
    }, []);

    const typeLabels = {
        HOLIDAY_LEAVE: t('requests.types.holiday', 'Urlop wypoczynkowy'),
        SICK_LEAVE: t('requests.types.sick', 'Zwolnienie lekarskie'),
        EARLY_EXIT: t('requests.types.earlyExit', 'Wyjście wcześniejsze'),
        OVERTIME_EARN: t('requests.types.overtimeEarn', 'Zgłoszenie nadgodzin'),
        OVERTIME_TAKE: t('requests.types.overtimeTake', 'Odbiór nadgodzin'),
        HOME_OFFICE: t('reports.homeOffice', 'Praca zdalna (HO)'),
    };

    // Mapa: 'YYYY-MM-DD' -> { holidays: [...], requests: [...] }
    const dayMap = useMemo(() => {
        const map = {};
        const ensure = (key) => (map[key] = map[key] || { holidays: [], requests: [] });

        holidays.forEach(h => {
            if (h.date) ensure(h.date).holidays.push({ id: `h-${h.holidayId}`, name: h.name });
        });

        applications
            .filter(a => VISIBLE_STATUSES.includes(a.status) && a.startDate)
            .forEach(a => {
                const start = parseKey(a.startDate);
                const end = parseKey(a.endDate || a.startDate);
                // zabezpieczenie przed błędnym zakresem
                for (let d = new Date(start), i = 0; d <= end && i < 400; d.setDate(d.getDate() + 1), i++) {
                    ensure(formatKey(d)).requests.push({
                        id: `a-${a.id}`,
                        type: a.type,
                        status: a.status,
                        title: typeLabels[a.type] || a.type,
                        hours: HOURLY_TYPES.includes(a.type) && a.durationSeconds
                            ? Math.round(a.durationSeconds / 3600)
                            : null,
                    });
                }
            });

        return map;
    }, [holidays, applications, t]);

    const days = generateMonth(view.year, view.month);
    const monthName = new Date(view.year, view.month).toLocaleString('default', { month: 'long' });

    const prevMonth = () => {
        const m = view.month - 1;
        if (m < 0) setView({ year: view.year - 1, month: 11 });
        else setView({ ...view, month: m });
    };
    const nextMonth = () => {
        const m = view.month + 1;
        if (m > 11) setView({ year: view.year + 1, month: 0 });
        else setView({ ...view, month: m });
    };

    const openRequestForm = (date) => {
        const key = formatKey(date);
        navigate('/requests', {
            state: {
                openForm: true,
                prefillType: 'HOLIDAY_LEAVE',
                prefillStartDate: key,
                prefillEndDate: key,
            },
        });
    };

    const selectedKey = selectedDate ? formatKey(selectedDate) : null;
    const selectedInfo = selectedKey ? (dayMap[selectedKey] || { holidays: [], requests: [] }) : null;

    return (
        <div className="grafik-root">
            <div className="grafik-header">
                <div className="grafik-header-left">
                    <button className="btn" onClick={prevMonth}>◀</button>
                    <div className="grafik-title">{monthName} {view.year}</div>
                    <button className="btn" onClick={nextMonth}>▶</button>
                </div>
                <div>
                    <button
                        className="btn btn-outline"
                        onClick={() => { setView({ year: today.getFullYear(), month: today.getMonth() }); setSelectedDate(null); }}
                    >
                        {t('schedule.today', 'Dziś')}
                    </button>
                </div>
            </div>

            <div className="grafik-legend">
                <span className="grafik-legend-item"><span className="legend-dot legend-dot--holiday" />{t('schedule.legendHoliday', 'Dzień wolny / święto')}</span>
                <span className="grafik-legend-item"><span className="legend-dot legend-dot--approved" />{t('schedule.legendApproved', 'Zatwierdzony wniosek')}</span>
                <span className="grafik-legend-item"><span className="legend-dot legend-dot--pending" />{t('schedule.legendPending', 'Wniosek oczekujący')}</span>
            </div>

            <div className="grafik-scroll">
                <div className="grafik-grid">
                    {['Ndz', 'Pon', 'Wt', 'Śr', 'Czw', 'Pt', 'Sob'].map(d => (
                        <div key={d} className="grafik-weekday">{d}</div>
                    ))}

                    {days.map((d, idx) => {
                        const key = d ? formatKey(d) : `empty-${idx}`;
                        const info = d ? dayMap[key] : null;
                        const dayHolidays = info ? info.holidays : [];
                        const dayRequests = info ? info.requests : [];
                        const count = dayHolidays.length + dayRequests.length;
                        const isToday = d && d.toDateString() === today.toDateString();
                        const isSelected = d && selectedKey === key;
                        const classes = ['grafik-cell'];
                        if (!d) classes.push('grafik-cell--empty');
                        if (dayHolidays.length > 0) classes.push('grafik-cell--holiday');
                        if (isSelected) classes.push('grafik-cell--selected');

                        const firstLine = dayHolidays[0]
                            ? dayHolidays[0].name
                            : (dayRequests[0] ? dayRequests[0].title : '');

                        return (
                            <div
                                key={key}
                                className={classes.join(' ')}
                                onClick={() => d && setSelectedDate(d)}
                            >
                                {d && (
                                    <div className="grafik-cell-inner">
                                        <div className="grafik-cell-top">
                                            <div className={`day-number ${isToday ? 'day-number--today' : ''}`}>{d.getDate()}</div>
                                            <div className="day-events">
                                                {count > 0 && <span className="event-badge">{count}</span>}
                                            </div>
                                        </div>

                                        <div
                                            className={`event-line ${dayHolidays.length > 0 ? 'event-line--holiday' : ''} ${dayHolidays.length === 0 && dayRequests[0] ? `event-line--${dayRequests[0].status.toLowerCase()}` : ''}`}
                                        >
                                            {firstLine}
                                        </div>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>

            <div className="grafik-panel-wrap">
                {selectedDate ? (
                    <div className="grafik-panel">
                        <div className="grafik-panel-head">
                            <h3 className="grafik-panel-title">
                                {t('schedule.eventsFor', 'Wydarzenia')} — {selectedDate.toLocaleDateString('pl-PL', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
                            </h3>
                            <button
                                type="button"
                                className="btn btn-primary grafik-request-btn"
                                onClick={() => openRequestForm(selectedDate)}
                            >
                                <PlusCircle size={16} />
                                <span>{t('schedule.createRequest', 'Złóż wniosek')}</span>
                            </button>
                        </div>

                        {selectedInfo.holidays.length === 0 && selectedInfo.requests.length === 0 ? (
                            <div className="grafik-panel-empty">{t('schedule.noEvents', 'Brak wydarzeń tego dnia.')}</div>
                        ) : (
                            <ul className="grafik-event-list">
                                {selectedInfo.holidays.map(h => (
                                    <li key={h.id} className="grafik-event-item grafik-event-item--holiday">
                                        <span className="legend-dot legend-dot--holiday" />
                                        <span>{h.name}</span>
                                        <span className="grafik-event-tag">{t('schedule.holidayTag', 'Dzień wolny')}</span>
                                    </li>
                                ))}
                                {selectedInfo.requests.map(r => (
                                    <li key={r.id} className="grafik-event-item">
                                        <span className={`legend-dot legend-dot--${r.status.toLowerCase()}`} />
                                        <span>{r.title}{r.hours ? ` (${r.hours} ${t('manager.hoursCountLabel', 'godz.')})` : ''}</span>
                                        <span className="grafik-event-tag">
                                            {r.status === 'APPROVED'
                                                ? t('requests.approved', 'Zatwierdzony')
                                                : t('requests.pending', 'Oczekujący')}
                                        </span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                ) : (
                    <div className="grafik-panel grafik-panel--empty">
                        {t('schedule.selectDay', 'Wybierz dzień by zobaczyć wydarzenia')}
                    </div>
                )}
            </div>
        </div>
    );
};

export default WorkSchedule;
