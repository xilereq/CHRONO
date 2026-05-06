import React, { useState, useEffect } from 'react';
import { RequestCard, RequestForm, Button } from '../../components';
import { applicationService } from '../../services';

const RequestList = () => {
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const userId = 1;

    const fetchRequests = async () => {
        setLoading(true);
        try {
            const data = await applicationService.getUserApplications(userId);
            setRequests(data);
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchRequests();
    }, [userId]);

    return (
        <div className="requests-container">
            <div className="section-panel filters-bar">
                <select className="filter-select">
                    <option value="">Status</option>
                    <option value="APPROVED">Zatwierdzony</option>
                    <option value="PENDING">Weryfikacja</option>
                    <option value="REJECTED">Odrzucony</option>
                </select>
                <select className="filter-select">
                    <option value="">Typ</option>
                    <option value="Urlop wypoczynkowy">Urlop wypoczynkowy</option>
                    <option value="Nadgodziny">Nadgodziny</option>
                </select>
                <input type="date" className="filter-input" />
            </div>

            {showForm && (
                <RequestForm
                    onCancel={() => setShowForm(false)}
                    onSuccess={() => {
                        setShowForm(false);
                        fetchRequests();
                    }}
                />
            )}

            {!showForm && (
                <div className="section-panel">
                    <div className="section-title">Nowy wniosek</div>
                    <div className="submit-actions submit-actions-row">
                        <Button
                            className="big-yellow-btn"
                            variant="primary"
                            onClick={() => setShowForm(true)}
                        >
                            NOWY WNIOSEK
                        </Button>
                    </div>
                </div>
            )}

            <div className="section-panel">
                <div className="section-title">Lista moich wniosk w</div>
                {loading ? (
                    <div className="empty">Ładowanie danych z serwera...</div>
                ) : !requests || requests.length === 0 ? (
                    <div className="empty">Brak wniosk w w wybranym okresie.</div>
                ) : (
                    <div className="requests-table-container">
                        <table className="requests-table">
                            <thead>
                            <tr>
                                <th>Typ Wniosku</th>
                                <th>Okres / Data</th>
                                <th>Liczba Dni/Godz.</th>
                                <th>Data Zlenia</th>
                                <th>Status</th>
                                <th>Zatwierdzający</th>
                                <th>Akcje</th>
                            </tr>
                            </thead>
                            <tbody>
                            {requests.map(r => (
                                <RequestCard key={r.id} request={r} />
                            ))}
                            </tbody>
                        </table>
                        <div className="pagination">
                            <span className="page-active">1</span>
                            <span>2</span>
                            <span>3</span>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default RequestList;
