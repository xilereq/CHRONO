import React, { useState, useEffect } from 'react';
import { applicationService } from '../../services';
import './ApprovalList.css';

const ApprovalList = () => {
    const [pendingRequests, setPendingRequests] = useState([]);
    const [loading, setLoading] = useState(true);

    const [activeAction, setActiveAction] = useState({ id: null, type: null });
    const [actionReason, setActionReason] = useState('');
    const [processingId, setProcessingId] = useState(null);

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

    const initiateAction = (id, type) => {
        setActiveAction({ id, type });
        setActionReason('');
    };

    const submitAction = async () => {
        // Usunięto wymóg podawania powodu (walidację)
        setProcessingId(activeAction.id);
        try {
            await applicationService.changeStatus(activeAction.id, activeAction.type, reviewerId, actionReason);
            setActiveAction({ id: null, type: null });
            setActionReason('');
            fetchPendingRequests();
        } catch (error) {
            alert("Nie udało się zmienić statusu wniosku.");
        } finally {
            setProcessingId(null);
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
                            <React.Fragment key={r.id}>
                                <tr>
                                    <td className="request-card-title-cell">{r.type || r.title}</td>
                                    <td>{r.startDate} - {r.endDate}</td>
                                    <td>{r.daysCount || '-'}</td>
                                    <td>{r.reason || '-'}</td>
                                    <td>
                                        <div className="submit-actions-row">
                                            <button
                                                className="action-btn action-btn-approve"
                                                onClick={() => initiateAction(r.id, 'APPROVED')}
                                                title="Zatwierdź"
                                            >
                                                Zatwierdź
                                            </button>
                                            <button
                                                className="action-btn action-btn-reject"
                                                onClick={() => initiateAction(r.id, 'REJECTED')}
                                                title="Odrzuć"
                                            >
                                                Odrzuć
                                            </button>
                                        </div>
                                    </td>
                                </tr>

                                {activeAction.id === r.id && (
                                    <tr>
                                        <td colSpan="5" className={`action-panel-td ${activeAction.type === 'APPROVED' ? 'approved' : 'rejected'}`}>
                                            <div className="action-panel-container">
                                                <span className={`action-panel-label ${activeAction.type === 'APPROVED' ? 'approved' : 'rejected'}`}>
                                                    {activeAction.type === 'APPROVED' ? 'Uwagi do akceptacji (opcjonalne):' : 'Powód odrzucenia (opcjonalne):'}
                                                </span>
                                                <input
                                                    type="text"
                                                    className="field-input action-panel-input"
                                                    value={actionReason}
                                                    onChange={(e) => setActionReason(e.target.value)}
                                                    placeholder="Wpisz tutaj..."
                                                    autoFocus
                                                />
                                                <button
                                                    className={`action-panel-btn-confirm ${activeAction.type === 'APPROVED' ? 'approved' : 'rejected'}`}
                                                    onClick={submitAction}
                                                    disabled={processingId === r.id}
                                                >
                                                    {processingId === r.id ? 'Zapisywanie...' : 'Potwierdź'}
                                                </button>
                                                <button
                                                    className="action-panel-btn-cancel"
                                                    onClick={() => setActiveAction({ id: null, type: null })}
                                                    disabled={processingId === r.id}
                                                >
                                                    Anuluj
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                )}
                            </React.Fragment>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
};

export default ApprovalList;
