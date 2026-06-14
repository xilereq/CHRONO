import React from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../ui/Button';
import { Search } from 'lucide-react';

const translateType = (type) => {
    switch (type) {
        case 'HOLIDAY_LEAVE': return 'Urlop wypoczynkowy';
        case 'SICK_LEAVE': return 'Zwolnienie lekarskie';
        case 'EARLY_EXIT': return 'Wyjście wcześniejsze';
        case 'OVERTIME_EARN': return 'Zgłoszenie nadgodzin';
        case 'OVERTIME_TAKE': return 'Odbiór nadgodzin';
        case 'HOME_OFFICE':   return 'Praca zdalna (HO)';
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

const formatDate = (dateValue) => {
    if (!dateValue) return '-';
    return new Date(dateValue).toLocaleDateString('pl-PL');
};

const RequestCard = ({ request }) => {
    const navigate = useNavigate();

    return (
        <tr>
            <td style={{ fontWeight: 600, color: 'var(--text-main)' }}>{translateType(request.type)}</td>
            <td>{request.startDate} - {request.endDate}</td>
            <td className="col-hide-mobile">{getDuration(request)}</td>
            <td className="col-hide-mobile">{formatDate(request.creationDateTime)}</td>
            <td>
                <div className="status-cell">
                    <span className={`status-dot ${request.status}`}></span>
                    {translateStatus(request.status)}
                </div>
            </td>
            <td>{request.reviewerEmail|| '-'}</td>
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
