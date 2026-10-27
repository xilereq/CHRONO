import React, { useState, useEffect, useCallback } from 'react';
import { UserCheck } from 'lucide-react';
import { userService } from '../../services';
import { Button } from '../../components';
import { useLanguage } from '../../context/LanguageContext';
import '../requests/requests.css';
import './manager.css';

const todayIso = () => new Date().toLocaleDateString('sv-SE');

const getStatus = (deputy, today) => {
    if (deputy.endDate < today) return 'ended';
    if (deputy.startDate > today) return 'planned';
    return 'active';
};

const DeputyTab = () => {
    const { t } = useLanguage();
    const [candidates, setCandidates] = useState([]);
    const [deputies, setDeputies] = useState([]);
    const [formData, setFormData] = useState({ deputyEmail: '', startDate: '', endDate: '' });
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [confirmRemoveId, setConfirmRemoveId] = useState(null);
    const [removingId, setRemovingId] = useState(null);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [listNotice, setListNotice] = useState('');

    const today = todayIso();

    const loadDeputies = useCallback(async () => {
        try {
            const data = await userService.getMyDeputies();
            setDeputies([...(data || [])].sort((a, b) => a.startDate.localeCompare(b.startDate)));
            setListNotice('');
        } catch (err) {
            setDeputies([]);
            setListNotice(err.notAvailable
                ? t('manager.deputyUnavailable', 'Obsługa zastępstw nie jest jeszcze dostępna na serwerze.')
                : t('manager.deputyLoadError', 'Nie udało się pobrać listy zastępstw.'));
        }
    }, [t]);

    useEffect(() => {
        const init = async () => {
            try {
                const { role } = await userService.getMyRole();
                const data = role === 'ROLE_ADMIN'
                    ? await userService.getAllUsers()
                    : await userService.getSubordinates();
                setCandidates(data || []);
            } catch (err) {
                console.error(t('manager.fetchSubordinatesError', 'Błąd podczas pobierania podwładnych:'), err);
            }
            await loadDeputies();
            setLoading(false);
        };
        init();
    }, [t, loadDeputies]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => {
            const next = { ...prev, [name]: value };
            if (name === 'startDate' && next.endDate && value > next.endDate) next.endDate = '';
            return next;
        });
    };

    const validate = () => {
        const { deputyEmail, startDate, endDate } = formData;
        if (!deputyEmail) return t('manager.deputySelectError', 'Wybierz osobę zastępującą.');
        if (!startDate || !endDate) return t('manager.deputyDatesError', 'Podaj daty rozpoczęcia i zakończenia zastępstwa.');
        if (endDate < startDate) return t('manager.dateOrderError', 'Data zakończenia nie może być wcześniejsza niż data rozpoczęcia.');
        if (startDate < today) return t('manager.deputyPastError', 'Zastępstwo nie może zaczynać się w przeszłości.');
        const overlaps = deputies.some(d => getStatus(d, today) !== 'ended' && !(endDate < d.startDate || startDate > d.endDate));
        if (overlaps) return t('manager.deputyOverlapError', 'W tym okresie masz już przypisane zastępstwo.');
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
            await userService.assignDeputy(formData);
            setMessage(t('manager.deputySuccess', 'Zastępstwo zostało przypisane.'));
            setFormData({ deputyEmail: '', startDate: '', endDate: '' });
            await loadDeputies();
        } catch (err) {
            setError(err.notAvailable
                ? t('manager.deputyUnavailable', 'Obsługa zastępstw nie jest jeszcze dostępna na serwerze.')
                : (err.message || t('manager.deputySaveError', 'Nie udało się przypisać zastępstwa.')));
        } finally {
            setSaving(false);
        }
    };

    const handleRemove = async (id) => {
        setError('');
        setMessage('');
        setRemovingId(id);
        try {
            await userService.removeDeputy(id);
            setMessage(t('manager.deputyRemoved', 'Zastępstwo zostało cofnięte.'));
            await loadDeputies();
        } catch (err) {
            setError(err.message || t('manager.deputyRemoveError', 'Nie udało się cofnąć zastępstwa.'));
        } finally {
            setRemovingId(null);
            setConfirmRemoveId(null);
        }
    };

    const statusLabels = {
        active: t('manager.deputyStatusActive', 'Trwa'),
        planned: t('manager.deputyStatusPlanned', 'Zaplanowane'),
        ended: t('manager.deputyStatusEnded', 'Zakończone'),
    };

    return (
        <div className="policy-section">
            <h2 className="section-title">{t('manager.deputyTitle', 'Zastępstwo na czas urlopu')}</h2>
            <p className="section-subtitle mb-3">
                {t('manager.deputySubtitle', 'Wskaż osobę, która na wybrany okres tymczasowo przejmie Twoje uprawnienia do rozpatrywania wniosków zespołu.')}
            </p>

            {error && <div className="manager-alert-error" role="alert">{error}</div>}
            {message && <div className="manager-alert-success" role="status"><strong>{message}</strong></div>}

            <form onSubmit={handleSubmit} className="request-form" style={{ marginTop: 'var(--space-5)' }}>
                <div className="request-form-grid">
                    <label className="field field-full-width">
                        <span className="field-label">{t('manager.deputyPersonLabel', 'Osoba zastępująca *')}</span>
                        <select
                            className="field-input"
                            name="deputyEmail"
                            value={formData.deputyEmail}
                            onChange={handleChange}
                            disabled={saving}
                            required
                        >
                            <option value="">{t('manager.deputySelectPlaceholder', '-- Wybierz osobę --')}</option>
                            {candidates.map(c => (
                                <option key={c.userID ?? c.email} value={c.email}>
                                    {c.name} {c.surname} ({c.email})
                                </option>
                            ))}
                        </select>
                        {!loading && candidates.length === 0 && (
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
            ) : listNotice ? (
                <div className="manager-empty-state">
                    <p className="manager-empty-text">{listNotice}</p>
                </div>
            ) : deputies.length === 0 ? (
                <div className="manager-empty-state">
                    <UserCheck size={40} className="manager-empty-icon" />
                    <p className="manager-empty-text">{t('manager.deputyListEmpty', 'Nie masz przypisanych zastępstw.')}</p>
                </div>
            ) : (
                <div className="manager-request-list">
                    {deputies.map(d => {
                        const status = getStatus(d, today);
                        const isConfirming = confirmRemoveId === d.id;
                        return (
                            <div key={d.id} className={`manager-request-card deputy-card deputy-card--${status}`}>
                                <div>
                                    <h4 className="manager-request-title">
                                        {[d.deputyName, d.deputySurname].filter(Boolean).join(' ') || d.deputyEmail}
                                    </h4>
                                    <div className="manager-request-details">
                                        <span>{d.deputyEmail}</span>
                                        <span>{d.startDate} – {d.endDate}</span>
                                    </div>
                                </div>
                                <div className="manager-actions">
                                    <span className={`deputy-badge deputy-badge--${status}`}>{statusLabels[status]}</span>
                                    {status !== 'ended' && (
                                        isConfirming ? (
                                            <>
                                                <button
                                                    type="button"
                                                    className="btn-reject deputy-remove-confirm"
                                                    onClick={() => handleRemove(d.id)}
                                                    disabled={removingId === d.id}
                                                >
                                                    {t('manager.deputyRemoveConfirm', 'Potwierdź cofnięcie')}
                                                </button>
                                                <button
                                                    type="button"
                                                    className="action-panel-btn-cancel"
                                                    onClick={() => setConfirmRemoveId(null)}
                                                    disabled={removingId === d.id}
                                                >
                                                    {t('common.cancel', 'Anuluj')}
                                                </button>
                                            </>
                                        ) : (
                                            <button
                                                type="button"
                                                className="btn-reject"
                                                onClick={() => setConfirmRemoveId(d.id)}
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
