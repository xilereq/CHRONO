import React from 'react';
import { Bell, User, LogOut } from 'lucide-react';

import './layout.css';

const Topbar = ({ userEmail, onLogout }) => {
    return (
        <header className="topbar">
            <h1></h1>
            <div className="topbar-right">
                <div className="user-profile">
                    <div className="user-icon">
                        <User size={16} />
                    </div>
                    <span className="user-email" title={userEmail || 'Zalogowany'}>
                        {userEmail || 'Zalogowany'}
                    </span>
                    <div className="notification-bell">
                        <Bell size={18} />
                        <span className="notification-dot"></span>
                    </div>
                </div>

                {/* Przycisk wylogowania */}
                <button onClick={onLogout} className="logout-btn" title="Wyloguj się">
                    <LogOut size={18} />
                </button>
            </div>
        </header>
    );
}

export default Topbar;
