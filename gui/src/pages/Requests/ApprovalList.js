import React, { useState, useEffect } from 'react';
import { applicationService } from '../../services/applicationService';

const ApprovalList = () => {
    const [pendingRequests, setPendingRequests] = useState([]);
    const [loading, setLoading] = useState(true);

    const reviewerId = 2;

    const fetchPendingRequests = async () => {
        setLoading(true);
        try {
            const data = await applicationService.getUserApplications(1);
            const pending = data.filter(req => req.status === 'PENDING');
            setPendingRequests(pending);
        } catch (error) {
            console.error("Błąd podczas ładowania wniosków do zatwierdzenia:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchPendingRequests();
    }, []);

    const handleAction = async (id, newStatus) => {
        try {
            await applicationService.changeStatus(id, newStatus, reviewerId);
            fetchPendingRequests();
        } catch (error) {
            alert("Nie udało się zmienić statusu wniosku.");
        }
    };

    return (
        <div className="section-panel">
            <div className="section-title">Wnioski oczekujące na Twoją decyzję</div>

            {loading ? (
                <div className="table-empty-cell">Ładowanie wniosków...</div>
            ) : pendingRequests.length === 0 ? (
                <div className="table-empty-cell">Super! Nie masz żadnych wniosków do zatwierdzenia.</div>
            ) : (
                <div className="requests-table-container">
                    <table className="requests-table">
                        <thead>
                        <tr>
                            <th>Typ Wniosku</th>
                            <th>Okres / Data</th>
                            <th>Liczba Dni</th>
                            <th>Powód</th>
                            <th>Akcje</th>
                        </tr>
                        </thead>
                        <tbody>
                        {pendingRequests.map(r => (
                            <tr key={r.id}>
                                <td className="request-card-title-cell">{r.type || r.title}</td>
                                <td>{r.startDate} - {r.endDate}</td>
                                <td>{r.daysCount || '-'}</td>
                                <td>{r.reason || '-'}</td>
                                <td>
                                    <div className="submit-actions-row">
                                        <button
                                            className="action-btn action-btn-approve"
                                            onClick={() => handleAction(r.id, 'APPROVED')}
                                            title="Zatwierdź"
                                        >
                                            Zatwierdź
                                        </button>
                                        <button
                                            className="action-btn action-btn-reject"
                                            onClick={() => handleAction(r.id, 'REJECTED')}
                                            title="Odrzuć"
                                        >
                                            Odrzuć
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default ApprovalList;
