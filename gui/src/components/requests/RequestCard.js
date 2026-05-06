import React from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../ui/Button';
import { Search } from 'lucide-react';

const RequestCard = ({ request }) => {
    const navigate = useNavigate();

    const translateType = (type) => {
        switch (type) {
            case 'HOLIDAY_LEAVE': return 'Urlop wypoczynkowy';
            case 'SICK_LEAVE': return 'Zwolnienie lekarskie';
            case 'OVERTIME': return 'Odbiór nadgodzin';
            default: return type || 'Inny';
        }
    };

    const translateStatus = (status) => {
        switch (status) {
            case 'APPROVED': return 'Zatwierdzony';
            case 'PENDING': return 'Oczekujący';
            case 'REJECTED': return 'Odrzucony';
            default: return status;
        }
    };

    return (
        <tr>
            <td style={{ fontWeight: 600, color: '#111827' }}>{translateType(request.type)}</td>
            <td>{request.startDate} - {request.endDate}</td>
            <td>{request.daysCount || '-'}</td>
            <td>{request.createdAt || '-'}</td>
            <td>
                <div className="status-cell">
                    <span className={`status-dot ${translateStatus(request.status)}`}></span>
                    {translateStatus(request.status)}
                </div>
            </td>
            <td>{request.reviewerName || '-'}</td>
            <td>
                <div className="actions-cell">
                    <Button
                        className="action-btn"
                        variant="outline"
                        onClick={() => navigate(`/requests/${request.id}`)}
                    >
                        <Search size={20} />
                    </Button>
                </div>
            </td>
        </tr>
    );
};

export default RequestCard;
