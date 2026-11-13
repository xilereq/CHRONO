import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { CalendarClock, Plus, Pencil, Trash2, Send, Power, X, Save, AlertTriangle } from 'lucide-react';
import { Card } from '..';
import { scheduledReportService, userService, applicationService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import './scheduledReports.css';

const EMPLOYEE_TYPES = ['EMPLOYEE_TIMESHEET', 'EMPLOYEE_OVERTIME'];
const TEAM_TYPES = ['TEAM_WORKLOAD', 'TEAM_ABSENCES', 'TEAM_OVERTIME', 'TEAM_ANOMALY'];
const FORMATS = ['CSV', 'PDF', 'XLSX'];
const FREQUENCIES = ['DAILY', 'WEEKLY', 'MONTHLY'];
const PERIODS = [
    'PREVIOUS_DAY', 'CURRENT_WEEK', 'PREVIOUS_WEEK', 'CURRENT_MONTH', 'PREVIOUS_MONTH',
    'LAST_3_MONTHS', 'LAST_6_MONTHS', 'LAST_YEAR', 'CUSTOM',
];

const DEFAULT_LABELS = {
    EMPLOYEE_TIMESHEET: 'Ewidencja czasu pracy pracownika',
    EMPLOYEE_OVERTIME:  'Nadgodziny pracownika',
    TEAM_WORKLOAD:      'Obłożenie zespołu',
    TEAM_ABSENCES:      'Nieobecności w zespole',
    TEAM_OVERTIME:      'Nadgodziny w zespole',
    TEAM_ANOMALY:       'Anomalie czasu pracy w zespole',
    DAILY:   'Codziennie',
    WEEKLY:  'Co tydzień',
    MONTHLY: 'Co miesiąc',
    PREVIOUS_DAY:   'Poprzedni dzień',
    CURRENT_WEEK:   'Bieżący tydzień',
    PREVIOUS_WEEK:  'Poprzedni tydzień',
    CURRENT_MONTH:  'Bieżący miesiąc',
    PREVIOUS_MONTH: 'Poprzedni miesiąc',
    LAST_3_MONTHS:  'Ostatnie 3 miesiące',
    LAST_6_MONTHS:  'Ostatnie 6 miesięcy',
    LAST_YEAR:      'Ostatni rok',
    CUSTOM:         'Własny zakres dat',
};

const DAY_NAMES = ['Poniedziałek', 'Wtorek', 'Środa', 'Czwartek', 'Piątek', 'Sobota', 'Niedziela'];
const DAY_KEYS = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const parseEmails = (text) =>
    [...new Set(text.split(/[\s,;]+/).map(e => e.trim()).filter(Boolean))];

const userLabel = (u) => `${u.name ?? ''} ${u.surname ?? ''}`.trim() || u.email;

const emptyForm = (currentUserId, currentUserEmail) => ({
    name: '',
    reportType: 'EMPLOYEE_TIMESHEET',
    reportFormat: 'PDF',
    frequency: 'WEEKLY',
    dayOfWeek: 1,
    dayOfMonth: 1,
    executionTime: '07:00',
    reportPeriod: 'PREVIOUS_WEEK',
    customStartDate: '',
    customEndDate: '',
    targetUserIds: currentUserId != null ? [currentUserId] : [],
    recipientEmailsText: currentUserEmail || '',
});

const formFromSchedule = (s) => ({
    name: s.name || '',
    reportType: s.reportType,
    reportFormat: s.reportFormat,
    frequency: s.frequency,
    dayOfWeek: s.dayOfWeek ?? 1,
    dayOfMonth: s.dayOfMonth ?? 1,
    executionTime: (s.executionTime || '07:00:00').slice(0, 5),
    reportPeriod: s.reportPeriod,
    customStartDate: s.customStartDate || '',
    customEndDate: s.customEndDate || '',
    targetUserIds: s.targetUserIds || [],
    recipientEmailsText: (s.recipientEmails || []).join(', '),
});


const ScheduledReports = ({ role, currentUserId: knownUserId = null, currentUserEmail, users = [] }) => {
    const { t, language } = useLanguage();
    const L = useCallback((key) => t(`reports.schedules.labels.${key}`, DEFAULT_LABELS[key] || key), [t]);
    const dayName = useCallback((d) => t(`reports.schedules.days.${DAY_KEYS[d - 1]}`, DAY_NAMES[d - 1]), [t]);

    const isEmployee = role === 'EMPLOYEE';
    const isManager = role === 'MANAGER';
    const isPrivileged = role === 'HR' || role === 'ADMIN';

    const [schedules, setSchedules] = useState([]);
    const [loading, setLoading] = useState(true);
    const [listError, setListError] = useState('');
    const [notice, setNotice] = useState('');
    const [busyId, setBusyId] = useState(null);

    const [formOpen, setFormOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [form, setForm] = useState(() => emptyForm(knownUserId, currentUserEmail));
    const [formError, setFormError] = useState('');
    const [saving, setSaving] = useState(false);
    const [userFilter, setUserFilter] = useState('');
    const [managers, setManagers] = useState([]);
    const [currentUserId, setCurrentUserId] = useState(knownUserId);

    const loadSchedules = useCallback(async () => {
        setLoading(true);
        setListError('');
        try {
            setSchedules(await scheduledReportService.getAll());
        } catch (e) {
            setListError(e.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { if (role) loadSchedules(); }, [role, loadSchedules]);

    useEffect(() => {
        if (knownUserId != null) { setCurrentUserId(knownUserId); return; }
        if (!role || loading) return;
        const byEmail = users.find(u => u.email && u.email === currentUserEmail);
        if (byEmail) { setCurrentUserId(byEmail.userID); return; }
        if (!isPrivileged && schedules.length > 0) { setCurrentUserId(schedules[0].creatorId); return; }
        let cancelled = false;
        applicationService.getUserApplications()
            .then(apps => {
                const id = Array.isArray(apps) ? apps.find(a => a.employeeId != null)?.employeeId : null;
                if (!cancelled && id != null) setCurrentUserId(id);
            })
            .catch(() => {});
        return () => { cancelled = true; };
    }, [knownUserId, role, loading, users, currentUserEmail, isPrivileged, schedules]);

    useEffect(() => {
        if (isPrivileged) userService.getManagers().then(setManagers).catch(() => setManagers([]));
    }, [isPrivileged]);

    const isTeamType = TEAM_TYPES.includes(form.reportType);
    const allowedTypes = isEmployee ? EMPLOYEE_TYPES : [...EMPLOYEE_TYPES, ...TEAM_TYPES];

    const targetCandidates = useMemo(() => {
        if (isEmployee) return [];
        let list;
        if (isManager) {
            const self = { userID: currentUserId, name: t('reports.schedules.me', 'Ja'), surname: '', email: currentUserEmail };
            list = [self, ...users.filter(u => u.userID !== currentUserId)];
        } else {
            list = isTeamType && managers.length ? managers : users;
        }
        return list.filter(u => u.userID != null);
    }, [isEmployee, isManager, isTeamType, users, managers, currentUserId, currentUserEmail, t]);

    const filteredCandidates = useMemo(() => {
        const q = userFilter.trim().toLowerCase();
        if (!q) return targetCandidates;
        return targetCandidates.filter(u => `${userLabel(u)} ${u.email}`.toLowerCase().includes(q));
    }, [targetCandidates, userFilter]);

    const userNameById = useMemo(() => {
        const map = new Map();
        [...users, ...managers].forEach(u => map.set(u.userID, userLabel(u)));
        if (currentUserId != null) map.set(currentUserId, t('reports.schedules.me', 'Ja'));
        return map;
    }, [users, managers, currentUserId, t]);

    const setField = (field, value) => setForm(f => ({ ...f, [field]: value }));

    const openCreate = () => {
        setEditingId(null);
        setForm(emptyForm(currentUserId, currentUserEmail));
        setFormError('');
        setUserFilter('');
        setFormOpen(true);
    };

    const openEdit = (s) => {
        setEditingId(s.scheduleId);
        setForm(formFromSchedule(s));
        setFormError('');
        setUserFilter('');
        setFormOpen(true);
    };

    const closeForm = () => {
        setFormOpen(false);
        setEditingId(null);
        setFormError('');
    };

    const changeReportType = (type) => {
        setForm(f => {
            const next = { ...f, reportType: type };
            if (isManager && TEAM_TYPES.includes(type) && !TEAM_TYPES.includes(f.reportType)) {
                next.targetUserIds = [currentUserId];
            }
            if (isPrivileged && TEAM_TYPES.includes(type) !== TEAM_TYPES.includes(f.reportType)) {
                next.targetUserIds = [];
            }
            return next;
        });
    };

    const toggleTarget = (id) => {
        setForm(f => ({
            ...f,
            targetUserIds: f.targetUserIds.includes(id)
                ? f.targetUserIds.filter(x => x !== id)
                : [...f.targetUserIds, id],
        }));
    };

    const buildPayload = () => {
        const recipients = isEmployee ? [currentUserEmail] : parseEmails(form.recipientEmailsText);
        const targets = isEmployee ? [currentUserId] : form.targetUserIds;
        return {
            name: form.name.trim(),
            reportType: form.reportType,
            reportFormat: form.reportFormat,
            frequency: form.frequency,
            dayOfWeek: form.frequency === 'WEEKLY' ? Number(form.dayOfWeek) : null,
            dayOfMonth: form.frequency === 'MONTHLY' ? Number(form.dayOfMonth) : null,
            executionTime: form.executionTime ? `${form.executionTime}:00` : null,
            reportPeriod: form.reportPeriod,
            customStartDate: form.reportPeriod === 'CUSTOM' ? form.customStartDate || null : null,
            customEndDate: form.reportPeriod === 'CUSTOM' ? form.customEndDate || null : null,
            targetUserIds: targets,
            recipientEmails: recipients,
        };
    };

    const validate = (p) => {
        if (!p.name) return t('reports.schedules.errName', 'Podaj nazwę harmonogramu.');
        if (p.name.length > 120) return t('reports.schedules.errNameLength', 'Nazwa może mieć maksymalnie 120 znaków.');
        if (!allowedTypes.includes(p.reportType)) return t('reports.schedules.errType', 'Nie masz uprawnień do tego typu raportu.');
        if (p.frequency === 'WEEKLY' && !(p.dayOfWeek >= 1 && p.dayOfWeek <= 7))
            return t('reports.schedules.errDayOfWeek', 'Wybierz dzień tygodnia.');
        if (p.frequency === 'MONTHLY' && !(p.dayOfMonth >= 1 && p.dayOfMonth <= 28))
            return t('reports.schedules.errDayOfMonth', 'Dzień miesiąca musi być z zakresu 1–28.');
        if (p.reportPeriod === 'CUSTOM') {
            if (!p.customStartDate || !p.customEndDate)
                return t('reports.schedules.errCustomDates', 'Podaj datę początkową i końcową.');
            if (p.customStartDate > p.customEndDate)
                return t('reports.schedules.errCustomOrder', 'Data początkowa nie może być późniejsza niż końcowa.');
        }
        if (p.targetUserIds.length === 0 || p.targetUserIds.some(id => id == null))
            return t('reports.schedules.errTargets', 'Wybierz co najmniej jedną osobę objętą raportem.');
        if (p.recipientEmails.length === 0 || p.recipientEmails.some(e => !e))
            return t('reports.schedules.errRecipients', 'Podaj co najmniej jeden adres e-mail odbiorcy.');
        const bad = p.recipientEmails.find(e => !EMAIL_RE.test(e));
        if (bad) return `${t('reports.schedules.errEmailFormat', 'Nieprawidłowy adres e-mail:')} ${bad}`;
        return null;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const payload = buildPayload();
        const err = validate(payload);
        if (err) { setFormError(err); return; }

        setSaving(true);
        setFormError('');
        try {
            if (editingId != null) {
                const updated = await scheduledReportService.update(editingId, payload);
                setSchedules(list => list.map(s => s.scheduleId === editingId ? updated : s));
                setNotice(t('reports.schedules.updated', 'Harmonogram został zaktualizowany.'));
            } else {
                const created = await scheduledReportService.create(payload);
                setSchedules(list => [...list, created]);
                setNotice(t('reports.schedules.created', 'Harmonogram został utworzony.'));
            }
            closeForm();
        } catch (ex) {
            setFormError(ex.message);
        } finally {
            setSaving(false);
        }
    };

    const runAction = async (id, fn) => {
        setBusyId(id);
        setListError('');
        setNotice('');
        try {
            await fn();
        } catch (e) {
            setListError(e.message);
        } finally {
            setBusyId(null);
        }
    };

    const handleToggle = (s) => runAction(s.scheduleId, async () => {
        const updated = await scheduledReportService.toggle(s.scheduleId);
        setSchedules(list => list.map(x => x.scheduleId === s.scheduleId ? updated : x));
    });

    const handleTrigger = (s) => runAction(s.scheduleId, async () => {
        await scheduledReportService.trigger(s.scheduleId);
        setNotice(`${t('reports.schedules.triggered', 'Raport został wygenerowany i wysłany:')} ${s.name}`);
        try {
            const fresh = await scheduledReportService.getById(s.scheduleId);
            setSchedules(list => list.map(x => x.scheduleId === s.scheduleId ? fresh : x));
        } catch { }
    });

    const handleDelete = (s) => {
        if (!window.confirm(`${t('reports.schedules.confirmDelete', 'Czy na pewno usunąć harmonogram')} „${s.name}”?`)) return;
        runAction(s.scheduleId, async () => {
            await scheduledReportService.remove(s.scheduleId);
            setSchedules(list => list.filter(x => x.scheduleId !== s.scheduleId));
            if (editingId === s.scheduleId) closeForm();
            setNotice(t('reports.schedules.deleted', 'Harmonogram został usunięty.'));
        });
    };

    const locale = language === 'pl' ? 'pl-PL' : language === 'de' ? 'de-DE' : 'en-GB';
    const fmtDateTime = (iso) => {
        if (!iso) return '—';
        const d = new Date(iso);
        return isNaN(d) ? iso : d.toLocaleString(locale, { dateStyle: 'short', timeStyle: 'short' });
    };

    const describeFrequency = (s) => {
        const time = (s.executionTime || '').slice(0, 5);
        if (s.frequency === 'WEEKLY' && s.dayOfWeek) return `${L('WEEKLY')} · ${dayName(s.dayOfWeek)} ${time}`;
        if (s.frequency === 'MONTHLY' && s.dayOfMonth) return `${L('MONTHLY')} · ${s.dayOfMonth}. ${t('reports.schedules.dayShort', 'dzień')} ${time}`;
        return `${L(s.frequency)} ${time}`;
    };

    const describePeriod = (s) =>
        s.reportPeriod === 'CUSTOM' ? `${s.customStartDate} – ${s.customEndDate}` : L(s.reportPeriod);

    const describeTargets = (ids = []) => {
        const names = ids.map(id => userNameById.get(id) || `#${id}`);
        return names.length > 3 ? `${names.slice(0, 3).join(', ')} +${names.length - 3}` : names.join(', ');
    };

    const canCreate = !isEmployee || (currentUserId != null && !!currentUserEmail);
    const missingIdInfo = !loading && isEmployee && !canCreate;

    return (
        <Card title={t('reports.schedules.title', 'Harmonogramy raportów')}>
            <div className="sr-section">
                <div className="sr-toolbar">
                    <p className="sr-hint">
                        {t('reports.schedules.hint', 'Raporty generowane automatycznie i wysyłane e-mailem według harmonogramu.')}
                    </p>
                    {!formOpen && (
                        <button className="report-dl-btn" onClick={openCreate} disabled={!canCreate}>
                            <Plus size={16} /> {t('reports.schedules.new', 'Nowy harmonogram')}
                        </button>
                    )}
                </div>

                {missingIdInfo && (
                    <div className="sr-muted">
                        {t('reports.schedules.missingUserId', 'Nie udało się ustalić Twojego identyfikatora użytkownika — tworzenie harmonogramu będzie możliwe po złożeniu pierwszego wniosku.')}
                    </div>
                )}
                {notice && <div className="sr-notice">{notice}</div>}
                {listError && <div className="login-error">{listError}</div>}

                {formOpen && (
                    <form className="sr-form" onSubmit={handleSubmit} noValidate>
                        <div className="sr-form-header">
                            <h4>{editingId != null
                                ? t('reports.schedules.editTitle', 'Edycja harmonogramu')
                                : t('reports.schedules.newTitle', 'Nowy harmonogram')}</h4>
                            <button type="button" className="sr-icon-btn" onClick={closeForm} title={t('reports.schedules.cancel', 'Anuluj')}>
                                <X size={16} />
                            </button>
                        </div>

                        <label className="sr-field sr-field--wide">
                            <span>{t('reports.schedules.name', 'Nazwa')}</span>
                            <input className="filter-input" maxLength={120} value={form.name}
                                onChange={e => setField('name', e.target.value)}
                                placeholder={t('reports.schedules.namePlaceholder', 'np. Raport tygodniowy obecności')} />
                        </label>

                        <label className="sr-field">
                            <span>{t('reports.schedules.reportType', 'Typ raportu')}</span>
                            <select className="filter-select" value={form.reportType} onChange={e => changeReportType(e.target.value)}>
                                <optgroup label={t('reports.schedules.individual', 'Indywidualne')}>
                                    {EMPLOYEE_TYPES.map(rt => <option key={rt} value={rt}>{L(rt)}</option>)}
                                </optgroup>
                                {!isEmployee && (
                                    <optgroup label={t('reports.schedules.team', 'Zespołowe')}>
                                        {TEAM_TYPES.map(rt => <option key={rt} value={rt}>{L(rt)}</option>)}
                                    </optgroup>
                                )}
                            </select>
                        </label>

                        <div className="sr-field">
                            <span>{t('reports.schedules.format', 'Format')}</span>
                            <div className="period-tabs">
                                {FORMATS.map(f => (
                                    <button type="button" key={f} className={`period-tab ${form.reportFormat === f ? 'active' : ''}`}
                                        onClick={() => setField('reportFormat', f)}>{f}</button>
                                ))}
                            </div>
                        </div>

                        <div className="sr-field">
                            <span>{t('reports.schedules.frequency', 'Częstotliwość')}</span>
                            <div className="period-tabs">
                                {FREQUENCIES.map(f => (
                                    <button type="button" key={f} className={`period-tab ${form.frequency === f ? 'active' : ''}`}
                                        onClick={() => setField('frequency', f)}>{L(f)}</button>
                                ))}
                            </div>
                        </div>

                        {form.frequency === 'WEEKLY' && (
                            <label className="sr-field">
                                <span>{t('reports.schedules.dayOfWeek', 'Dzień tygodnia')}</span>
                                <select className="filter-select" value={form.dayOfWeek} onChange={e => setField('dayOfWeek', Number(e.target.value))}>
                                    {DAY_NAMES.map((_, i) => <option key={i + 1} value={i + 1}>{dayName(i + 1)}</option>)}
                                </select>
                            </label>
                        )}

                        {form.frequency === 'MONTHLY' && (
                            <label className="sr-field">
                                <span>{t('reports.schedules.dayOfMonth', 'Dzień miesiąca')}</span>
                                <select className="filter-select" value={form.dayOfMonth} onChange={e => setField('dayOfMonth', Number(e.target.value))}>
                                    {Array.from({ length: 28 }, (_, i) => <option key={i + 1} value={i + 1}>{i + 1}</option>)}
                                </select>
                            </label>
                        )}

                        <label className="sr-field">
                            <span>{t('reports.schedules.executionTime', 'Godzina wysyłki')}</span>
                            <input type="time" className="filter-input" value={form.executionTime}
                                onChange={e => setField('executionTime', e.target.value)} />
                        </label>

                        <label className="sr-field">
                            <span>{t('reports.schedules.period', 'Okres raportu')}</span>
                            <select className="filter-select" value={form.reportPeriod} onChange={e => setField('reportPeriod', e.target.value)}>
                                {PERIODS.map(p => <option key={p} value={p}>{L(p)}</option>)}
                            </select>
                        </label>

                        {form.reportPeriod === 'CUSTOM' && (
                            <div className="sr-field sr-field--wide">
                                <span>{t('reports.schedules.customRange', 'Zakres dat')}</span>
                                <div className="dl-date-row">
                                    <input type="date" className="filter-input" value={form.customStartDate}
                                        max={form.customEndDate || undefined}
                                        onChange={e => setField('customStartDate', e.target.value)} />
                                    <span className="dl-label">–</span>
                                    <input type="date" className="filter-input" value={form.customEndDate}
                                        min={form.customStartDate || undefined}
                                        onChange={e => setField('customEndDate', e.target.value)} />
                                </div>
                            </div>
                        )}

                        <div className="sr-field sr-field--wide">
                            <span>
                                {isTeamType
                                    ? t('reports.schedules.targetsTeam', 'Zespoły (managerowie)')
                                    : t('reports.schedules.targets', 'Osoby objęte raportem')}
                                {!isEmployee && form.targetUserIds.length > 0 && <em className="sr-count"> · {form.targetUserIds.length}</em>}
                            </span>
                            {isEmployee ? (
                                <div className="sr-readonly">{t('reports.schedules.onlyMe', 'Tylko Ty')} ({currentUserEmail})</div>
                            ) : (
                                <div className="sr-picker">
                                    {targetCandidates.length > 6 && (
                                        <input className="filter-input sr-picker-search" value={userFilter}
                                            onChange={e => setUserFilter(e.target.value)}
                                            placeholder={t('reports.schedules.searchUser', 'Szukaj osoby…')} />
                                    )}
                                    <div className="sr-picker-list">
                                        {filteredCandidates.length === 0 && <div className="empty">{t('reports.noData', 'Brak danych.')}</div>}
                                        {filteredCandidates.map(u => (
                                            <label key={u.userID} className="sr-picker-item">
                                                <input type="checkbox" checked={form.targetUserIds.includes(u.userID)}
                                                    onChange={() => toggleTarget(u.userID)} />
                                                <span>{userLabel(u)}</span>
                                                {u.email && <span className="sr-muted">{u.email}</span>}
                                            </label>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>

                        <label className="sr-field sr-field--wide">
                            <span>{t('reports.schedules.recipients', 'Odbiorcy e-mail')}</span>
                            {isEmployee ? (
                                <div className="sr-readonly">{currentUserEmail}</div>
                            ) : (
                                <>
                                    <textarea className="filter-input sr-textarea" rows={2} value={form.recipientEmailsText}
                                        onChange={e => setField('recipientEmailsText', e.target.value)}
                                        placeholder="jan.kowalski@chrono.pl, anna.nowak@chrono.pl" />
                                    <small className="sr-muted">{t('reports.schedules.recipientsHint', 'Oddziel adresy przecinkiem, średnikiem lub nową linią.')}</small>
                                </>
                            )}
                        </label>

                        {editingId != null && (
                            <p className="sr-muted sr-field--wide">
                                {t('reports.schedules.editNote', 'Zapisanie zmian wyzeruje licznik błędów i przeliczy termin kolejnej wysyłki.')}
                            </p>
                        )}

                        {formError && <div className="login-error sr-field--wide">{formError}</div>}

                        <div className="sr-form-footer sr-field--wide">
                            <button type="button" className="sr-btn-secondary" onClick={closeForm} disabled={saving}>
                                {t('reports.schedules.cancel', 'Anuluj')}
                            </button>
                            <button type="submit" className="report-dl-btn" disabled={saving}>
                                <Save size={16} />
                                {saving ? t('reports.schedules.saving', 'Zapisywanie…') : t('reports.schedules.save', 'Zapisz')}
                            </button>
                        </div>
                    </form>
                )}

                {loading ? (
                    <div className="empty">{t('reports.loadingShort', 'Ładowanie…')}</div>
                ) : schedules.length === 0 ? (
                    <div className="sr-empty">
                        <CalendarClock size={28} />
                        <span>{t('reports.schedules.empty', 'Nie masz jeszcze żadnych harmonogramów.')}</span>
                    </div>
                ) : (
                    <div className="sr-list">
                        {schedules.map(s => {
                            const busy = busyId === s.scheduleId;
                            const showCreator = isPrivileged && s.creatorId !== currentUserId;
                            return (
                                <div key={s.scheduleId} className={`sr-item${s.isActive ? '' : ' sr-item--inactive'}`}>
                                    <div className="sr-item-main">
                                        <div className="sr-item-title">
                                            <span className="sr-name">{s.name}</span>
                                            <span className={`sr-badge ${s.isActive ? 'sr-badge--on' : 'sr-badge--off'}`}>
                                                {s.isActive ? t('reports.schedules.active', 'Aktywny') : t('reports.schedules.inactive', 'Wyłączony')}
                                            </span>
                                            <span className="sr-badge">{s.reportFormat}</span>
                                        </div>
                                        <div className="sr-item-meta">
                                            <span>{L(s.reportType)}</span>
                                            <span>{describeFrequency(s)}</span>
                                            <span>{t('reports.schedules.periodShort', 'Okres:')} {describePeriod(s)}</span>
                                        </div>
                                        <div className="sr-item-meta sr-muted">
                                            <span>{t('reports.schedules.for', 'Dotyczy:')} {describeTargets(s.targetUserIds)}</span>
                                            <span>{t('reports.schedules.to', 'Do:')} {(s.recipientEmails || []).join(', ')}</span>
                                            {showCreator && <span>{t('reports.schedules.creator', 'Utworzył(a):')} {s.creatorFullName}</span>}
                                        </div>
                                        <div className="sr-item-meta sr-muted">
                                            <span>{t('reports.schedules.nextRun', 'Następna wysyłka:')} {s.isActive ? fmtDateTime(s.nextRunAt) : '—'}</span>
                                            <span>
                                                {t('reports.schedules.lastRun', 'Ostatnia:')} {fmtDateTime(s.lastRunAt)}
                                                {s.lastRunStatus && (
                                                    <span className={`sr-status sr-status--${s.lastRunStatus.toLowerCase()}`}>
                                                        {s.lastRunStatus === 'SUCCESS'
                                                            ? t('reports.schedules.success', 'sukces')
                                                            : t('reports.schedules.failed', 'błąd')}
                                                    </span>
                                                )}
                                            </span>
                                            {s.retryCount > 0 && (
                                                <span className="sr-retry">
                                                    <AlertTriangle size={12} /> {t('reports.schedules.retries', 'Nieudane próby:')} {s.retryCount}/5
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                    <div className="sr-actions">
                                        <button className="sr-icon-btn" disabled={busy} onClick={() => handleTrigger(s)}
                                            title={t('reports.schedules.triggerNow', 'Wyślij teraz')}><Send size={16} /></button>
                                        <button className={`sr-icon-btn${s.isActive ? ' sr-icon-btn--on' : ''}`} disabled={busy} onClick={() => handleToggle(s)}
                                            title={s.isActive ? t('reports.schedules.disable', 'Wyłącz') : t('reports.schedules.enable', 'Włącz')}><Power size={16} /></button>
                                        <button className="sr-icon-btn" disabled={busy} onClick={() => openEdit(s)}
                                            title={t('reports.schedules.edit', 'Edytuj')}><Pencil size={16} /></button>
                                        <button className="sr-icon-btn sr-icon-btn--danger" disabled={busy} onClick={() => handleDelete(s)}
                                            title={t('reports.schedules.delete', 'Usuń')}><Trash2 size={16} /></button>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </Card>
    );
};

export default ScheduledReports;
