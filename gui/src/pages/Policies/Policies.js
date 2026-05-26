import React, { useState } from 'react';
import '../main.css';
import './policies.css';
import { PoliciesSidebar, PolicyFormTab, HolidayTabs, UserFormTab, PolicyListTab, AssignPolicyTab } from "../../components";

const Policies = () => {
    // Stan zarządzający tym, która zakładka jest obecnie otwarta
    const [activeTab, setActiveTab] = useState('policies');

    return (
        <div className="page-container">
            <div className="policies-page-wrapper">

                {/* Wstrzykiwanie komponentu bocznego menu */}
                <PoliciesSidebar activeTab={activeTab} setActiveTab={setActiveTab} />

                {/* Prawy panel treści renderujący odpowiednią zakładkę */}
                <div className="policies-content">
                    {activeTab === 'policies' && <PolicyFormTab />}
                    {activeTab === 'policyList' && <PolicyListTab />}
                    {activeTab === 'assignPolicy' && <AssignPolicyTab />}
                    {activeTab === 'holidays' && <HolidayTabs />}
                    {activeTab === 'users' && <UserFormTab />}
                </div>

            </div>
        </div>
    );
};

export default Policies;
