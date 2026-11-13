import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { UserCheck } from 'lucide-react';
import { userService, delegationService } from '../../services';
import { Button } from '../../components';
import { useLanguage } from '../../context/LanguageContext';
import '../requests/requests.css';
import './manager.css';

const todayIso = () => new Date().toLocaleDateString('sv-SE');
const OPEN_STATUSES = ['ACTIVE', 'SCHEDULED'];
const EMPTY_FORM = { managerId: '', deputyId: '', startDate: '', endDate: '', reason: '' };

const userLabel = (u) => `${u.name || ''} ${u.surname || ''}`.trim() + (u.email ? ` (${u.email})` : '');

const DeputyTab = ({ isAdmin = false }) => {
    const { t } = useLanguage();
    const [candidates, setCandidates] = useState([]);
    const [managers, setManagers] = useState([]);
    const [delegations, setDelegations] = useState([]);
    const [formData, setFormData] = useState(EMPTY_FORM);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [confirmRevokeId, setConfirmRevokeId] = useState(null);
    const [revokingId, setRevokingId] = useState(null);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [listError, setListError] = useState('');

    const today = todayIso();

    const errorMessage = useCallback((err, fallbackKey, fallback) => {
        const byCode = {
            DELEGATION_OVERLAPPING: t('manager.deputyOverlapError', 'W tym okresie jest już przypisane zastępstwo.'),
            DELEGATION_INVALID_PERIOD: t('manager.deputyPeriodError', 'Nieprawidłowy okres zastępstwa (data w przeszłości lub koniec przed początkiem).'),
            DELEGATION_INVALID_DEPUTY: t('manager.deputyInvalidError', 'Wybrana osoba nie może zastępować (to ten sam menedżer albo nie jest zatrudniona przez cały okres).'),
            DELEGATION_NOT_A_MANAGER: t('manager.deputyNotManagerError', 'Wybrany użytkownik nie jest menedżerem.'),
            DELEGATION_FORBIDDEN: t('manager.deputyForbiddenError', 'Brak uprawnień do tej operacji.'),
            DELEGATION_ALREADY_FINISHED: t('manager.deputyFinishedError', 'To zastępstwo zostało już cofnięte lub wygasło.'),
            DELEGATION_NOT_FOUND: t('manager.deputyNotFoundError', 'Nie znaleziono zastępstwa.'),
        };
        return byCode[err?.code] || t(fallbackKey, fallback);
    }, [t]);

    const loadDelegations = useCallback(async () => {
        try {
            const data = isAdmin ? await delegationService.getAll() : await delegationService.getGiven();
            setDelegations([...(data || [])].sort((a, b) => b.startDate.localeCompare(a.startDate)));
            setListError('');
        } catch (err) {
            setDelegations([]);
            setListError(errorMessage(err, 'manager.deputyLoadError', 'Nie udało się pobrać listy zastępstw.'));
        }
    }, [isAdmin, errorMessage]);

    useEffect(() => {
        const init = async () => {
            try {
                if (isAdmin) {
                    const [allUsers, allManagers] = await Promise.all([userService.getAllUsers(), userService.getManagers()]);
                    setCandidates(allUsers || []);
                    setManagers(allManagers || []);
                } else {
                    setCandidates((await userService.getSubordinates()) || []);
                }
            } catch (err) {
                console.error(t('manager.fetchSubordinatesError', 'Błąd podczas pobierania podwładnych:'), err);
            }
            await loadDelegations();
            setLoading(false);
        };
        init();
    }, [isAdmin, t, loadDelegations]);

    const deputyOptions = useMemo(() => {
        if (!isAdmin) return candidates;
        const managerId = Number(formData.managerId);
        return candidates.filter(c => c.userID !== managerId);
    }, [candidates, formData.managerId, isAdmin]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => {
            const next = { ...prev, [name]: value };
            if (name === 'startDate' && next.endDate && value > next.endDate) next.endDate = '';
            if (name === 'managerId' && String(prev.deputyId) === value) next.deputyId = '';
            return next;
        });
    };

    const validate = () => {
        const { managerId, deputyId, startDate, endDate } = formData;
        if (isAdmin && !managerId) return t('manager.deputyManagerSelectError', 'Wybierz menedżera.');
        if (!deputyId) return t('manager.deputySelectError', 'Wybierz osobę zastępującą.');
        if (!startDate || !endDate) return t('manager.deputyDatesError', 'Podaj daty rozpoczęcia i zakończenia zastępstwa.');
        if (endDate < startDate) return t('manager.dateOrderError', 'Data zakończenia nie może być wcześniejsza niż data rozpoczęcia.');
        if (startDate < today) return t('manager.deputyPastError', 'Zastępstwo nie może zaczynać się w przeszłości.');
        const overlaps = delegations.some(d =>
            OPEN_STATUSES.includes(d.status)
            && (!isAdmin || d.managerId === Number(managerId))
            && !(endDate < d.startDate || startDate > d.endDate));
        if (overlaps) return t('manager.deputyOverlapError', 'W tym okresie jest już przypisane zastępstwo.');
        return '';
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setMessage('');

        const validationError = validate();
        if (validationError) {
            setError(validationError);
            return;
        }

        setSaving(true);
        try {
            await delegationService.create({ ...formData, managerId: isAdmin ? formData.managerId : null });
            setMessage(t('manager.deputySuccess', 'Zastępstwo zostało przypisane.'));
            setFormData(EMPTY_FORM);
            await loadDelegations();
        } catch (err) {
            setError(errorMessage(err, 'manager.deputySaveError', 'Nie udało się przypisać zastępstwa.'));
        } finally {
            setSaving(false);
        }
    };

    const handleRevoke = async (id) => {
        setError('');
        setMessage('');
        setRevokingId(id);
        try {
            await delegationService.revoke(id);
            setMessage(t('manager.deputyRemoved', 'Zastępstwo zostało cofnięte.'));
            await loadDelegations();
        } catch (err) {
            setError(errorMessage(err, 'manager.deputyRemoveError', 'Nie udało się cofnąć zastępstwa.'));
        } finally {
            setRevokingId(null);
            setConfirmRevokeId(null);
        }
    };

    const statusLabels = {
        ACTIVE: t('manager.deputyStatusActive', 'Trwa'),
        SCHEDULED: t('manager.deputyStatusPlanned', 'Zaplanowane'),
        EXPIRED: t('manager.deputyStatusEnded', 'Zakończone'),
        REVOKED: t('manager.deputyStatusRevoked', 'Cofnięte'),
    };

    return (
        <div className="policy-section">
            <h2 className="section-title">{t('manager.deputyTitle', 'Zastępstwo na czas urlopu')}</h2>
            <p className="section-subtitle mb-3">
                {isAdmin
                    ? t('manager.deputySubtitleAdmin', 'Wskaż menedżera i osobę, która na wybrany okres tymczasowo przejmie jego uprawnienia do rozpatrywania wniosków zespołu.')
                    : t('manager.deputySubtitle', 'Wskaż osobę, która na wybrany okres tymczasowo przejmie Twoje uprawnienia do rozpatrywania wniosków zespołu.')}
            </p>

            {error && <div className="manager-alert-error" role="alert">{error}</div>}
            {message && <div className="manager-alert-success" role="status"><strong>{message}</strong></div>}

            <form onSubmit={handleSubmit} className="request-form" style={{ marginTop: 'var(--space-5)' }}>
                <div className="request-form-grid">
                    {isAdmin && (
                        <label className="field field-full-width">
                            <span className="field-label">{t('manager.deputyManagerLabel', 'Menedżer *')}</span>
                            <select
                                className="field-input"
                                name="managerId"
                                value={formData.managerId}
                                onChange={handleChange}
                                disabled={saving}
                                required
                            >
                                <option value="">{t('manager.deputyManagerPlaceholder', '-- Wybierz menedżera --')}</option>
                                {managers.map(m => (
                                    <option key={m.userID} value={m.userID}>{userLabel(m)}</option>
                                ))}
                            </select>
                        </label>
                    )}

                    <label className="field field-full-width">
                        <span className="field-label">{t('manager.deputyPersonLabel', 'Osoba zastępująca *')}</span>
                        <select
                            className="field-input"
                            name="deputyId"
                            value={formData.deputyId}
                            onChange={handleChange}
                            disabled={saving}
                            required
                        >
                            <option value="">{t('manager.deputySelectPlaceholder', '-- Wybierz osobę --')}</option>
                            {deputyOptions.map(c => (
                                <option key={c.userID} value={c.userID}>{userLabel(c)}</option>
                            ))}
                        </select>
                        {!loading && deputyOptions.length === 0 && (
                            <span className="deputy-hint">
                                {t('manager.deputyNoCandidates', 'Brak osób w zespole, które mogą Cię zastąpić.')}
                            </span>
                        )}
                    </label>

                    <div className="field-row" style={{ gridColumn: 'span 2' }}>
                        <label className="field field-flex">
                            <span className="field-label">{t('manager.deputyDateFrom', 'Zastępstwo od')}</span>
                            <input
                                className="field-input"
                                type="date"
                                name="startDate"
                                value={formData.startDate}
                                min={today}
                                onChange={handleChange}
                                disabled={saving}
                                required
                            />
                        </label>
                        <label className="field field-flex">
                            <span className="field-label">{t('manager.deputyDateTo', 'Zastępstwo do')}</span>
                            <input
                                className="field-input"
                                type="date"
                                name="endDate"
                                value={formData.endDate}
                                min={formData.startDate || today}
                                onChange={handleChange}
                                disabled={saving}
                                required
                            />
                        </label>
                    </div>

                    <label className="field field-full-width">
                        <span className="field-label">{t('manager.deputyReasonLabel', 'Powód (opcjonalnie)')}</span>
                        <input
                            className="field-input"
                            name="reason"
                            value={formData.reason}
                            onChange={handleChange}
                            maxLength={255}
                            placeholder={t('manager.deputyReasonPlaceholder', 'np. urlop wypoczynkowy')}
                            disabled={saving}
                        />
                    </label>
                </div>

                <div className="policies-footer">
                    <div style={{ flex: 1 }}></div>
                    <div className="footer-actions">
                        <Button type="submit" variant="primary" disabled={saving || loading} style={{ padding: '0.6rem 1.5rem', borderRadius: 'var(--radius-md)' }}>
                            {saving ? t('common.processing', 'Przetwarzanie...') : (
                                <>
                                    <UserCheck size={18} />
                                    <span>{t('manager.deputySaveBtn', 'Przypisz zastępstwo')}</span>
                                </>
                            )}
                        </Button>
                    </div>
                </div>
            </form>

            <h3 className="deputy-list-title">{t('manager.deputyListTitle', 'Przypisane zastępstwa')}</h3>

            {loading ? (
                <div className="manager-loading-state">{t('common.loading_data', 'Ładowanie danych...')}</div>
            ) : listError ? (
                <div className="manager-empty-state">
                    <p className="manager-empty-text">{listError}</p>
                </div>
            ) : delegations.length === 0 ? (
                <div className="manager-empty-state">
                    <UserCheck size={40} className="manager-empty-icon" />
                    <p className="manager-empty-text">{t('manager.deputyListEmpty', 'Nie masz przypisanych zastępstw.')}</p>
                </div>
            ) : (
                <div className="manager-request-list">
                    {delegations.map(d => {
                        const status = d.status;
                        const canRevoke = OPEN_STATUSES.includes(status);
                        const isConfirming = confirmRevokeId === d.id;
                        return (
                            <div key={d.id} className={`manager-request-card deputy-card deputy-card--${status.toLowerCase()}`}>
                                <div>
                                    <h4 className="manager-request-title">
                                        {isAdmin
                                            ? `${d.managerName} → ${d.deputyName}`
                                            : d.deputyName}
                                    </h4>
                                    <div className="manager-request-details">
                                        <span>{d.startDate} – {d.endDate}</span>
                                        {d.reason && <span>{d.reason}</span>}
                                    </div>
                                </div>
                                <div className="manager-actions">
                                    <span className={`deputy-badge deputy-badge--${status.toLowerCase()}`}>
                                        {statusLabels[status] || status}
                                    </span>
                                    {canRevoke && (
                                        isConfirming ? (
                                            <>
                                                <button
                                                    type="button"
                                                    className="btn-reject deputy-remove-confirm"
                                                    onClick={() => handleRevoke(d.id)}
                                                    disabled={revokingId === d.id}
                                                >
                                                    {t('manager.deputyRemoveConfirm', 'Potwierdź cofnięcie')}
                                                </button>
                                                <button
                                                    type="button"
                                                    className="action-panel-btn-cancel"
                                                    onClick={() => setConfirmRevokeId(null)}
                                                    disabled={revokingId === d.id}
                                                >
                                                    {t('common.cancel', 'Anuluj')}
                                                </button>
                                            </>
                                        ) : (
                                            <button
                                                type="button"
                                                className="btn-reject"
                                                onClick={() => setConfirmRevokeId(d.id)}
                                            >
                                                {t('manager.deputyRemove', 'Cofnij')}
                                            </button>
                                        )
                                    )}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
};

export default DeputyTab;
