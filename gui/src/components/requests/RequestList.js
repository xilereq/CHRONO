import React, { useState, useEffect } from 'react';
import { RequestCard, RequestForm, Button } from '../../components';
import { applicationService } from '../../services';

const RequestList = () => {
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);

    // 1. Dodane stany dla poszczególnych filtrów
    const [filterStatus, setFilterStatus] = useState("");
    const [filterType, setFilterType] = useState("");
    const [filterDate, setFilterDate] = useState("");

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

    // 2. Logika filtrowania - generujemy nową tablicę na podstawie wybranych filtrów
    const filteredRequests = requests.filter(r => {
        const matchStatus = filterStatus === "" || r.status === filterStatus;
        const matchType = filterType === "" || r.type === filterType;

        let matchDate = true;
        if (filterDate !== "") {
            // Sprawdzamy czy wybrana z filtra data mieści się w przedziale trwania wniosku
            const requestStart = new Date(r.startDate);
            const requestEnd = new Date(r.endDate);
            const selectedDate = new Date(filterDate);

            matchDate = selectedDate >= requestStart && selectedDate <= requestEnd;
        }

        return matchStatus && matchType && matchDate;
    });

    return (
        <div className="requests-container">
            <div className="section-panel filters-bar">
                {/* 3. Podpięcie stanów (value i onChange) do elementów select i input */}
                <select
                    className="filter-select"
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                >
                    <option value="">Status</option>
                    <option value="APPROVED">Zatwierdzony</option>
                    <option value="PENDING">Weryfikacja</option>
                    <option value="REJECTED">Odrzucony</option>
                </select>

                <select
                    className="filter-select"
                    value={filterType}
                    onChange={(e) => setFilterType(e.target.value)}
                >
                    <option value="">Typ</option>
                    <option value="HOLIDAY_LEAVE">Urlop wypoczynkowy</option>
                    <option value="SICK_LEAVE">Zwolnienie lekarskie</option>
                    <option value="OVERTIME">Nadgodziny</option>
                </select>

                <input
                    type="date"
                    className="filter-input"
                    value={filterDate}
                    onChange={(e) => setFilterDate(e.target.value)}
                />
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
                <div className="section-title">Lista moich wniosków</div>
                {loading ? (
                    <div className="empty">Ładowanie danych z serwera...</div>
                ) : !filteredRequests || filteredRequests.length === 0 ? ( // Użyto filteredRequests
                    <div className="empty">Brak wniosków spełniających kryteria.</div>
                ) : (
                    <div className="requests-table-container">
                        <table className="requests-table">
                            <thead>
                            <tr>
                                <th>Typ Wniosku</th>
                                <th>Okres / Data</th>
                                <th>Liczba Dni/Godz.</th>
                                <th>Data Złożenia</th>
                                <th>Status</th>
                                <th>Zatwierdzający</th>
                                <th>Akcje</th>
                            </tr>
                            </thead>
                            <tbody>
                            {/* 4. Mapowanie pofiltrowanej tablicy zamiast oryginalnej */}
                            {filteredRequests.map(r => (
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
