import React, { useState, useEffect } from 'react';
import { applicationService } from '../../services';
import '../../components/requests/requests.css';
import './ApprovalList.css';

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

const HOURLY_TYPES = ['EARLY_EXIT', 'OVERTIME_EARN', 'OVERTIME_TAKE'];

const getDuration = (request) => {
    if (HOURLY_TYPES.includes(request.type)) {
        if (!request.durationSeconds) return '-';
        return `${Math.round(request.durationSeconds / 3600)} godz.`;
    }
    if (!request.startDate || !request.endDate) return '-';
    const start = new Date(request.startDate);
    const end = new Date(request.endDate);
    const days = Math.round((end - start) / (1000 * 60 * 60 * 24)) + 1;
    return `${days} dni`;
};

const ApprovalList = ({ userRole }) => {

    const [pendingRequests, setPendingRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [activeAction, setActiveAction] = useState({ id: null, type: null });
    const [actionReason, setActionReason] = useState('');
    const [processingId, setProcessingId] = useState(null);
    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(() => window.innerWidth <= 768 ? 5 : 10);

    useEffect(() => {
        const onResize = () => setItemsPerPage(window.innerWidth <= 768 ? 5 : 10);
        window.addEventListener('resize', onResize);
        return () => window.removeEventListener('resize', onResize);
    }, []);

    const loadRequests = async () => {
        setLoading(true);
        try {
            let data;
            if (userRole === 'ADMIN') {
                data = await applicationService.getAllApplications();
            } else if (userRole === 'MANAGER') {
                data = await applicationService.getPendingForManager();
            } else {
                data = [];
            }
            setPendingRequests(data || []);
        } catch (error) {
            console.error("Błąd podczas ładowania wniosków:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (userRole) {
            loadRequests();
        }
    }, [userRole]);

    useEffect(() => {
        setCurrentPage(1);
    }, [pendingRequests, itemsPerPage]);

    const initiateAction = (id, type) => {
        setActiveAction({ id, type });
        setActionReason('');
    };

    const submitAction = async () => {
        setProcessingId(activeAction.id);
        const decisionReason = actionReason;
        try {
            await applicationService.changeStatus(activeAction.id, activeAction.type, decisionReason);
            setActiveAction({ id: null, type: null });
            setActionReason('');
            loadRequests();
        } catch (error) {
            alert("Nie udało się zmienić statusu wniosku.");
        } finally {
            setProcessingId(null);
        }
    };

    const handlePageChange = (page) => {
        setCurrentPage(page);
        setActiveAction({ id: null, type: null });
    };

    const totalPages = Math.ceil(pendingRequests.length / itemsPerPage);
    const pageItems = pendingRequests.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
    );

    const translateStatus = (status) => {
        switch (status) {
            case 'APPROVED': return 'Zatwierdzony';
            case 'PENDING':  return 'Oczekujący';
            case 'REJECTED': return 'Odrzucony';
            case 'CANCELLED': return 'Anulowany';
            default: return status || '-';
        }
    };

    const canActOn = (r) => r.status === 'PENDING';
    const colSpan = userRole === 'ADMIN' ? 7 : 6;

    return (
        <div className="requests-container">
            <div className="section-panel">
                <div className="section-title">
                    {userRole === 'MANAGER' ? 'Wnioski pracownicze' : 'Wnioski w systemie'}
                </div>

                {loading ? (
                    <div className="table-empty-cell">Ładowanie wniosków...</div>
                ) : pendingRequests.length === 0 ? (
                    <div className="table-empty-cell">Brak wniosków do wyświetlenia.</div>
                ) : (
                    <div className="requests-table-container">
                        <table className="requests-table">
                            <thead>
                            <tr>
                                <th>Typ Wniosku</th>
                                <th>Okres / Data</th>
                                <th className="col-hide-mobile">Liczba Dni</th>
                                <th className="col-hide-mobile">Powód</th>
                                <th>E-mail wnioskodawcy</th>
                                {userRole === 'ADMIN' && <th>Status</th>}
                                <th>Akcje</th>
                            </tr>
                            </thead>
                            <tbody>
                            {pageItems.map(r => (
                                <React.Fragment key={r.id}>
                                    <tr>
                                        <td className="request-card-title-cell">{translateType(r.type)}</td>
                                        <td>{r.startDate} - {r.endDate}</td>
                                        <td className="col-hide-mobile">{getDuration(r)}</td>
                                        <td className="col-hide-mobile">{r.reason || '-'}</td>
                                        <td>{r.employeeEmail || '-'}</td>

                                        {userRole === 'ADMIN' && (
                                            <td>
                                                <span className={`status-dot ${r.status}`} style={{ marginRight: '6px' }} />
                                                {translateStatus(r.status)}
                                            </td>
                                        )}

                                        <td>
                                            {canActOn(r) ? (
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
                                            ) : (
                                                <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>—</span>
                                            )}
                                        </td>
                                    </tr>

                                    {activeAction.id === r.id && (
                                        <tr>
                                            <td colSpan={colSpan} className={`action-panel-td ${activeAction.type === 'APPROVED' ? 'approved' : 'rejected'}`}>
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

                        {totalPages > 1 && (
                            <div className="pagination">
                                <span
                                    className={currentPage === 1 ? 'page-disabled' : 'page-btn'}
                                    onClick={() => currentPage > 1 && handlePageChange(currentPage - 1)}
                                >
                                    «
                                </span>
                                {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                                    <span
                                        key={page}
                                        className={page === currentPage ? 'page-active' : 'page-btn'}
                                        onClick={() => handlePageChange(page)}
                                    >
                                        {page}
                                    </span>
                                ))}
                                <span
                                    className={currentPage === totalPages ? 'page-disabled' : 'page-btn'}
                                    onClick={() => currentPage < totalPages && handlePageChange(currentPage + 1)}
                                >
                                    »
                                </span>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default ApprovalList;
