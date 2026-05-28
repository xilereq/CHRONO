import React from 'react';
import './ui.css';

const Card = ({ title, children, className = '' }) => {
    return (
        <div className={`card ${className}`}>
            {title && <h3 className="card-title">{title}</h3>}
            {children}
        </div>
    );
}

export default Card;
