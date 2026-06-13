import React, { useState, useEffect } from 'react';
import { userService } from '../../services';
import { Check, X, Clock } from 'lucide-react';
import './manager.css'; // <--- IMPORT NOWYCH STYLÓW

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
        try {
            const data = await userService.getScheduleRequests();
            setRequests(data || []);
        } catch (err) {
            console.error("Błąd pobierania wniosków:", err);

            setRequests([
                { id: 1, employeeName: 'Jan Kowalski', date: '2023-11-20', oldHours: '08:00 - 16:00', newHours: '09:00 - 17:00', reason: 'Wizyta u lekarza' },
                { id: 2, employeeName: 'Anna Nowak', date: '2023-11-22', oldHours: '09:00 - 17:00', newHours: '07:00 - 15:00', reason: 'Odbiór dziecka z przedszkola' }
            ]);
        } finally {
            setIsLoading(false);
        }
    };

    const handleApprove = async (id) => {
        setError(''); setMessage('');
        try {
            await userService.approveScheduleRequest(id);
            setMessage("Wniosek został zaakceptowany.");
            setRequests(prev => prev.filter(req => req.id !== id));
        } catch (err) {
            setError(err.message || 'Błąd podczas akceptacji wniosku.');
        }
    };

    const handleReject = async (id) => {
        setError(''); setMessage('');
        try {
            await userService.rejectScheduleRequest(id);
            setMessage("Wniosek został odrzucony.");
            setRequests(prev => prev.filter(req => req.id !== id));
        } catch (err) {
            setError(err.message || 'Błąd podczas odrzucania wniosku.');
        }
    };

    return (
        <div className="policy-section">
            <h2 className="section-title">Oczekujące zmiany grafiku</h2>
            <p className="section-subtitle mb-3">Przejrzyj i rozpatrz prośby pracowników o zmianę godzin pracy.</p>

            {error && <div className="manager-alert-error">{error}</div>}
            {message && <div className="manager-alert-success"><strong>{message}</strong></div>}

            {isLoading ? (
                <div className="manager-loading-state">Ładowanie wniosków...</div>
            ) : requests.length === 0 ? (
                <div className="manager-empty-state">
                    <Clock size={40} className="manager-empty-icon" />
                    <p className="manager-empty-text">Brak oczekujących wniosków o zmianę grafiku.</p>
                </div>
            ) : (
                <div className="manager-request-list">
                    {requests.map(req => (
                        <div key={req.id} className="manager-request-card">
                            <div>
                                <h3 className="manager-request-title">{req.employeeName}</h3>
                                <div className="manager-request-details">
                                    <span><strong>Data:</strong> {req.date}</span>
                                    <span><strong>Proponowane godziny:</strong> {req.newHours} (zamiast {req.oldHours})</span>
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
