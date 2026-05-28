import React from 'react';
import './ui.css';

const Button = ({ children, variant = 'primary', className = '', ...props }) => {
    const variantClass = variant === 'primary' ? 'btn-primary' : 'btn-outline';

    return (
        <button className={`btn ${variantClass} ${className}`} {...props}>
            {children}
        </button>
    );
}

export default Button;
