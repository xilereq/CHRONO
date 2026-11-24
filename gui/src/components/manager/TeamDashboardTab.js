import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
    Building2, Home, Palmtree, UserX, RefreshCw, Users, Search, ChevronLeft, ChevronRight,
} from 'lucide-react';
import { teamDashboardService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import {
    STATUSES, normalizeDashboard, todayIso, shiftIsoDate, dateLimits, initials, percent,
} from './teamDashboardUtils';
import './manager.css';
import './teamDashboard.css';

const STATUS_ICONS = { office: Building2, home: Home, leave: Palmtree, none: UserX };

const ERROR_KEYS = {
    UNAUTHORIZED_MANAGER: ['manager.dashboard.forbiddenError', 'Brak dostępu do dashboardu tego zespołu.'],
    INVALID_MANAGER_ID: ['manager.dashboard.invalidManagerError', 'Nieprawidłowy menedżer.'],
    DASHBOARD_DATE_OUT_OF_RANGE: ['manager.dashboard.dateRangeError', 'Data musi mieścić się w zakresie ±365 dni od dziś.'],
};

const LOAD_ERROR = ['manager.dashboard.loadError', 'Nie udało się pobrać danych zespołu.'];
const NOT_AVAILABLE_ERROR = ['manager.dashboard.notAvailableError', 'Dashboard zespołu nie jest dostępny na serwerze (brak endpointu /api/team-dashboard).'];

const StackedBar = ({ counts, total, labels }) => (
    <div
        className="team-dash-bar"
        role="img"
        aria-label={STATUSES.map(s => `${labels[s]}: ${counts[s]}`).join(', ')}
    >
        {total === 0
            ? <span className="team-dash-bar-empty" />
            : STATUSES.filter(s => counts[s] > 0).map(s => (
                <span
                    key={s}
                    className={`team-dash-bar-seg team-dash-status-${s}`}
                    style={{ flexGrow: counts[s] }}
                    title={`${labels[s]}: ${counts[s]} (${percent(counts[s], total)}%)`}
                />
            ))}
    </div>
);

const TeamDashboardTab = ({ represented = [], isRealManager = true }) => {
    const { t, language } = useLanguage();
    const [date, setDate] = useState(todayIso);
    const [managerId, setManagerId] = useState(
        () => (isRealManager ? '' : String(represented[0]?.managerId ?? ''))
    );
    const [dashboard, setDashboard] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null); // [kluczTłumaczenia, tekstDomyślny]
    const [updatedAt, setUpdatedAt] = useState(null);
    const [statusFilter, setStatusFilter] = useState(null);
    const [query, setQuery] = useState('');

    const limits = useMemo(dateLimits, []);
    const isToday = date === todayIso();

    const labels = {
        office: t('manager.dashboard.office', 'W biurze'),
        home: t('manager.dashboard.home', 'Zdalnie'),
        leave: t('manager.dashboard.leave', 'Urlop'),
        none: t('manager.dashboard.none', 'Brak rejestracji'),
    };

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await teamDashboardService.getDashboard({
                date,
                managerId: managerId ? Number(managerId) : undefined,
            });
            setDashboard(normalizeDashboard(data));
            setUpdatedAt(new Date());
        } catch (err) {
            setError(ERROR_KEYS[err.code] || (err.status === 404 ? NOT_AVAILABLE_ERROR : LOAD_ERROR));
            setDashboard(null);
        } finally {
            setLoading(false);
        }
    }, [date, managerId]);

    useEffect(() => { load(); }, [load]);

    const changeDate = (value) => {
        if (!value) return;
        if (value < limits.min || value > limits.max) {
            setError(ERROR_KEYS.DASHBOARD_DATE_OUT_OF_RANGE);
            return;
        }
        setStatusFilter(null);
        setDate(value);
    };

    const visiblePeople = useMemo(() => {
        if (!dashboard) return [];
        const q = query.trim().toLowerCase();
        return dashboard.members
            .filter(p => !statusFilter || p.status === statusFilter)
            .filter(p => !q || p.name.toLowerCase().includes(q))
            .sort((a, b) => STATUSES.indexOf(a.status) - STATUSES.indexOf(b.status) || a.name.localeCompare(b.name));
    }, [dashboard, statusFilter, query]);

    const formattedDate = useMemo(() => {
        const [y, m, d] = date.split('-').map(Number);
        return new Date(y, m - 1, d).toLocaleDateString(language, { weekday: 'long', day: 'numeric', month: 'long' });
    }, [date, language]);

    const personDetail = (p) => {
        if (p.status === 'leave') {
            return p.leaveType ? t(`manager.dashboard.leaveTypes.${p.leaveType}`, p.leaveType) : null;
        }
        if (p.startTime) {
            return `${t('manager.dashboard.since', 'od')} ${p.startTime}`;
        }
        if (p.status === 'home') {
            return t('manager.dashboard.plannedHome', 'zaplanowana praca zdalna');
        }
        return null;
    };

    const counts = dashboard?.counts;
    const total = dashboard?.total ?? 0;
    const present = counts ? counts.office + counts.home : 0;

    return (
        <div className="team-dash">
            <header className="team-dash-header">
                <div>
                    <h2 className="team-dash-title">{t('manager.dashboard.title', 'Dashboard zespołu')}</h2>
                    <p className="team-dash-subtitle">
                        {isToday ? t('manager.dashboard.subtitle', 'Gdzie jest dziś Twój zespół') : formattedDate}
                        {updatedAt && isToday && ` · ${t('manager.dashboard.updated', 'aktualizacja')} ${updatedAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`}
                    </p>
                </div>
                <div className="team-dash-actions">
                    {represented.length > 0 && (
                        <select
                            className="team-dash-select"
                            value={managerId}
                            onChange={e => { setStatusFilter(null); setManagerId(e.target.value); }}
                            aria-label={t('manager.dashboard.teamSelect', 'Zespół')}
                        >
                            {isRealManager && <option value="">{t('manager.dashboard.myTeam', 'Mój zespół')}</option>}
                            {represented.map(d => (
                                <option key={d.id} value={d.managerId}>
                                    {t('manager.dashboard.teamOf', 'Zespół')}: {d.managerName}
                                </option>
                            ))}
                        </select>
                    )}
                    <div className="team-dash-datenav">
                        <button
                            className="team-dash-icon-btn"
                            onClick={() => changeDate(shiftIsoDate(date, -1))}
                            disabled={date <= limits.min}
                            aria-label={t('manager.dashboard.prevDay', 'Poprzedni dzień')}
                        >
                            <ChevronLeft size={16} />
                        </button>
                        <input
                            type="date"
                            className="team-dash-date"
                            value={date}
                            min={limits.min}
                            max={limits.max}
                            onChange={e => changeDate(e.target.value)}
                            aria-label={t('manager.dashboard.date', 'Dzień')}
                        />
                        <button
                            className="team-dash-icon-btn"
                            onClick={() => changeDate(shiftIsoDate(date, 1))}
                            disabled={date >= limits.max}
                            aria-label={t('manager.dashboard.nextDay', 'Następny dzień')}
                        >
                            <ChevronRight size={16} />
                        </button>
                        {!isToday && (
                            <button className="team-dash-today-btn" onClick={() => changeDate(todayIso())}>
                                {t('manager.dashboard.today', 'Dziś')}
                            </button>
                        )}
                    </div>
                    <button
                        className="team-dash-icon-btn"
                        onClick={load}
                        disabled={loading}
                        title={t('manager.dashboard.refresh', 'Odśwież')}
                        aria-label={t('manager.dashboard.refresh', 'Odśwież')}
                    >
                        <RefreshCw size={16} className={loading ? 'team-dash-spin' : ''} />
                    </button>
                </div>
            </header>

            {error && <div className="manager-alert-error">{t(...error)}</div>}

            {loading && !dashboard ? (
                <div className="manager-loading-state">{t('manager.dashboard.loading', 'Ładowanie danych zespołu...')}</div>
            ) : dashboard && total === 0 ? (
                <div className="manager-empty-state">
                    <Users size={40} className="manager-empty-icon" />
                    <p className="manager-empty-text">
                        {t('manager.dashboard.empty', 'Nie masz jeszcze przypisanych pracowników.')}
                    </p>
                </div>
            ) : dashboard && (
                <div className={`team-dash-body ${loading ? 'team-dash-stale' : ''}`}>
                    <section className="team-dash-kpis">
                        {STATUSES.map(s => {
                            const Icon = STATUS_ICONS[s];
                            const active = statusFilter === s;
                            return (
                                <button
                                    key={s}
                                    className={`team-dash-kpi team-dash-status-${s} ${active ? 'active' : ''}`}
                                    onClick={() => setStatusFilter(active ? null : s)}
                                    aria-pressed={active}
                                >
                                    <span className="team-dash-kpi-icon"><Icon size={18} /></span>
                                    <span className="team-dash-kpi-label">{labels[s]}</span>
                                    <span className="team-dash-kpi-value">{counts[s]}</span>
                                    <span className="team-dash-kpi-meta">{percent(counts[s], total)}% {t('manager.dashboard.ofTeam', 'zespołu')}</span>
                                </button>
                            );
                        })}
                    </section>

                    <section className="team-dash-card">
                        <div className="team-dash-card-head">
                            <h3>{t('manager.dashboard.distribution', 'Rozkład dnia')}</h3>
                            <span className="team-dash-muted">
                                {t('manager.dashboard.present', 'Obecni')}: <strong>{present}</strong> / {total}
                                {isToday && (
                                    <> · {t('manager.dashboard.workingNow', 'Teraz pracuje')}: <strong>{dashboard.workingNow}</strong></>
                                )}
                            </span>
                        </div>
                        <StackedBar counts={counts} total={total} labels={labels} />
                        <ul className="team-dash-legend">
                            {STATUSES.map(s => (
                                <li key={s}>
                                    <span className={`team-dash-dot team-dash-status-${s}`} />
                                    {labels[s]} <strong>{counts[s]}</strong>
                                </li>
                            ))}
                        </ul>
                    </section>

                    <section className="team-dash-card">
                        <div className="team-dash-card-head">
                            <h3>
                                {t('manager.dashboard.peopleList', 'Pracownicy')}
                                {statusFilter && (
                                    <button className="team-dash-filter-tag" onClick={() => setStatusFilter(null)}>
                                        {labels[statusFilter]} ×
                                    </button>
                                )}
                            </h3>
                            <label className="team-dash-search">
                                <Search size={14} />
                                <input
                                    type="search"
                                    value={query}
                                    onChange={e => setQuery(e.target.value)}
                                    placeholder={t('manager.dashboard.search', 'Szukaj pracownika')}
                                />
                            </label>
                        </div>
                        {visiblePeople.length === 0 ? (
                            <p className="team-dash-muted team-dash-no-results">{t('manager.dashboard.noResults', 'Brak pracowników spełniających kryteria.')}</p>
                        ) : (
                            <ul className="team-dash-people">
                                {visiblePeople.map(p => {
                                    const detail = personDetail(p);
                                    return (
                                        <li key={p.id}>
                                            <span className="team-dash-avatar">{initials(p.name)}</span>
                                            <span className="team-dash-person">
                                                <span className="team-dash-person-name">{p.name}</span>
                                                {detail && <span className="team-dash-muted">{detail}</span>}
                                            </span>
                                            {isToday && p.currentlyWorking && (
                                                <span className="team-dash-live" title={t('manager.dashboard.workingNow', 'Teraz pracuje')}>
                                                    <span className="team-dash-live-dot" />
                                                    {t('manager.dashboard.live', 'pracuje')}
                                                </span>
                                            )}
                                            <span className={`team-dash-pill team-dash-status-${p.status}`}>{labels[p.status]}</span>
                                        </li>
                                    );
                                })}
                            </ul>
                        )}
                    </section>
                </div>
            )}
        </div>
    );
};

export default TeamDashboardTab;
