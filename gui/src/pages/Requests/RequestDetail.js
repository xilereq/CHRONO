import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { applicationService } from '../../services';
import '../main.css';

const translateType = (type) => {
    switch (type) {
        case 'HOLIDAY_LEAVE': return 'Urlop wypoczynkowy';
        case 'SICK_LEAVE': return 'Zwolnienie lekarskie';
        case 'OVERTIME': return 'Odbiór nadgodzin';
        default: return type || 'Inny';
    }
};

const translateStatus = (status) => {
    switch (status) {
        case 'APPROVED': return 'Zatwierdzony';
        case 'PENDING': return 'Oczekujący';
        case 'REJECTED': return 'Odrzucony';
        default: return status;
    }
};

const RequestDetail = () => {
    const { id } = useParams();
    const [request, setRequest] = useState(null);
    const [loading, setLoading] = useState(true);


    useEffect(() => {
        const fetchRequestDetails = async () => {
            try {
                const data = await applicationService.getUserApplications(userId);
                const foundRequest = data.find(r => r.id.toString() === id);
                setRequest(foundRequest);
            } catch (error) {
                console.error("Błąd podczas pobierania szczegółów wniosku:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchRequestDetails();
    }, [id, userId]);

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

    return (
        <div className="dashboard">
            <h2 className="dashboard-title">Podgląd wniosku #{request.id}</h2>

            <div className="card request-card">
                <div className="request-details">

                    <div className="request-row">
                        <strong className="request-label">Typ wniosku:</strong>
                        <span className="request-value">{translateType(request.type) || request.title}</span>
                    </div>

                    <div className="request-row">
                        <strong className="request-label">Status:</strong>
                        <span className="request-value">
                            {translateStatus(request.status)}
                        </span>
                    </div>

                    <div className="request-row">
                        <strong className="request-label">Data złożenia:</strong>
                        <span>{request.createdAt || '-'}</span>
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

                    {/* Jeśli wniosek został odrzucony, wyświetlamy też powód odrzucenia (jeśli API go zwraca) */}
                    {request.status === 'REJECTED' && request.rejectReason && (
                        <div className="request-section">
                            <strong className="reject-label">Powód odrzucenia (uwagi przełożonego):</strong>
                            <div className="reject-box">
                                {request.rejectReason}
                            </div>
                        </div>
                    )}
                </div>

                <div className="request-actions">
                    <Link to="/requests" className="back-link">
                        ← Wróć do listy wniosków
                    </Link>
                </div>
            </div>
        </div>
    );
}

export default RequestDetail;
