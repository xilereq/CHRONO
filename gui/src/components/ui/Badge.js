import React from 'react';
import './ui.css';

const Badge = ({ status, label }) => {
    const display = label || status;

    if (status === 'APPROVED') {
        return <span className="badge-success-text">{display}</span>;
    }
    if (status === 'PENDING') {
        return (
            <div className="badge-dots">
                <span className="dot dot-yellow"></span>
                <span className="dot dot-green"></span>
            </div>
        );
    }
    if (status === 'REJECTED') {
        return (
            <div className="badge-dots">
                <span className="dot dot-red"></span>
            </div>
        );
    }
    if (status === 'CANCELLED') {
        return <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{display}</span>;
    }
    return <span>{display}</span>;
};

export default Badge;
