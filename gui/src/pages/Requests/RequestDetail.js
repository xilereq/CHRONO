import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { applicationService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import RequestForm from '../../components/requests/RequestForm';
import '../main.css';

const RequestDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const { t } = useLanguage();
    const [request, setRequest] = useState(null);
    const [loading, setLoading] = useState(true);
    const [showEditForm, setShowEditForm] = useState(false);
    const [actionError, setActionError] = useState('');

    const typeLabels = {
        HOLIDAY_LEAVE: t('requests.types.holiday', 'Urlop wypoczynkowy'),
        SICK_LEAVE:    t('requests.types.sick', 'Zwolnienie lekarskie'),
        EARLY_EXIT:    t('requests.types.earlyExit', 'Wyjście wcześniejsze'),
        OVERTIME_EARN: t('requests.types.overtimeEarn', 'Zgłoszenie nadgodzin'),
        OVERTIME_TAKE: t('requests.types.overtimeTake', 'Odbiór nadgodzin'),
        HOME_OFFICE:   t('reports.homeOffice', 'Praca zdalna (HO)'),
    };

    const statusLabels = {
        APPROVED:  t('requests.approved', 'Zatwierdzony'),
        PENDING:   t('requests.pending', 'Oczekujący'),
        REJECTED:  t('requests.rejected', 'Odrzucony'),
        CANCELLED: t('requests.cancelled', 'Anulowany'),
    };

    useEffect(() => {
        const fetchRequestDetails = async () => {
            try {
                const data = await applicationService.getUserApplications();
                const foundRequest = data.find(r => r.id.toString() === id);
                setRequest(foundRequest || null);
            } catch (error) {
                console.error(t('requests.fetchError', 'Błąd podczas pobierania szczegółów wniosku:'), error);
            } finally {
                setLoading(false);
            }
        };

        fetchRequestDetails();
    }, [id]);

    const handleCancel = async () => {
        if (!window.confirm(t('requests.cancelConfirm', 'Czy na pewno chcesz anulować ten wniosek?'))) return;
        setActionError('');
        try {
            await applicationService.cancelApplication(request.id);
            navigate('/requests');
        } catch (err) {
            setActionError(t('requests.cancelError', 'Nie udało się anulować wniosku: ') + err.message);
        }
    };

    if (loading) {
        return <div className="page-loading">{t('requests.loadingDetails', 'Ładowanie szczegółów wniosku...')}</div>;
    }

    if (!request) {
        return (
            <div className="page-empty">
                {t('requests.notFound', 'Wniosek nie został znaleziony.')}{' '}
                <Link to="/requests" className="accent-link">{t('requests.backToList', 'Wróć do listy wniosków')}</Link>
            </div>
        );
    }

    if (showEditForm) {
        return (
            <div className="dashboard">
                <h2 className="dashboard-title">{t('requests.editTitle', 'Edycja wniosku #')}{request.id}</h2>
                <p style={{ marginBottom: '1rem', color: 'var(--text-secondary)' }}>
                    {t('requests.editWarning', 'Złożenie zmienionego wniosku anuluje poprzedni i wysyła nowy do kierownika.')}
                </p>
                <RequestForm
                    initialData={request}
                    oldApplicationId={request.id}
                    onCancel={() => setShowEditForm(false)}
                    onSuccess={() => navigate('/requests')}
                />
            </div>
        );
    }

    const canModify = request.status === 'PENDING';

    return (
        <div className="dashboard">
            <h2 className="dashboard-title">{t('requests.viewTitle', 'Podgląd wniosku #')}{request.id}</h2>

            {actionError && (
                <div className="login-error" style={{ marginBottom: '1rem' }}>{actionError}</div>
            )}

            <div className="card request-card">
                <div className="request-details">
                    <div className="request-row">
                        <strong className="request-label">{t('requests.typeLabel', 'Typ wniosku:')}</strong>
                        <span className="request-value">{typeLabels[request.type] || request.type || '-'}</span>
                    </div>

                    <div className="request-row">
                        <strong className="request-label">{t('requests.statusLabel', 'Status:')}</strong>
                        <span className="request-value">{statusLabels[request.status] || request.status}</span>
                    </div>

                    <div className="request-row">
                        <strong className="request-label">{t('requests.submitDateLabel', 'Data złożenia:')}</strong>
                        <span>{request.creationDateTime ? new Date(request.creationDateTime).toLocaleDateString('pl-PL') : '-'}</span>
                    </div>

                    <div className="request-row">
                        <strong className="request-label">{t('requests.absencePeriod', 'Okres nieobecności:')}</strong>
                        <span>{request.startDate}  —  {request.endDate}</span>
                    </div>

                    <div className="request-section">
                        <strong className="request-label">{t('requests.reasonEmployee', 'Powód (uzasadnienie pracownika):')}</strong>
                        <div className="request-reason-box">
                            {request.reason || <span className="placeholder-muted">{t('requests.noReason', 'Brak wpisanego uzasadnienia.')}</span>}
                        </div>
                    </div>

                    {request.status === 'REJECTED' && request.decisionReason && (
                        <div className="request-section">
                            <strong className="reject-label">{t('requests.rejectReason', 'Powód odrzucenia (uwagi przełożonego):')}</strong>
                            <div className="reject-box">{request.decisionReason}</div>
                        </div>
                    )}
                </div>

                <div className="request-actions" style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem', flexWrap: 'wrap' }}>
                    <Link to="/requests" className="back-link">← {t('requests.backToList', 'Wróć do listy wniosków')}</Link>
                    {canModify && (
                        <button className="action-btn" onClick={() => setShowEditForm(true)}>
                            {t('requests.editBtn', 'Edytuj wniosek')}
                        </button>
                    )}
                    {canModify && (
                        <button className="action-btn btn-cancel-custom" onClick={handleCancel}>
                            {t('requests.cancelRequest', 'Anuluj wniosek')}
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default RequestDetail;
