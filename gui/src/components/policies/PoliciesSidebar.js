import React, { useState, useEffect } from 'react';
import { Clock, Calendar, UserCheck, List } from 'lucide-react';
import { userService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';

const PoliciesSidebar = ({ activeTab, setActiveTab }) => {
    const { t } = useLanguage();
    const [users, setUsers] = useState([]);
    const [selectedUser, setSelectedUser] = useState(null);
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);

    const [roleFilter, setRoleFilter] = useState('ALL');
    const [isLoading, setIsLoading] = useState(false);

    useEffect(() => {
        fetchUsersByFilter(roleFilter);
    }, [roleFilter]);

    const fetchUsersByFilter = async (filter) => {
        setIsLoading(true);
        try {
            let data = [];

            if (filter === 'MANAGER') data = await userService.getManagers();
            else if (filter === 'EMPLOYEE') data = await userService.getEmployees();
            else if (filter === 'ADMIN') data = await userService.getAdmins();
            else if (filter === 'HR') data = await userService.getHrs();
            else data = await userService.getAllUsers();

            setUsers(data);

            if (data && data.length > 0 && !selectedUser) {
                setSelectedUser(data[0]);
            }
        } catch (error) {
            console.error(t('policies.fetchUsersConsoleError', "Błąd pobierania użytkowników:"), error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleUserSelect = (user) => {
        setSelectedUser(user);
        setIsDropdownOpen(false);
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
                    <Clock size={18} /> {t('policies.systemPoliciesTab', 'System pracy (Polityki)')}
                </button>

                <button
                    className={`policies-nav-item ${activeTab === 'policyList' ? 'active' : ''}`}
                    onClick={() => setActiveTab('policyList')}
                >
                    <List size={18} /> {t('policies.policyListTab', 'Lista polityk')}
                </button>

                <button
                    className={`policies-nav-item ${activeTab === 'assignPolicy' ? 'active' : ''}`}
                    onClick={() => setActiveTab('assignPolicy')}
                >
                    <UserCheck size={18} /> {t('policies.assignPolicyTab', 'Przypisz politykę')}
                </button>

                <button
                    className={`policies-nav-item ${activeTab === 'holidays' ? 'active' : ''}`}
                    onClick={() => setActiveTab('holidays')}
                >
                    <Calendar size={18} /> {t('policies.globalHolidaysTab', 'Święta globalne')}
                </button>
            </nav>
        </div>
    );
};

export default PoliciesSidebar;
