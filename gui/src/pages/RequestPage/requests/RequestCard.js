import React from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../../../components/ui/Button';

const RequestCard = ({ request }) => {
    const navigate = useNavigate();

    return (
        <tr>
            <td style={{ fontWeight: 600, color: '#111827' }}>{request.title}</td>
            <td>{request.period || request.date}</td>
            <td>{request.duration || '-'}</td>
            <td>{request.submittedDate || request.date}</td>
            <td>
                <div className="status-cell">
                    <span className={`status-dot ${request.status}`}></span>
                    {request.status}
                </div>
            </td>
            <td>{request.approver || '-'}</td>
            <td>
                <div className="actions-cell">
                    {/* Przycisk Podglądu */}
                    <Button
                        className="action-btn"
                        variant="outline"
                        title="Podgląd"
                        onClick={() => navigate(`/my-requests/${request.id}`)}
                    >
                        👁️
                    </Button>

                    {/* Dodatkowe akcje na podstawie statusu */}
                    {request.status === 'Zatwierdzony' && (
                        <Button className="action-btn" variant="outline" title="Drukuj PDF">🖨️</Button>
                    )}
                    {request.status === 'Weryfikacja' && (
                        <>
                            <Button className="action-btn" variant="outline" title="Edytuj">📝</Button>
                            <Button className="action-btn delete" variant="outline" title="Usuń">🗑️</Button>
                        </>
                    )}
                </div>
            </td>
        </tr>
    );
};

export default RequestCard;
