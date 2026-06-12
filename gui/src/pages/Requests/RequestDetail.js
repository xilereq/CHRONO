import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { applicationService } from '../../services';
import RequestForm from '../../components/requests/RequestForm';
import '../main.css';

const translateType = (type) => {
    switch (type) {
        case 'HOLIDAY_LEAVE': return 'Urlop wypoczynkowy';
        case 'SICK_LEAVE': return 'Zwolnienie lekarskie';
        case 'EARLY_EXIT': return 'Wyjście wcześniejsze';
        case 'OVERTIME_EARN': return 'Zgłoszenie nadgodzin';
        case 'OVERTIME_TAKE': return 'Odbiór nadgodzin';
        default: return type || 'Inny';
    }
};

const translateStatus = (status) => {
    switch (status) {
        case 'APPROVED': return 'Zatwierdzony';
        case 'PENDING': return 'Oczekujący';
        case 'REJECTED': return 'Odrzucony';
        case 'CANCELLED': return 'Anulowany';
        default: return status;
    }
};

const RequestDetail = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [request, setRequest] = useState(null);
    const [loading, setLoading] = useState(true);
    const [showEditForm, setShowEditForm] = useState(false);
    const [actionError, setActionError] = useState('');

    useEffect(() => {
        const fetchRequestDetails = async () => {
            try {
                const data = await applicationService.getUserApplications();
                const foundRequest = data.find(r => r.id.toString() === id);
                setRequest(foundRequest || null);
            } catch (error) {
                console.error("Błąd podczas pobierania szczegółów wniosku:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchRequestDetails();
    }, [id]);

    const handleCancel = async () => {
        if (!window.confirm('Czy na pewno chcesz anulować ten wniosek?')) return;
        setActionError('');
        try {
            await applicationService.cancelApplication(request.id);
            navigate('/requests');
        } catch (err) {
            setActionError('Nie udało się anulować wniosku: ' + err.message);
        }
    };

    if (loading) {
        return <div className="page-loading">Ładowanie szczegółów wniosku...</div>;
    }

    if (!request) {
        return (
            <div className="page-empty">
                Wniosek nie został znaleziony. <Link to="/requests" className="accent-link">Wróć do listy</Link>
            </div>
        );
    }

    if (showEditForm) {
        return (
            <div className="dashboard">
                <h2 className="dashboard-title">Edycja wniosku #{request.id}</h2>
                <p style={{ marginBottom: '1rem', color: 'var(--text-secondary)' }}>
                    Złożenie zmienionego wniosku anuluje poprzedni i wysyła nowy do kierownika.
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
            <h2 className="dashboard-title">Podgląd wniosku #{request.id}</h2>

            {actionError && (
                <div className="login-error" style={{ marginBottom: '1rem' }}>{actionError}</div>
            )}

            <div className="card request-card">
                <div className="request-details">
                    <div className="request-row">
                        <strong className="request-label">Typ wniosku:</strong>
                        <span className="request-value">{translateType(request.type)}</span>
                    </div>

                    <div className="request-row">
                        <strong className="request-label">Status:</strong>
                        <span className="request-value">{translateStatus(request.status)}</span>
                    </div>

                    <div className="request-row">
                        <strong className="request-label">Data złożenia:</strong>
                        <span>{request.creationDateTime ? new Date(request.creationDateTime).toLocaleDateString('pl-PL') : '-'}</span>
                    </div>

                    <div className="request-row">
                        <strong className="request-label">Okres nieobecności:</strong>
                        <span>{request.startDate}  —  {request.endDate}</span>
                    </div>

                    <div className="request-section">
                        <strong className="request-label">Powód (uzasadnienie pracownika):</strong>
                        <div className="request-reason-box">
                            {request.reason || <span className="placeholder-muted">Brak wpisanego uzasadnienia.</span>}
                        </div>
                    </div>

                    {request.status === 'REJECTED' && request.decisionReason && (
                        <div className="request-section">
                            <strong className="reject-label">Powód odrzucenia (uwagi przełożonego):</strong>
                            <div className="reject-box">{request.decisionReason}</div>
                        </div>
                    )}
                </div>

                <div className="request-actions" style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem', flexWrap: 'wrap' }}>
                    <Link to="/requests" className="back-link">← Wróć do listy wniosków</Link>
                    {canModify && (
                        <button className="action-btn" onClick={() => setShowEditForm(true)}>
                            Edytuj wniosek
                        </button>
                    )}
                    {canModify && (
                        <button className="action-btn btn-cancel-custom" onClick={handleCancel}>
                            Anuluj wniosek
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default RequestDetail;
