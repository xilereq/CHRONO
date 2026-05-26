import React, {useEffect, useState} from 'react';
import { RequestList } from '../../components';
import { ApprovalList } from '../../pages';
import '../main.css'
import {userService} from "../../services";

const Requests = () => {
    const [isAdminOrManager, setIsAdminOrManager] = useState(false);

    useEffect(() => {
        const fetchRole = async () => {
            try {
                const response = await userService.getMyRole();
                if (response.role === "ROLE_ADMIN" || response.role === "ROLE_MANAGER") {
                    setIsAdminOrManager(true);
                }
            } catch (error) {
                console.error("Nie udało się pobrać roli", error);
            }
        };

        fetchRole();
    }, []);

    return (
        <div className="page-container">
            <h2 className="page-title-no-margin">Moje wnioski</h2>
            <div>
                <RequestList />

                <ApprovalList />
            </div>
        </div>
    );
};

export default Requests;
