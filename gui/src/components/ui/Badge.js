import React from 'react';

// UI styles
import './ui.css';

const Badge = ({ status }) => {
    if (status === 'Zatwierdzony') {
        return <span className="badge-success-text">Zatwierdzony</span>;
    }
    if (status === 'Oczekujący') {
        return (
            <div className="badge-dots">
                <span className="dot dot-yellow"></span>
                <span className="dot dot-green"></span>
            </div>
        );
    }
    if (status === 'Odrzucony') {
        return (
            <div className="badge-dots">
                <span className="dot dot-red"></span>
            </div>
        );
    }
    return null;
}
export default Badge;