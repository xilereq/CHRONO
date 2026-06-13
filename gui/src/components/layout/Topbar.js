import React, { useState, useEffect } from 'react';
import { Bell, User, LogOut, Check } from 'lucide-react';
import { notificationService } from '../../services';
import './layout.css';

const Topbar = ({ userEmail, onLogout }) => {
    const [notifications, setNotifications] = useState([]);
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);

    useEffect(() => {
        const fetchUnread = async () => {
            try {
                const data = await notificationService.getUnread();
                setNotifications(data);
            } catch (err) {
                console.error("Błąd pobierania powiadomień", err);
            }
        };
        fetchUnread();

        const sseUrl = notificationService.getSseUrl();
        const eventSource = new EventSource(sseUrl);

        eventSource.addEventListener('notification', (event) => {
            try {
                const newNotification = JSON.parse(event.data);
                setNotifications(prev => [newNotification, ...prev]);
            } catch (err) {
                console.error("Błąd parsowania powiadomienia", err);
            }
        });

        eventSource.onerror = (err) => {
            console.error("Błąd połączenia SSE", err);
            eventSource.close();
        };

        return () => {
            eventSource.close();
        };
    }, []);

    const handleMarkAsRead = async (id) => {
        try {
            await notificationService.markAsRead(id);
            setNotifications(prev => prev.filter(n => n.id !== id));
        } catch (err) {
            console.error("Nie udało się oznaczyć jako przeczytane", err);
        }
    };

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

                    <div
                        className="notification-bell"
                        onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                    >
                        <Bell size={18} />
                        {notifications.length > 0 && (
                            <span className="notification-dot"></span>
                        )}

                        {isDropdownOpen && (
                            <div className="notification-dropdown">
                                <div className="notification-dropdown-header">
                                    Powiadomienia ({notifications.length})
                                </div>
                                <div className="notification-dropdown-list">
                                    {notifications.length === 0 ? (
                                        <div className="notification-empty">Brak nowych powiadomień</div>
                                    ) : (
                                        notifications.map(notif => (
                                            <div key={notif.id} className="notification-item">
                                                <p className="notification-message">{notif.message}</p>
                                                <button
                                                    className="notification-read-btn"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleMarkAsRead(notif.id);
                                                    }}
                                                >
                                                    <Check size={14} /> Odznacz
                                                </button>
                                            </div>
                                        ))
                                    )}
                                </div>
                            </div>
                        )}
                    </div>
                </div>

                <button onClick={onLogout} className="logout-btn" title="Wyloguj się">
                    <LogOut size={18} />
                </button>
            </div>
        </header>
    );
}

export default Topbar;
