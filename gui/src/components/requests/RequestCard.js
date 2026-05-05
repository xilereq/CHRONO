import React from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../ui/Button';
import { Search } from 'lucide-react';

const RequestCard = ({ request }) => {
    const navigate = useNavigate();

    return (
        <tr>
            <td style={{ fontWeight: 600, color: '#111827' }}>{request.title || request.type}</td>
            <td>{request.startDate} - {request.endDate}</td>
            <td>{request.daysCount || '-'}</td>
            <td>{request.createdAt || '-'}</td>
            <td>
                <div className="status-cell">
                    <span className={`status-dot ${request.status}`}></span>
                    {request.status}
                </div>
            </td>
            <td>{request.reviewerName || '-'}</td>
            <td>
                <div className="actions-cell">
                    <Button
                        className="action-btn"
                        variant="outline"
                        onClick={() => navigate(`/my-requests/${request.id}`)}
                    >
                        <Search size={20} />
                    </Button>
                </div>
            </td>
        </tr>
    );
};

export default RequestCard;
