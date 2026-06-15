import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { RequestList } from '../../components';
import RequestForm from '../../components/requests/RequestForm';
import { useLanguage } from '../../context/LanguageContext';
import '../main.css';

const Requests = () => {
    const { t } = useLanguage();
    const location = useLocation();
    const [isFormOpen, setIsFormOpen] = useState(false);
    const [prefillData, setPrefillData] = useState(null);

    useEffect(() => {
        if (location.state && location.state.openForm) {
            setIsFormOpen(true);
            if (location.state.prefillType) {
                setPrefillData({ type: location.state.prefillType });
            }
            window.history.replaceState({}, document.title);
        }
    }, [location]);

    const handleFormSuccess = () => {
        setIsFormOpen(false);
        setPrefillData(null);
        window.location.reload();
    };

    return (
        <div className="page-container">
            <h2 className="page-title-no-margin">{t('requests.myRequests', 'Moje wnioski')}</h2>

            {isFormOpen && (
                <div style={{ marginBottom: '2rem', animation: 'fadeIn 0.3s' }}>
                    <RequestForm
                        initialData={prefillData}
                        onCancel={() => {
                            setIsFormOpen(false);
                            setPrefillData(null);
                        }}
                        onSuccess={handleFormSuccess}
                    />
                </div>
            )}

            <RequestList />
        </div>
    );
};

export default Requests;
