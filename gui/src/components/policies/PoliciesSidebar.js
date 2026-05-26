import React, { useState, useEffect } from 'react';
import { Clock, Calendar, UserPlus, ChevronDown, Filter } from 'lucide-react';
import { userService } from '../../services';

const PoliciesSidebar = ({ activeTab, setActiveTab }) => {
    const [users, setUsers] = useState([]);
    const [selectedUser, setSelectedUser] = useState(null);
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);

    // Stany dla filtrów i wskaźnika ładowania
    const [roleFilter, setRoleFilter] = useState('ALL');
    const [isLoading, setIsLoading] = useState(false);

    // Kiedy zmieni się filtr, automatycznie pobieramy odpowiednią listę z backendu
    useEffect(() => {
        fetchUsersByFilter(roleFilter);
    }, [roleFilter]);

    const fetchUsersByFilter = async (filter) => {
        setIsLoading(true);
        try {
            let data = [];

            // Wykorzystanie odpowiednich endpointów w zależności od wyboru
            if (filter === 'MANAGER') data = await userService.getManagers();
            else if (filter === 'EMPLOYEE') data = await userService.getEmployees();
            else if (filter === 'ADMIN') data = await userService.getAdmins();
            else if (filter === 'HR') data = await userService.getHrs();
            else data = await userService.getAllUsers(); // Wszyscy (dla Admina)

            setUsers(data);

            // Ustawienie pierwszego użytkownika jako wybranego (tylko, jeśli żaden nie był wybrany)
            if (data && data.length > 0 && !selectedUser) {
                setSelectedUser(data[0]);
            }
        } catch (error) {
            console.error("Błąd pobierania użytkowników:", error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleUserSelect = (user) => {
        setSelectedUser(user);
        setIsDropdownOpen(false); // Zamyka menu po wyborze użytkownika
    };

    const getInitials = () => {
        if (!selectedUser) return 'AN';
        const first = selectedUser.name ? selectedUser.name.charAt(0) : '';
        const last = selectedUser.surname ? selectedUser.surname.charAt(0) : '';
        return (first + last).toUpperCase() || 'U';
    };

    return (
        <div className="policies-sidebar">
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

            <div
                className="policies-user-card dropdown-toggle"
                onClick={() => setIsDropdownOpen(!isDropdownOpen)}
            >
                <div className="user-avatar">{getInitials()}</div>

                <div className="user-info dropdown-user-info">
                    <div className="user-name">
                        {selectedUser
                            ? `${selectedUser.name || ''} ${selectedUser.surname || ''}`
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
                        onClick={(e) => e.stopPropagation()} // Zapobiega zamknięciu menu po kliknięciu w jego środku
                        style={{ display: 'flex', flexDirection: 'column' }}
                    >
                        {/* --- PANEL FILTROWANIA --- */}
                        <div style={{ padding: '10px 15px', borderBottom: '1px solid #eee', backgroundColor: '#f9fafb' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '5px', marginBottom: '6px' }}>
                                <Filter size={14} color="#6b7280" />
                                <span style={{ fontSize: '12px', fontWeight: 600, color: '#6b7280' }}>Filtruj po roli:</span>
                            </div>
                            <select
                                className="form-input"
                                style={{ width: '100%', padding: '6px 8px', fontSize: '13px', height: 'auto' }}
                                value={roleFilter}
                                onChange={(e) => setRoleFilter(e.target.value)}
                            >
                                <option value="ALL">Wszyscy użytkownicy</option>
                                <option value="EMPLOYEE">Pracownicy (Zwykli)</option>
                                <option value="MANAGER">Kierownicy</option>
                                <option value="HR">Dział HR</option>
                                <option value="ADMIN">Administratorzy</option>
                            </select>
                        </div>

                        {/* --- LISTA UŻYTKOWNIKÓW --- */}
                        <div style={{ maxHeight: '240px', overflowY: 'auto' }}>
                            {isLoading ? (
                                <div className="dropdown-empty">Wyszukiwanie użytkowników...</div>
                            ) : users.length === 0 ? (
                                <div className="dropdown-empty">
                                    Brak użytkowników dla wybranej roli
                                </div>
                            ) : (
                                users.map(user => (
                                    <div
                                        key={user.id}
                                        onClick={() => handleUserSelect(user)}
                                        className={`dropdown-item ${selectedUser?.id === user.id ? 'active' : ''}`}
                                    >
                                        <strong className="dropdown-item-name">
                                            {user.name} {user.surname}
                                        </strong>
                                        <span className="dropdown-item-email">
                                            {user.email}
                                        </span>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default PoliciesSidebar;
