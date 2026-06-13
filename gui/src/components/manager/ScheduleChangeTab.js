import React, { useState, useEffect } from 'react';
import { workLogRequestService } from '../../services';
import { Check, X, Clock } from 'lucide-react';
import './manager.css';

const ScheduleChangeTab = () => {
    const [requests, setRequests] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    useEffect(() => {
        fetchRequests();
    }, []);

    const fetchRequests = async () => {
        setIsLoading(true);
        setError('');
        setMessage('');
        try {
            const data = await workLogRequestService.getPendingRequests();
            setRequests(data || []);
        } catch (err) {
            console.error("Błąd pobierania wniosków:", err);
            setError('Nie udało się pobrać oczekujących wniosków z serwera.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleApprove = async (id) => {
        setError('');
        setMessage('');
        try {
            await workLogRequestService.approveRequest(id);
            setMessage("Wniosek został pomyślnie zaakceptowany.");
            setRequests(prev => prev.filter(req => req.id !== id));
        } catch (err) {
            setError(err.message || 'Błąd podczas akceptacji wniosku.');
        }
    };

    const handleReject = async (id) => {
        setError('');
        setMessage('');
        try {
            await workLogRequestService.rejectRequest(id);
            setMessage("Wniosek został odrzucony.");
            setRequests(prev => prev.filter(req => req.id !== id));
        } catch (err) {
            setError(err.message || 'Błąd podczas odrzucania wniosku.');
        }
    };

    return (
        <div className="policy-section">
            <h2 className="section-title">Oczekujące eventy do akceptacji</h2>

            {error && <div className="manager-alert-error">{error}</div>}
            {message && <div className="manager-alert-success"><strong>{message}</strong></div>}

            {isLoading ? (
                <div className="manager-loading-state">Ładowanie wniosków...</div>
            ) : requests.length === 0 ? (
                <div className="manager-empty-state">
                    <Clock size={40} className="manager-empty-icon" />
                    <p className="manager-empty-text">Brak oczekujących wniosków w Twoim zespole.</p>
                </div>
            ) : (
                <div className="manager-request-list">
                    {requests.map(req => (
                        <div key={req.id} className="manager-request-card">
                            <div>
                                <h3 className="manager-request-title">
                                    {req.employeeName || 'Nieznany pracownik'}
                                    {req.type && (
                                        <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginLeft: '10px' }}>
                                            [{req.type}]
                                        </span>
                                    )}
                                </h3>

                                <div className="manager-request-details">
                                    <span><strong>Data:</strong> {req.startDate || req.date}</span>
                                    {(req.startTime || req.newHours) && (
                                        <span>
                                            <strong>Godziny: </strong>
                                            {req.newHours || `${req.startTime} - ${req.endTime}`}
                                        </span>
                                    )}
                                </div>

                                {req.reason && (
                                    <p className="manager-request-reason">
                                        Powód: {req.reason}
                                    </p>
                                )}
                            </div>

                            <div className="manager-actions">
                                <button onClick={() => handleReject(req.id)} className="btn-reject">
                                    <X size={16} /> Odrzuć
                                </button>
                                <button onClick={() => handleApprove(req.id)} className="btn-approve">
                                    <Check size={16} /> Akceptuj
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default ScheduleChangeTab;
