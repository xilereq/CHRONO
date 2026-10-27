import React, { useState } from 'react';
import ManagerSidebar from '../../components/manager/ManagerSidebar';
import ScheduleChangeTab from '../../components/manager/ScheduleChangeTab';
import ManagerRequestTab from '../../components/manager/ManagerRequestTab';
import DeputyTab from '../../components/manager/DeputyTab';
import ApprovalList from '../Requests/ApprovalList';

import '../main.css';
import '../Policies/policies.css';

const ManagerSchedulePage = () => {
    const [activeTab, setActiveTab] = useState('schedule');

    return (
        <div className="page-container">
            <div className="policies-page-wrapper">

                <ManagerSidebar activeTab={activeTab} setActiveTab={setActiveTab} />

                <div className="policies-content">

                    <div className="policies-scrollable-content" style={{ marginTop: '1.5rem' }}>
                        {activeTab === 'schedule' && <ScheduleChangeTab />}

                        {activeTab === 'approvals' && <ApprovalList />}

                        {activeTab === 'team-requests' && <ManagerRequestTab />}

                        {activeTab === 'deputy' && <DeputyTab />}
                    </div>
                </div>

            </div>
        </div>
    );
};

export default ManagerSchedulePage;
