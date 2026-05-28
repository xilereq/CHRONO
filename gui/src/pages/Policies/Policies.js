import React, { useState } from 'react';
import '../main.css';
import './policies.css';
import { PoliciesSidebar, PolicyFormTab, HolidayTabs, UserManagementTab, PolicyListTab, AssignPolicyTab } from "../../components";

const Policies = () => {
    const [activeTab, setActiveTab] = useState('policies');

    return (
        <div className="page-container">
            <div className="policies-page-wrapper">

                <PoliciesSidebar activeTab={activeTab} setActiveTab={setActiveTab} />

                <div className="policies-content">
                    {activeTab === 'policies' && <PolicyFormTab />}
                    {activeTab === 'policyList' && <PolicyListTab />}
                    {activeTab === 'assignPolicy' && <AssignPolicyTab />}
                    {activeTab === 'holidays' && <HolidayTabs />}
                    {activeTab === 'users' && <UserManagementTab />}
                </div>

            </div>
        </div>
    );
};

export default Policies;
