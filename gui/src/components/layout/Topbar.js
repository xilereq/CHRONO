import React from 'react';
import { Bell, User } from 'lucide-react';

import './layout.css';

const Topbar = () => {
    return (
        <header className="topbar">
            <h1></h1>
            <div className="topbar-right">
                <div className="user-profile">
                    <div className="user-icon">
                        <User size={16} />
                    </div>
                    Jan Kowalski (Pracownik)
                    <div className="notification-bell">
                        <Bell size={18} />
                        <span className="notification-dot"></span>
                    </div>
                </div>
            </div>
        </header>
    );
}

export default Topbar;