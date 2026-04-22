import React from 'react';
import '../features/dashboard.css';
import { RequestList } from '../components';

const statusClass = () => 'status-badge';

const MyRequests = () => {
    return (
        <div className="dashboard">
            <h2 className="dashboard-title">Moje wnioski</h2>
            <div className="requests-container">
                {/* Render RequestList with empty data to initialize helper files */}
                <RequestList requests={[]} statusClass={statusClass} />
            </div>
        </div>
    );
}

export default MyRequests;
