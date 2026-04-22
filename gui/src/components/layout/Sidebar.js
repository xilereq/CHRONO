import React from 'react';
import { Clock, LayoutGrid, FileText, BarChart2, Calendar, Settings } from 'lucide-react';

import './layout.css';

const Sidebar = () => {
    return (
        <aside className="sidebar">
            <div className="sidebar-logo">
                <Clock size={32} />
            </div>
            <nav className="sidebar-nav">
                <div className="sidebar-item active">
                    <LayoutGrid size={24} />
                    <span>Pulpit</span>
                </div>
                <div className="sidebar-item">
                    <FileText size={24} />
                    <span>Moje Wnioski</span>
                </div>
                <div className="sidebar-item">
                    <BarChart2 size={24} />
                    <span>Raporty</span>
                </div>
                <div className="sidebar-item">
                    <Calendar size={24} />
                    <span>Grafik</span>
                </div>
                <div className="sidebar-item mt-auto">
                    <Settings size={24} />
                    <span>Ustawienia</span>
                </div>
            </nav>
        </aside>
    );
}

export default Sidebar;