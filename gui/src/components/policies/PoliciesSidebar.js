 import React, { useState, useEffect } from 'react';
import { User, FileText, Clock, CreditCard, UserPlus, Calendar, ChevronDown } from 'lucide-react';
import { userService } from '../../services';

const PoliciesSidebar = ({ activeTab, setActiveTab }) => {
    const [subordinates, setSubordinates] = useState([]);
    const [selectedUser, setSelectedUser] = useState(null);
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);

    useEffect(() => {
        loadSubordinates();
    }, []);

    const loadSubordinates = async () => {
        try {
            const data = await userService.getSubordinates();
            setSubordinates(data);

            if (data && data.length > 0) {
                setSelectedUser(data[0]);
            }
        } catch (err) {
            console.error("Błąd pobierania podwładnych:", err);
        }
    };

    const handleUserSelect = (user) => {
        setSelectedUser(user);
        setIsDropdownOpen(false);
    };

    const getInitials = () => {
        if (!selectedUser) return 'AN';
        const first = selectedUser.firstName ? selectedUser.firstName.charAt(0) : '';
        const last = selectedUser.lastName ? selectedUser.lastName.charAt(0) : '';
        return (first + last).toUpperCase() || 'U';
    };

    return (
        <div className="policies-sidebar">

            <div
                className="policies-user-card dropdown-toggle"
                onClick={() => setIsDropdownOpen(!isDropdownOpen)}
            >
                <div className="user-avatar">{getInitials()}</div>

                <div className="user-info dropdown-user-info">
                    <div className="user-name">
                        {selectedUser
                            ? `${selectedUser.firstName || ''} ${selectedUser.lastName || ''}`
                            : 'Wybierz pracownika...'}
                    </div>
                    <div className="user-role dropdown-user-email">
                        {selectedUser ? selectedUser.email : 'Ładowanie...'}
                    </div>
                </div>

                <ChevronDown
                    size={18}
                    className={`dropdown-icon ${isDropdownOpen ? 'open' : ''}`}
                />

                {isDropdownOpen && (
                    <div
                        className="user-dropdown-menu"
                        onClick={(e) => e.stopPropagation()}
                    >
                        {subordinates.length === 0 ? (
                            <div className="dropdown-empty">
                                Brak przypisanych pracowników
                            </div>
                        ) : (
                            subordinates.map(user => (
                                <div
                                    key={user.id}
                                    onClick={() => handleUserSelect(user)}
                                    className={`dropdown-item ${selectedUser?.id === user.id ? 'active' : ''}`}
                                >
                                    <strong className="dropdown-item-name">
                                        {user.firstName} {user.lastName}
                                    </strong>
                                    <span className="dropdown-item-email">
                                        {user.email}
                                    </span>
                                </div>
                            ))
                        )}
                    </div>
                )}
            </div>

            <nav className="policies-nav">
                <button
                    className={`policies-nav-item ${activeTab === 'policies' ? 'active' : ''}`}
                    onClick={() => setActiveTab('policies')}
                >
                    <Clock size={18} /> System pracy (Polityki)
                </button>

                <button
                    className={`policies-nav-item ${activeTab === 'holidays' ? 'active' : ''}`}
                    onClick={() => setActiveTab('holidays')}
                >
                    <Calendar size={18} /> Święta globalne
                </button>

                <button
                    className={`policies-nav-item ${activeTab === 'users' ? 'active' : ''}`}
                    onClick={() => setActiveTab('users')}
                >
                    <UserPlus size={18} /> Użytkownicy
                </button>

            </nav>
        </div>
    );
};

export default PoliciesSidebar;
