import React, { useState, useEffect } from 'react';
import '../main.css';
import { RequestList } from '../../components';
import { applicationService } from '../../services/applicationService';

const MyRequests = () => {
    const [applications, setApplications] = useState([]);
    const [loading, setLoading] = useState(true);
    const userId = 1;

    useEffect(() => {
        const loadData = async () => {
            try {
                const data = await applicationService.getUserApplications(userId);
                setApplications(data);
            } catch (err) {
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        loadData();
    }, [userId]);

    return (
        <div className="dashboard">
            <h2 className="dashboard-title">Moje wnioski</h2>
            <div className="requests-container">
                {loading ? (
                    <p>Ładowanie wniosków...</p>
                ) : (
                    <RequestList requests={applications} />
                )}
            </div>
        </div>
    );
};

export default MyRequests;
