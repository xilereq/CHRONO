import React, { useState, useEffect } from 'react';
import { workLogRequestService } from '../../services';
import { Check, X, Clock } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';
import './manager.css';

const ScheduleChangeTab = () => {
    const { t } = useLanguage();
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
            console.error(t('manager.fetchRequestsConsoleError', "Błąd pobierania wniosków:"), err);
            setError(t('manager.fetchRequestsError', 'Nie udało się pobrać oczekujących wniosków z serwera.'));
        } finally {
            setIsLoading(false);
        }
    };

    const handleApprove = async (id) => {
        setError('');
        setMessage('');
        try {
            await workLogRequestService.approveRequest(id);
            setMessage(t('manager.approveSuccess', "Wniosek został pomyślnie zaakceptowany."));
            setRequests(prev => prev.filter(req => req.id !== id));
        } catch (err) {
            setError(err.message || t('manager.approveError', 'Błąd podczas akceptacji wniosku.'));
        }
    };

    const handleReject = async (id) => {
        setError('');
        setMessage('');
        try {
            await workLogRequestService.rejectRequest(id);
            setMessage(t('manager.rejectSuccess', "Wniosek został odrzucony."));
            setRequests(prev => prev.filter(req => req.id !== id));
        } catch (err) {
            setError(err.message || t('manager.rejectError', 'Błąd podczas odrzucania wniosku.'));
        }
    };

    return (
        <div className="policy-section">
            <h2 className="section-title">{t('manager.pendingEventsTitle', 'Oczekujące eventy do akceptacji')}</h2>

            {error && <div className="manager-alert-error">{error}</div>}
            {message && <div className="manager-alert-success"><strong>{message}</strong></div>}

            {isLoading ? (
                <div className="manager-loading-state">{t('manager.loadingRequests', 'Ładowanie wniosków...')}</div>
            ) : requests.length === 0 ? (
                <div className="manager-empty-state">
                    <Clock size={40} className="manager-empty-icon" />
                    <p className="manager-empty-text">{t('manager.noPendingRequests', 'Brak oczekujących wniosków w Twoim zespole.')}</p>
                </div>
            ) : (
                <div className="manager-request-list">
                    {requests.map(req => (
                        <div key={req.id} className="manager-request-card">
                            <div>
                                <h3 className="manager-request-title">
                                    {req.employeeName || t('manager.unknownEmployee', 'Nieznany pracownik')}
                                    {req.type && (
                                        <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginLeft: '10px' }}>
                                            [{req.type}]
                                        </span>
                                    )}
                                </h3>

                                <div className="manager-request-details">
                                    <span><strong>{t('manager.dateField', 'Data:')}</strong> {req.startDate || req.date}</span>
                                    {(req.startTime || req.newHours) && (
                                        <span>
                                            <strong>{t('manager.hoursField', 'Godziny: ')}</strong>
                                            {req.newHours || `${req.startTime} - ${req.endTime}`}
                                        </span>
                                    )}
                                </div>

                                {req.reason && (
                                    <p className="manager-request-reason">
                                        {t('manager.reasonField', 'Powód:')} {req.reason}
                                    </p>
                                )}
                            </div>

                            <div className="manager-actions">
                                <button onClick={() => handleReject(req.id)} className="btn-reject">
                                    <X size={16} /> {t('manager.rejectBtn', 'Odrzuć')}
                                </button>
                                <button onClick={() => handleApprove(req.id)} className="btn-approve">
                                    <Check size={16} /> {t('manager.approveBtn', 'Akceptuj')}
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
