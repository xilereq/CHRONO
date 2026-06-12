import React, { useState, useEffect } from 'react';
import { Card, Badge } from '../index';
import { applicationService } from '../../services';
import '../../pages/main.css';

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

const RecentRequests = () => {
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchRequests = async () => {
            try {
                const data = await applicationService.getUserApplications();
                setRequests(data.slice(0, 4));
            } catch (error) {
                console.error(error);
            } finally {
                setLoading(false);
            }
        };

        fetchRequests();
    }, []);

    return (
        <Card title="OSTATNIE WNIOSKI" className="table-container">
            <table className="requests-table">
                <thead>
                <tr>
                    <th>Typ</th>
                    <th>Data zgłoszenia</th>
                    <th>Data nieobecności</th>
                    <th>Status</th>
                </tr>
                </thead>
                <tbody>
                {loading ? (
                    <tr>
                        <td colSpan="4" style={{ textAlign: 'center', padding: '1rem' }}>Ładowanie danych...</td>
                    </tr>
                ) : requests.length === 0 ? (
                    <tr>
                        <td colSpan="4" style={{ textAlign: 'center', padding: '1rem' }}>Brak ostatnich wniosków.</td>
                    </tr>
                ) : (
                    requests.map((req) => (
                        <tr key={req.id}>
                            <td>{translateType(req.type)}</td>
                            <td>{req.creationDateTime ? new Date(req.creationDateTime).toLocaleDateString('pl-PL') : '-'}</td>
                            <td>{req.startDate} - {req.endDate}</td>
                            <td><Badge status={translateStatus(req.status)} /></td>
                        </tr>
                    ))
                )}
                </tbody>
            </table>
        </Card>
    );
}

export default RecentRequests;
