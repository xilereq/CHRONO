import React from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../ui/Button';
import { Search } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';

const HOURLY_TYPES = ['EARLY_EXIT', 'OVERTIME_EARN', 'OVERTIME_TAKE'];

const formatDate = (dateValue) => {
    if (!dateValue) return '-';
    return new Date(dateValue).toLocaleDateString('pl-PL');
};

const RequestCard = ({ request }) => {
    const navigate = useNavigate();
    const { t } = useLanguage();

    const typeLabels = {
        HOLIDAY_LEAVE: t('requests.types.holiday', 'Urlop wypoczynkowy'),
        SICK_LEAVE:    t('requests.types.sick', 'Zwolnienie lekarskie'),
        EARLY_EXIT:    t('requests.types.earlyExit', 'Wyjście wcześniejsze'),
        OVERTIME_EARN: t('requests.types.overtimeEarn', 'Zgłoszenie nadgodzin'),
        OVERTIME_TAKE: t('requests.types.overtimeTake', 'Odbiór nadgodzin'),
        HOME_OFFICE:   t('reports.homeOffice', 'Praca zdalna (HO)'),
    };

    const statusLabels = {
        APPROVED:  t('requests.approved', 'Zatwierdzony'),
        PENDING:   t('requests.pending', 'Oczekujący'),
        REJECTED:  t('requests.rejected', 'Odrzucony'),
        CANCELLED: t('requests.cancelled', 'Anulowany'),
    };

    const getDuration = (req) => {
        if (HOURLY_TYPES.includes(req.type)) {
            if (!req.durationSeconds) return '-';
            return `${Math.round(req.durationSeconds / 3600)} ${t('manager.hoursCountLabel', 'godz.')}`;
        }
        if (!req.startDate || !req.endDate) return '-';
        const start = new Date(req.startDate);
        const end = new Date(req.endDate);
        const days = Math.round((end - start) / (1000 * 60 * 60 * 24)) + 1;
        return `${days} ${t('reports.daysUnit', 'dni')}`;
    };

    return (
        <tr>
            <td style={{ fontWeight: 600, color: 'var(--text-main)' }}>{typeLabels[request.type] || request.type || '-'}</td>
            <td>{request.startDate} - {request.endDate}</td>
            <td className="col-hide-mobile">{getDuration(request)}</td>
            <td className="col-hide-mobile">{formatDate(request.creationDateTime)}</td>
            <td>
                <div className="status-cell">
                    <span className={`status-dot ${request.status}`}></span>
                    {statusLabels[request.status] || request.status}
                </div>
            </td>
            <td>{request.reviewerEmail || '-'}</td>
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
