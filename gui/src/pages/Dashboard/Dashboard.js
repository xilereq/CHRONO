import React from 'react';

import {TimeTracker, StatusCharts, QuickRequests, RecentRequests} from '../../pages';

const Dashboard = () => {
    return (
        <div className="dashboard">
            <h2 className="dashboard-title">Ewidencja Czasu Pracy - Pulpit Pracownika</h2>
            <div className="dashboard-grid">
                <TimeTracker />
                <StatusCharts />
                <QuickRequests />
                <RecentRequests />
            </div>
        </div>
    );
}

export default Dashboard;
