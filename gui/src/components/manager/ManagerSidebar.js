import React from 'react';
import { Clock, UserPlus } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';

const ManagerSidebar = ({ activeTab, setActiveTab }) => {
    const { t } = useLanguage();

    return (
        <aside className="policies-sidebar">
            <div className="policies-sidebar-header">
                <h2>{t('manager.toolsTitle', 'Narzędzia')}</h2>
            </div>
            <nav className="policies-nav">

                <button
                    className={`policies-nav-item ${activeTab === 'schedule' ? 'active' : ''}`}
                    onClick={() => setActiveTab('schedule')}
                >
                    <Clock size={18} /> {t('manager.teamScheduleTab', 'Czas pracy zespołu')}
                </button>

                <button
                    className={`policies-nav-item ${activeTab === 'team-requests' ? 'active' : ''}`}
                    onClick={() => setActiveTab('team-requests')}
                >
                    <UserPlus size={18} /> {t('manager.employeeRequestsTab', 'Wnioski za pracownika')}
                </button>

            </nav>
        </aside>
    );
};

export default ManagerSidebar;
