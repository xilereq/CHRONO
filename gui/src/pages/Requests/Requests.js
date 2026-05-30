import React, { useEffect, useState } from 'react';
import { RequestList } from '../../components';
import ApprovalList from './ApprovalList';
import { userService, applicationService } from '../../services';
import '../main.css';

const Requests = () => {

    // sekcja zmienne lokalne
    const [isAdmin, setIsAdmin] = useState(false);
    const [isManager, setIsManager] = useState(false);
    const [isLoading, setIsLoading] = useState(true);

    // sekcja automatycznego wykrywania uprawnień
    useEffect(() => {
        const checkPermissions = async () => {
            try {
                const roleResponse = await userService.getMyRole();

                if (roleResponse.role === "ROLE_ADMIN") {
                    setIsAdmin(true);
                    setIsLoading(false);
                    return;
                }

                try {
                    await applicationService.getPendingForManager();
                    setIsManager(true);
                } catch (managerError) {
                    setIsManager(false);
                }

            } catch (error) {
                console.error("Błąd podczas sprawdzania uprawnień:", error);
            } finally {
                setIsLoading(false);
            }
        };

        checkPermissions();
    }, []);

    // sekcja wyglądu widoku
    return (
        <div className="page-container">
            <h2 className="page-title-no-margin">Moje wnioski</h2>

            {isLoading ? (
                <div style={{ padding: '20px' }}>Ładowanie uprawnień...</div>
            ) : (
                <div>
                    <RequestList />
                    {isAdmin && <ApprovalList userRole="ADMIN" />}
                    {!isAdmin && isManager && <ApprovalList userRole="MANAGER" />}
                </div>
            )}
        </div>
    );
};

export default Requests;
