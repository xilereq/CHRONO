import React from 'react';
import { useNavigate } from 'react-router-dom';

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
                    <button
                        className="action-btn"
                        title="Podgląd"
                        onClick={() => navigate(`/my-requests/${request.id}`)}
                    >
                        👁️
                    </button>

                    {/* Dodatkowe akcje na podstawie statusu */}
                    {request.status === 'Zatwierdzony' && (
                        <button className="action-btn" title="Drukuj PDF">🖨️</button>
                    )}
                    {request.status === 'Weryfikacja' && (
                        <>
                            <button className="action-btn" title="Edytuj">📝</button>
                            <button className="action-btn delete" title="Usuń">🗑️</button>
                        </>
                    )}
                </div>
            </td>
        </tr>
    );
};

export default RequestCard;