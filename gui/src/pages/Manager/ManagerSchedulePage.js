import React, { useState, useEffect } from 'react';
import ManagerSidebar from '../../components/manager/ManagerSidebar';
import ScheduleChangeTab from '../../components/manager/ScheduleChangeTab';
import ManagerRequestTab from '../../components/manager/ManagerRequestTab';
import DeputyTab from '../../components/manager/DeputyTab';
import ApprovalList from '../Requests/ApprovalList';
import { userService, delegationService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';

import '../main.css';
import '../Policies/policies.css';
import '../../components/manager/manager.css';

const ManagerSchedulePage = () => {
    const { t } = useLanguage();
    const [activeTab, setActiveTab] = useState('schedule');
    const [role, setRole] = useState(null);
    const [represented, setRepresented] = useState([]);
    const [ready, setReady] = useState(false);

    useEffect(() => {
        const init = async () => {
            let myRole = null;
            try {
                myRole = (await userService.getMyRole()).role;
            } catch {
                myRole = null;
            }
            setRole(myRole);
            if (myRole !== 'ROLE_MANAGER' && myRole !== 'ROLE_ADMIN') {
                setRepresented(await delegationService.getActiveReceived());
                setActiveTab('approvals');
            }
            setReady(true);
        };
        init();
    }, []);

    const isAdmin = role === 'ROLE_ADMIN';
    const isRealManager = role === 'ROLE_MANAGER' || isAdmin;

    return (
        <div className="page-container">
            <div className="policies-page-wrapper">

                <ManagerSidebar activeTab={activeTab} setActiveTab={setActiveTab} isActingDeputy={role !== null && !isRealManager} />

                <div className="policies-content">

                    <div className="policies-scrollable-content" style={{ marginTop: '1.5rem' }}>
                        {represented.length > 0 && (
                            <div className="deputy-banner" role="status">
                                {t('manager.actingDeputyBanner', 'Zastępujesz menedżera:')}{' '}
                                {represented.map(d => `${d.managerName} (${d.startDate} – ${d.endDate})`).join(', ')}
                            </div>
                        )}

                        {ready && activeTab === 'schedule' && <ScheduleChangeTab />}

                        {ready && activeTab === 'approvals' && <ApprovalList />}

                        {ready && activeTab === 'team-requests' && <ManagerRequestTab />}

                        {ready && activeTab === 'deputy' && isRealManager && <DeputyTab isAdmin={isAdmin} />}
                    </div>
                </div>

            </div>
        </div>
    );
};

export default ManagerSchedulePage;
