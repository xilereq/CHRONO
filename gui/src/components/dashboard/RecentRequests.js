import React, { useState, useEffect } from 'react';
import { Card, Badge } from '../index';
import { applicationService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import '../../pages/main.css';

const RecentRequests = () => {
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const { t } = useLanguage();

    const translateType = (type) => {
        switch (type) {
            case 'HOLIDAY_LEAVE': return t('requests.types.holiday', 'Urlop wypoczynkowy');
            case 'SICK_LEAVE': return t('requests.types.sick', 'Zwolnienie lekarskie');
            case 'EARLY_EXIT': return t('requests.types.earlyExit', 'Wyjście wcześniejsze');
            case 'OVERTIME_EARN': return t('requests.types.overtimeEarn', 'Zgłoszenie nadgodzin');
            case 'OVERTIME_TAKE': return t('requests.types.overtimeTake', 'Odbiór nadgodzin');
            default: return type || t('common.other', 'Inny');
        }
    };

    const translateStatus = (status) => {
        switch (status) {
            case 'APPROVED': return t('requests.approved', 'Zatwierdzony');
            case 'PENDING': return t('requests.pending', 'Oczekujący');
            case 'REJECTED': return t('requests.rejected', 'Odrzucony');
            case 'CANCELLED': return t('requests.cancelled', 'Anulowany');
            default: return status;
        }
    };

    useEffect(() => {
        const fetchRequests = async () => {
            try {
                const data = await applicationService.getUserApplications();
                setRequests(data.slice(0, 4));
            } catch (error) {
                console.error(error);
            } finally {
                setLoading(false);
            }
        };

        fetchRequests();
    }, []);

    return (
        <Card title={t('dashboard.recentRequests', 'OSTATNIE WNIOSKI')} className="table-container">
            <table className="requests-table">
                <thead>
                <tr>
                    <th>{t('requests.requestType', 'Typ')}</th>
                    <th>{t('requests.submitDate', 'Data zgłoszenia')}</th>
                    <th>{t('requests.absenceDate', 'Data nieobecności')}</th>
                    <th>{t('requests.status', 'Status')}</th>
                </tr>
                </thead>
                <tbody>
                {loading ? (
                    <tr>
                        <td colSpan="4" style={{ textAlign: 'center', padding: '1rem' }}>{t('common.loading_data', 'Ładowanie danych...')}</td>
                    </tr>
                ) : requests.length === 0 ? (
                    <tr>
                        <td colSpan="4" style={{ textAlign: 'center', padding: '1rem' }}>{t('dashboard.noRecentRequests', 'Brak ostatnich wniosków.')}</td>
                    </tr>
                ) : (
                    requests.map((req) => (
                        <tr key={req.id}>
                            <td>{translateType(req.type)}</td>
                            <td>{req.creationDateTime ? new Date(req.creationDateTime).toLocaleDateString(t('common.locale', 'pl-PL')) : '-'}</td>
                            <td>{req.startDate} - {req.endDate}</td>
                            <td><Badge status={req.status} label={translateStatus(req.status)} /></td>
                        </tr>
                    ))
                )}
                </tbody>
            </table>
        </Card>
    );
}

export default RecentRequests;
