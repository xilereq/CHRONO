import React from 'react';
import '../main.css';
import { RequestList, Button } from '../../components';

const statusClass = () => 'status-badge';

const MyRequests = () => {
    return (
        <div className="dashboard">
            <h2 className="dashboard-title">Moje wnioski</h2>
            <div className="requests-container">
                {/* Render RequestList with empty data to initialize helper files */}
                <RequestList
                    requests={[]}
                    statusClass={statusClass}
                    ButtonComponent={Button}
                />
            </div>
        </div>
    );
}

export default MyRequests;
