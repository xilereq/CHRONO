import React, { useState, useEffect } from 'react';
import { Clock, LayoutGrid, FileText, BarChart2, Settings, Shield, Terminal, Users, Calendar  } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { userService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import './layout.css';

const Sidebar = () => {
    const { t } = useLanguage();
    const [isAdmin, setIsAdmin] = useState(false);
    const [isHr, setIsHr] = useState(false);
    const [isManager, setIsManager] = useState(false);

    useEffect(() => {
        const fetchRole = async () => {
            try {
                const response = await userService.getMyRole();
                if (response.role === "ROLE_ADMIN") setIsAdmin(true);
                if (response.role === "ROLE_HR") setIsHr(true);
                if (response.role === "ROLE_MANAGER") setIsManager(true);
            } catch (error) {
                console.error(t('errors.failedToFetchRole', "Nie udało się pobrać roli"), error);
            }
        };
        fetchRole();
    }, [t]);

    return (
        <aside className="sidebar">
            <div className="sidebar-logo">
                <Clock size={32} />
            </div>

            <nav
                className="sidebar-nav"
                style={{
                    overflowY: 'auto',
                    overflowX: 'hidden',
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column'
                }}
            >
                <NavLink to="/" end className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                    <LayoutGrid size={24} />
                    <span>{t('sidebar.dashboard', 'Pulpit')}</span>
                </NavLink>
                <NavLink to="/requests" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                    <FileText size={24} />
                    <span>{t('sidebar.requests', 'Wnioski')}</span>
                </NavLink>
                <NavLink to="/raporty" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                    <BarChart2 size={24} />
                    <span>{t('sidebar.reports', 'Raporty')}</span>
                </NavLink>
                <NavLink to="/grafik" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                    <Calendar  size={24} />
                    <span>{t('sidebar.schedule', 'Grafik')}</span>
                </NavLink>

                {(isHr || isAdmin) && (
                    <NavLink to="/users" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                        <Users size={24} />
                        <span style={{ textAlign: 'center' }}>{t('sidebar.userManagement', 'Zarządzanie Użytkownikami')}</span>
                    </NavLink>
                )}

                {(isManager || isAdmin) && (
                    <NavLink to="/manager" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                        <Clock size={24} />
                        <span style={{ textAlign: 'center' }}>{t('sidebar.teamSchedule', 'Czas pracy zespołu')}</span>
                    </NavLink>
                )}

                {isAdmin && (
                    <>
                        <NavLink to="/policies" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                            <Shield size={24} />
                            <span style={{ textAlign: 'center' }}>{t('sidebar.adminPanel', 'Panel Administratora')}</span>
                        </NavLink>
                        <NavLink to="/logs" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                            <Terminal size={24} />
                            <span style={{ textAlign: 'center' }}>{t('sidebar.systemLogs', 'Logi Systemowe')}</span>
                        </NavLink>
                    </>
                )}

                <NavLink to="/ustawienia" className={({isActive}) => isActive ? 'sidebar-item mt-auto active' : 'sidebar-item mt-auto'}>
                    <Settings size={24} />
                    <span>{t('sidebar.settings', 'Ustawienia')}</span>
                </NavLink>
            </nav>
        </aside>
    );
};

export default Sidebar;
