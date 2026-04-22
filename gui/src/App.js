import React from 'react';
import { Sidebar, Topbar } from './components';
import Dashboard from './pages/Dashboard';
import './index.css';

const App = () => {
    return (
        <div className="app-container">
            <Sidebar />
            <div className="app-content">
                <Topbar />
                <Dashboard />
            </div>
        </div>
    );
}

export default App;