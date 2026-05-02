import React from 'react';
import { Card, Badge } from '../../../components';

import '../../main.css';

const RecentRequests = () => {
    const requests = [
        { id: 1, type: 'Urlop wypoczynkowy', submitDate: '25.03.2026', absenceDate: '10.04 - 14.04.2026', status: 'Zatwierdzony' },
        { id: 2, type: 'Urlop wypoczynkowy', submitDate: '25.03.2026', absenceDate: '10.04 - 14.04.2026', status: 'Oczekujący' },
        { id: 3, type: 'Urlop wypoczynkowy', submitDate: '25.03.2026', absenceDate: '10.04 - 17.04.2026', status: 'Oczekujący' },
        { id: 4, type: 'Urlop wypoczynkowy', submitDate: '25.03.2026', absenceDate: '10.04 - 16.04.2026', status: 'Odrzucony' },
    ];

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
                {requests.map((req) => (
                    <tr key={req.id}>
                        <td>{req.type}</td>
                        <td>{req.submitDate}</td>
                        <td>{req.absenceDate}</td>
                        <td><Badge status={req.status} /></td>
                    </tr>
                ))}
                </tbody>
            </table>
        </Card>
    );
}

export default RecentRequests;
