import React, { useState, useEffect } from 'react';
import { Clock, LayoutGrid, FileText, BarChart2, Calendar, Settings, Shield, Terminal } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { userService } from '../../services';

import './layout.css';

const Sidebar = () => {
    const [isAdmin, setIsAdmin] = useState(false);

    useEffect(() => {
        const fetchRole = async () => {
            try {
                const response = await userService.getMyRole();
                if (response.role === "ROLE_ADMIN") {
                    setIsAdmin(true);
                }
            } catch (error) {
                console.error("Nie udało się pobrać roli", error);
            }
        };

        fetchRole();
    }, []);
    return (
        <aside className="sidebar">
            <div className="sidebar-logo">
                <Clock size={32} />
            </div>
            <nav className="sidebar-nav">
                <NavLink to="/" end className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                    <LayoutGrid size={24} />
                    <span>Pulpit</span>
                </NavLink>
                <NavLink to="/requests" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                    <FileText size={24} />
                    <span>Wnioski</span>
                </NavLink>
                <NavLink to="/raporty" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                    <BarChart2 size={24} />
                    <span>Raporty</span>
                </NavLink>
                {isAdmin && (
                    <>
                        <NavLink to="/policies" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                            <Shield size={24} />
                            <span style={{ textAlign: 'center' }}>Panel Administratora</span>
                        </NavLink>

                        <NavLink to="/logs" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                            <Terminal size={24} />
                            <span style={{ textAlign: 'center' }}>Logi Systemowe</span>
                        </NavLink>
                    </>
                )}

                <NavLink to="/ustawienia" className={({isActive}) => isActive ? 'sidebar-item mt-auto active' : 'sidebar-item mt-auto'}>
                    <Settings size={24} />
                    <span>Ustawienia</span>
                </NavLink>
            </nav>
        </aside>
    );
}

export default Sidebar;
