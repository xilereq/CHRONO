import React from 'react';
import { Clock, LayoutGrid, FileText, BarChart2, Calendar, Settings } from 'lucide-react';
import { NavLink } from 'react-router-dom';

import './layout.css';

const Sidebar = () => {
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
                <NavLink to="/my-requests" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                    <FileText size={24} />
                    <span>My Requests</span>
                </NavLink>
                <NavLink to="/raporty" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                    <BarChart2 size={24} />
                    <span>Raporty</span>
                </NavLink>
                <NavLink to="/grafik" className={({isActive}) => isActive ? 'sidebar-item active' : 'sidebar-item'}>
                    <Calendar size={24} />
                    <span>Grafik</span>
                </NavLink>
                <NavLink to="/ustawienia" className={({isActive}) => isActive ? 'sidebar-item mt-auto active' : 'sidebar-item mt-auto'}>
                    <Settings size={24} />
                    <span>Ustawienia</span>
                </NavLink>
            </nav>
        </aside>
    );
}

export default Sidebar;