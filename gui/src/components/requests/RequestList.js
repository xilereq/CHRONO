import React, { useState, useEffect } from 'react';
import { RequestCard, RequestForm, Button } from '../../components';
import { applicationService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import './requests.css';

const RequestList = () => {
    const { t } = useLanguage();
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);

    const [filterStatus, setFilterStatus] = useState("");
    const [filterType, setFilterType] = useState("");
    const [filterDateFrom, setFilterDateFrom] = useState("");
    const [filterDateTo, setFilterDateTo] = useState("");

    const [currentPage, setCurrentPage] = useState(1);
    const [itemsPerPage, setItemsPerPage] = useState(() => window.innerWidth <= 768 ? 5 : 10);

    useEffect(() => {
        const onResize = () => setItemsPerPage(window.innerWidth <= 768 ? 5 : 10);
        window.addEventListener('resize', onResize);
        return () => window.removeEventListener('resize', onResize);
    }, []);

    useEffect(() => {
        setCurrentPage(1);
    }, [filterStatus, filterType, filterDateFrom, filterDateTo, itemsPerPage]);


    const fetchRequests = async () => {
        setLoading(true);
        try {
            const data = await applicationService.getUserApplications();
            setRequests(data);
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchRequests();
    }, []);

    const filteredRequests = requests.filter(r => {
        const matchStatus = filterStatus === "" || r.status === filterStatus;
        const matchType = filterType === "" || r.type === filterType;

        let matchDate = true;
        if (filterDateFrom) matchDate = r.startDate >= filterDateFrom;
        if (filterDateTo) matchDate = matchDate && r.startDate <= filterDateTo;

        return matchStatus && matchType && matchDate;
    });

    return (
        <div className="requests-container">
            <div className="section-panel filters-bar">
                <select
                    className="filter-select"
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                >
                    <option value="">{t('requests.status', 'Status')}</option>
                    <option value="APPROVED">{t('requests.approved', 'Zatwierdzony')}</option>
                    <option value="PENDING">{t('requests.pending', 'Oczekujący')}</option>
                    <option value="REJECTED">{t('requests.rejected', 'Odrzucony')}</option>
                    <option value="CANCELLED">{t('requests.cancelled', 'Anulowany')}</option>
                </select>

                <select
                    className="filter-select"
                    value={filterType}
                    onChange={(e) => setFilterType(e.target.value)}
                >
                    <option value="">{t('requests.requestType', 'Typ')}</option>
                    <option value="HOLIDAY_LEAVE">{t('requests.types.holiday', 'Urlop wypoczynkowy')}</option>
                    <option value="SICK_LEAVE">{t('requests.types.sick', 'Zwolnienie lekarskie')}</option>
                    <option value="HOME_OFFICE">{t('reports.homeOffice', 'Praca zdalna (HO)')}</option>
                    <option value="EARLY_EXIT">{t('requests.types.earlyExit', 'Wcześniejsze wyjście')}</option>
                    <option value="OVERTIME_EARN">{t('requests.types.overtimeEarn', 'Zgłoszenie nadgodzin')}</option>
                    <option value="OVERTIME_TAKE">{t('requests.types.overtimeTake', 'Odbiór nadgodzin')}</option>
                </select>

                <div className="filters-date-row">
                    <input
                        type="date"
                        className="filter-input"
                        title="Od"
                        value={filterDateFrom}
                        onChange={(e) => {
                            setFilterDateFrom(e.target.value);
                            if (filterDateTo && e.target.value > filterDateTo) setFilterDateTo('');
                        }}
                    />
                    <input
                        type="date"
                        className="filter-input"
                        title="Do"
                        value={filterDateTo}
                        min={filterDateFrom || undefined}
                        onChange={(e) => setFilterDateTo(e.target.value)}
                    />
                </div>
            </div>

            <div className="section-panel">
                <div className="section-title">{t('requests.myRequestsList', 'Lista moich wniosków')}</div>
                {loading ? (
                    <div className="empty">{t('requests.loadingList', 'Ładowanie danych z serwera...')}</div>
                ) : !filteredRequests || filteredRequests.length === 0 ? (
                    <div className="empty">{t('requests.noFilteredRequests', 'Brak wniosków spełniających kryteria.')}</div>
                ) : (
                    <div className="requests-table-container">
                        <table className="requests-table">
                            <thead>
                            <tr>
                                <th>{t('requests.typeColumn', 'Typ Wniosku')}</th>
                                <th>{t('requests.periodColumn', 'Okres / Data')}</th>
                                <th className="col-hide-mobile">{t('requests.durationColumn', 'Liczba Dni/Godz.')}</th>
                                <th className="col-hide-mobile">{t('requests.submitDateColumn', 'Data Złożenia')}</th>
                                <th>{t('requests.status', 'Status')}</th>
                                <th>{t('requests.approverColumn', 'Zatwierdzający')}</th>
                                <th>{t('requests.actionsColumn', 'Akcje')}</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredRequests
                                .slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage)
                                .map(r => (
                                    <RequestCard key={r.id} request={r} />
                                ))}
                            </tbody>
                        </table>

                        {Math.ceil(filteredRequests.length / itemsPerPage) > 1 && (
                            <div className="pagination">
                                <span
                                    className={currentPage === 1 ? 'page-disabled' : 'page-btn'}
                                    onClick={() => currentPage > 1 && setCurrentPage(p => p - 1)}
                                >
                                    «
                                </span>
                                {Array.from(
                                    { length: Math.ceil(filteredRequests.length / itemsPerPage) },
                                    (_, i) => i + 1
                                ).map(page => (
                                    <span
                                        key={page}
                                        className={page === currentPage ? 'page-active' : 'page-btn'}
                                        onClick={() => setCurrentPage(page)}
                                    >
                                        {page}
                                    </span>
                                ))}
                                <span
                                    className={currentPage === Math.ceil(filteredRequests.length / itemsPerPage) ? 'page-disabled' : 'page-btn'}
                                    onClick={() => currentPage < Math.ceil(filteredRequests.length / itemsPerPage) && setCurrentPage(p => p + 1)}
                                >
                                    »
                                </span>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {showForm ? (
                <RequestForm
                    onCancel={() => setShowForm(false)}
                    onSuccess={() => {
                        setShowForm(false);
                        fetchRequests();
                    }}
                />
            ) : (
                <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.75rem' }}>
                    <Button
                        className="big-yellow-btn"
                        variant="primary"
                        onClick={() => setShowForm(true)}
                    >
                        {t('requests.newRequestBtn', 'Utwórz wniosek')}
                    </Button>
                </div>
            )}
        </div>
    );
};

export default RequestList;
