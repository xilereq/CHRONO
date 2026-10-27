import React, { useState, useEffect, useMemo } from 'react';
import { applicationService, userService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import '../../components/requests/requests.css';
import './ApprovalList.css';

const HOURLY_TYPES = ['EARLY_EXIT', 'OVERTIME_EARN', 'OVERTIME_TAKE'];

const ApprovalList = () => {
    const { t } = useLanguage();
    const [userRole, setUserRole]               = useState(null);
    const [pendingRequests, setPendingRequests] = useState([]);
    const [loading, setLoading]                 = useState(true);
    const [activeAction, setActiveAction]       = useState({ id: null, type: null });
    const [actionReason, setActionReason]       = useState('');
    const [processingId, setProcessingId]       = useState(null);
    const [selectedIds, setSelectedIds]         = useState(() => new Set());
    const [bulkConfirmOpen, setBulkConfirmOpen] = useState(false);
    const [bulkReason, setBulkReason]           = useState('');
    const [bulkProcessing, setBulkProcessing]   = useState(false);
    const [bulkResult, setBulkResult]           = useState(null);
    const [currentPage, setCurrentPage]         = useState(1);
    const [itemsPerPage, setItemsPerPage]       = useState(() => window.innerWidth <= 768 ? 5 : 10);

    const [filterType, setFilterType]               = useState('');
    const [filterStatus, setFilterStatus]           = useState('');
    const [filterDateFrom, setFilterDateFrom]       = useState('');
    const [filterDateTo, setFilterDateTo]           = useState('');
    const [filterEmployeeId, setFilterEmployeeId]   = useState('');

    useEffect(() => {
        const onResize = () => setItemsPerPage(window.innerWidth <= 768 ? 5 : 10);
        window.addEventListener('resize', onResize);
        return () => window.removeEventListener('resize', onResize);
    }, []);

    useEffect(() => {
        userService.getMyRole()
            .then(res => setUserRole(res.role === 'ROLE_ADMIN' ? 'ADMIN' : 'MANAGER'))
            .catch(() => setUserRole('MANAGER'));
    }, []);

    const loadRequests = async () => {
        if (!userRole) return;
        setLoading(true);
        try {
            const data = userRole === 'ADMIN'
                ? await applicationService.getAllApplications()
                : await applicationService.getPendingForManager();
            setPendingRequests(data || []);
        } catch (error) {
            console.error('Błąd podczas ładowania wniosków:', error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadRequests();
    }, [userRole]);

    const employees = useMemo(() => {
        const map = new Map();
        pendingRequests.forEach(r => {
            if (r.employeeId != null && !map.has(r.employeeId)) {
                map.set(r.employeeId, r.employeeEmail || String(r.employeeId));
            }
        });
        return [...map.entries()]
            .map(([id, email]) => ({ id, email }))
            .sort((a, b) => a.email.localeCompare(b.email));
    }, [pendingRequests]);

    const filteredRequests = useMemo(() => pendingRequests.filter(r => {
        if (filterType && r.type !== filterType) return false;
        if (filterStatus && r.status !== filterStatus) return false;
        if (filterEmployeeId && String(r.employeeId) !== filterEmployeeId) return false;
        if (filterDateFrom && r.startDate < filterDateFrom) return false;
        if (filterDateTo && r.startDate > filterDateTo) return false;
        return true;
    }), [pendingRequests, filterType, filterStatus, filterEmployeeId, filterDateFrom, filterDateTo]);

    const pendingFiltered = useMemo(
        () => filteredRequests.filter(r => r.status === 'PENDING'),
        [filteredRequests]
    );

    useEffect(() => {
        setCurrentPage(1);
        setActiveAction({ id: null, type: null });
    }, [filteredRequests, itemsPerPage]);

    useEffect(() => {
        const visiblePending = new Set(pendingFiltered.map(r => r.id));
        setSelectedIds(prev => {
            const next = new Set([...prev].filter(id => visiblePending.has(id)));
            return next.size === prev.size ? prev : next;
        });
    }, [pendingFiltered]);

    const initiateAction = (id, type) => {
        setActiveAction({ id, type });
        setActionReason('');
    };

    const submitAction = async () => {
        setProcessingId(activeAction.id);
        try {
            await applicationService.changeStatus(activeAction.id, activeAction.type, actionReason);
            setActiveAction({ id: null, type: null });
            setActionReason('');
            loadRequests();
        } catch {
            alert(t('approvals.statusError', 'Nie udało się zmienić statusu wniosku.'));
        } finally {
            setProcessingId(null);
        }
    };

    const submitBulk = async () => {
        const ids = [...selectedIds];
        if (ids.length === 0) return;

        const byId = new Map(pendingRequests.map(r => [r.id, r]));
        setBulkProcessing(true);
        setBulkResult(null);
        try {
            const result = await applicationService.approveMany(ids, bulkReason);
            const failed = result.failed.map(f => {
                const req = byId.get(f.id);
                const label = req
                    ? `${typeLabels[req.type] || req.type || '#' + f.id} · ${req.employeeEmail || '#' + f.id}`
                    : `#${f.id}`;
                return { ...f, label };
            });
            setBulkResult({ total: ids.length, approved: result.approved.length, failed });
            setSelectedIds(new Set());
            setBulkConfirmOpen(false);
            setBulkReason('');
            await loadRequests();
        } catch (error) {
            setBulkResult({ total: ids.length, approved: 0, failed: [], error: error.message });
        } finally {
            setBulkProcessing(false);
        }
    };

    const handlePageChange = (page) => {
        setCurrentPage(page);
        setActiveAction({ id: null, type: null });
    };

    const typeLabels = {
        HOLIDAY_LEAVE: t('requests.types.holiday', 'Urlop wypoczynkowy'),
        SICK_LEAVE:    t('requests.types.sick', 'Zwolnienie lekarskie'),
        EARLY_EXIT:    t('requests.types.earlyExit', 'Wcześniejsze wyjście'),
        OVERTIME_EARN: t('requests.types.overtimeEarn', 'Zgłoszenie nadgodzin'),
        OVERTIME_TAKE: t('requests.types.overtimeTake', 'Odbiór nadgodzin'),
        HOME_OFFICE:   t('reports.homeOffice', 'Praca zdalna (HO)'),
    };

    const statusLabels = {
        APPROVED:  t('requests.approved', 'Zatwierdzony'),
        PENDING:   t('requests.pending', 'Oczekujący'),
        REJECTED:  t('requests.rejected', 'Odrzucony'),
        CANCELLED: t('requests.cancelled', 'Anulowany'),
    };

    const getDuration = (request) => {
        if (HOURLY_TYPES.includes(request.type)) {
            if (!request.durationSeconds) return '-';
            return `${Math.round(request.durationSeconds / 3600)} ${t('manager.hoursCountLabel', 'godz.')}`;
        }
        if (!request.startDate || !request.endDate) return '-';
        const start = new Date(request.startDate);
        const end   = new Date(request.endDate);
        const days  = Math.round((end - start) / (1000 * 60 * 60 * 24)) + 1;
        return `${days} ${t('reports.daysUnit', 'dni')}`;
    };

    const totalPages = Math.ceil(filteredRequests.length / itemsPerPage);
    const pageItems  = filteredRequests.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
    );
    const canActOn  = (r) => r.status === 'PENDING';
    const colSpan   = userRole === 'ADMIN' ? 8 : 7;

    const selectedCount     = selectedIds.size;
    const pagePendingIds    = pageItems.filter(canActOn).map(r => r.id);
    const allPageSelected   = pagePendingIds.length > 0 && pagePendingIds.every(id => selectedIds.has(id));
    const somePageSelected  = pagePendingIds.some(id => selectedIds.has(id));

    const toggleOne = (id) => setSelectedIds(prev => {
        const next = new Set(prev);
        if (next.has(id)) next.delete(id); else next.add(id);
        return next;
    });

    const togglePage = () => setSelectedIds(prev => {
        const next = new Set(prev);
        pagePendingIds.forEach(id => (allPageSelected ? next.delete(id) : next.add(id)));
        return next;
    });

    const selectAllPending = () => setSelectedIds(new Set(pendingFiltered.map(r => r.id)));

    const clearSelection = () => {
        setSelectedIds(new Set());
        setBulkConfirmOpen(false);
        setBulkReason('');
    };

    if (!userRole || loading) {
        return <div className="table-empty-cell">{t('approvals.loadingApprovals', 'Ładowanie wniosków...')}</div>;
    }

    return (
        <div className="requests-container">

            <div className="section-panel filters-bar">
                <select className="filter-select" value={filterType} onChange={e => setFilterType(e.target.value)}>
                    <option value="">{t('requests.requestType', 'Typ')}</option>
                    <option value="HOLIDAY_LEAVE">{t('requests.types.holiday', 'Urlop wypoczynkowy')}</option>
                    <option value="SICK_LEAVE">{t('requests.types.sick', 'Zwolnienie lekarskie')}</option>
                    <option value="HOME_OFFICE">{t('reports.homeOffice', 'Praca zdalna (HO)')}</option>
                    <option value="EARLY_EXIT">{t('requests.types.earlyExit', 'Wcześniejsze wyjście')}</option>
                    <option value="OVERTIME_EARN">{t('requests.types.overtimeEarn', 'Zgłoszenie nadgodzin')}</option>
                    <option value="OVERTIME_TAKE">{t('requests.types.overtimeTake', 'Odbiór nadgodzin')}</option>
                </select>

                <select className="filter-select" value={filterStatus} onChange={e => setFilterStatus(e.target.value)}>
                    <option value="">{t('requests.status', 'Status')}</option>
                    <option value="PENDING">{t('requests.pending', 'Oczekujący')}</option>
                    <option value="APPROVED">{t('requests.approved', 'Zatwierdzony')}</option>
                    <option value="REJECTED">{t('requests.rejected', 'Odrzucony')}</option>
                    <option value="CANCELLED">{t('requests.cancelled', 'Anulowany')}</option>
                </select>

                <select className="filter-select" value={filterEmployeeId} onChange={e => setFilterEmployeeId(e.target.value)}>
                    <option value="">{t('approvals.employeeColumn', 'Pracownik')}</option>
                    {employees.map(emp => (
                        <option key={emp.id} value={String(emp.id)}>{emp.email}</option>
                    ))}
                </select>

                <div className="filters-date-row">
                    <input
                        type="date"
                        className="filter-input"
                        title="Od"
                        value={filterDateFrom}
                        onChange={e => {
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
                        onChange={e => setFilterDateTo(e.target.value)}
                    />
                </div>
            </div>

            <div className="section-panel">
                <div className="section-title">
                    {userRole === 'MANAGER' ? t('approvals.employeeRequestsTitle', 'Wnioski pracownicze') : t('approvals.systemRequestsTitle', 'Wnioski w systemie')}
                    {filteredRequests.length !== pendingRequests.length && (
                        <span style={{ fontWeight: 400, fontSize: '0.85rem', color: 'var(--text-muted)', marginLeft: '0.75rem' }}>
                            ({filteredRequests.length} z {pendingRequests.length})
                        </span>
                    )}
                </div>

                {bulkResult && (
                    <div
                        className={`bulk-result ${bulkResult.error ? 'bulk-result--error' : bulkResult.failed.length ? 'bulk-result--partial' : 'bulk-result--success'}`}
                        role="status"
                    >
                        <div className="bulk-result-header">
                            <strong>
                                {bulkResult.error
                                    ? t('approvals.bulkError', 'Nie udało się zatwierdzić wybranych wniosków.')
                                    : bulkResult.failed.length
                                        ? t('approvals.bulkPartial', 'Zatwierdzono {done} z {total} wniosków.')
                                            .replace('{done}', bulkResult.approved).replace('{total}', bulkResult.total)
                                        : t('approvals.bulkSuccess', 'Zatwierdzono wnioski: {done}.')
                                            .replace('{done}', bulkResult.approved)}
                            </strong>
                            <button
                                type="button"
                                className="bulk-result-close"
                                onClick={() => setBulkResult(null)}
                                aria-label={t('common.close', 'Zamknij')}
                            >
                                ×
                            </button>
                        </div>
                        {bulkResult.error && <div className="bulk-result-detail">{bulkResult.error}</div>}
                        {bulkResult.failed.length > 0 && (
                            <ul className="bulk-result-list">
                                {bulkResult.failed.map(f => (
                                    <li key={f.id}><span>{f.label}</span> — {f.message}</li>
                                ))}
                            </ul>
                        )}
                    </div>
                )}

                {selectedCount > 0 && (
                    <div className="bulk-bar" role="region" aria-label={t('approvals.bulkBarLabel', 'Akcje zbiorcze')}>
                        <span className="bulk-bar-count">
                            {t('approvals.selectedCount', 'Zaznaczono: {n}').replace('{n}', selectedCount)}
                        </span>
                        {selectedCount < pendingFiltered.length && (
                            <button type="button" className="bulk-link" onClick={selectAllPending} disabled={bulkProcessing}>
                                {t('approvals.selectAllPending', 'Zaznacz wszystkie oczekujące ({n})').replace('{n}', pendingFiltered.length)}
                            </button>
                        )}
                        <div className="bulk-bar-actions">
                            <button
                                type="button"
                                className="action-panel-btn-cancel"
                                onClick={clearSelection}
                                disabled={bulkProcessing}
                            >
                                {t('approvals.clearSelection', 'Odznacz')}
                            </button>
                            <button
                                type="button"
                                className="action-btn-approve bulk-approve-btn"
                                onClick={() => setBulkConfirmOpen(true)}
                                disabled={bulkProcessing || bulkConfirmOpen}
                            >
                                {t('approvals.approveSelected', 'Akceptuj wybrane')} ({selectedCount})
                            </button>
                        </div>
                    </div>
                )}

                {selectedCount > 0 && bulkConfirmOpen && (
                    <div className="bulk-confirm">
                        <span className="action-panel-label approved">
                            {t('approvals.bulkConfirmLabel', 'Zatwierdzić {n} wybranych wniosków? Uwagi (opcjonalne):').replace('{n}', selectedCount)}
                        </span>
                        <input
                            type="text"
                            className="field-input action-panel-input"
                            value={bulkReason}
                            onChange={(e) => setBulkReason(e.target.value)}
                            placeholder={t('approvals.inputPlaceholder', 'Wpisz tutaj...')}
                            disabled={bulkProcessing}
                            autoFocus
                        />
                        <button
                            type="button"
                            className="action-panel-btn-confirm approved"
                            onClick={submitBulk}
                            disabled={bulkProcessing}
                        >
                            {bulkProcessing ? t('common.saving', 'Zapisywanie...') : t('approvals.confirmBtn', 'Potwierdź')}
                        </button>
                        <button
                            type="button"
                            className="action-panel-btn-cancel"
                            onClick={() => setBulkConfirmOpen(false)}
                            disabled={bulkProcessing}
                        >
                            {t('common.cancel', 'Anuluj')}
                        </button>
                    </div>
                )}

                {filteredRequests.length === 0 ? (
                    <div className="table-empty-cell">{t('approvals.noApprovals', 'Brak wniosków spełniających kryteria.')}</div>
                ) : (
                    <div className="requests-table-container">
                        <table className="requests-table">
                            <thead>
                                <tr>
                                    <th className="col-select">
                                        <input
                                            type="checkbox"
                                            checked={allPageSelected}
                                            ref={el => { if (el) el.indeterminate = somePageSelected && !allPageSelected; }}
                                            onChange={togglePage}
                                            disabled={pagePendingIds.length === 0 || bulkProcessing}
                                            aria-label={t('approvals.selectAllOnPage', 'Zaznacz wszystkie oczekujące na tej stronie')}
                                        />
                                    </th>
                                    <th>{t('approvals.typeColumn', 'Typ wniosku')}</th>
                                    <th>{t('requests.periodColumn', 'Okres / Data')}</th>
                                    <th className="col-hide-mobile">{t('approvals.durationColumn', 'Czas trwania')}</th>
                                    <th className="col-hide-mobile">{t('approvals.reasonColumn', 'Powód')}</th>
                                    <th>{t('approvals.employeeColumn', 'Pracownik')}</th>
                                    {userRole === 'ADMIN' && <th>{t('approvals.statusColumn', 'Status')}</th>}
                                    <th>{t('approvals.actionsColumn', 'Akcje')}</th>
                                </tr>
                            </thead>
                            <tbody>
                                {pageItems.map(r => (
                                    <React.Fragment key={r.id}>
                                        <tr className={selectedIds.has(r.id) ? 'row-selected' : undefined}>
                                            <td className="col-select">
                                                {canActOn(r) && (
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedIds.has(r.id)}
                                                        onChange={() => toggleOne(r.id)}
                                                        disabled={bulkProcessing}
                                                        aria-label={`${t('approvals.selectRow', 'Zaznacz wniosek')}: ${r.employeeEmail || r.id}`}
                                                    />
                                                )}
                                            </td>
                                            <td className="request-card-title-cell">{typeLabels[r.type] || r.type || '-'}</td>
                                            <td>{r.startDate}{r.endDate && r.endDate !== r.startDate ? ` – ${r.endDate}` : ''}</td>
                                            <td className="col-hide-mobile">{getDuration(r)}</td>
                                            <td className="col-hide-mobile">{r.reason || '-'}</td>
                                            <td>{r.employeeEmail || '-'}</td>

                                            {userRole === 'ADMIN' && (
                                                <td>
                                                    <span className={`status-dot ${r.status}`} style={{ marginRight: '6px' }} />
                                                    {statusLabels[r.status] || r.status || '-'}
                                                </td>
                                            )}

                                            <td>
                                                {canActOn(r) ? (
                                                    <div className="submit-actions-row">
                                                        <button
                                                            className="action-btn action-btn-approve"
                                                            onClick={() => initiateAction(r.id, 'APPROVED')}
                                                        >
                                                            {t('approvals.approve', 'Zatwierdź')}
                                                        </button>
                                                        <button
                                                            className="action-btn action-btn-reject"
                                                            onClick={() => initiateAction(r.id, 'REJECTED')}
                                                        >
                                                            {t('approvals.reject', 'Odrzuć')}
                                                        </button>
                                                    </div>
                                                ) : (
                                                    <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>—</span>
                                                )}
                                            </td>
                                        </tr>

                                        {activeAction.id === r.id && (
                                            <tr>
                                                <td colSpan={colSpan} className={`action-panel-td ${activeAction.type === 'APPROVED' ? 'approved' : 'rejected'}`}>
                                                    <div className="action-panel-container">
                                                        <span className={`action-panel-label ${activeAction.type === 'APPROVED' ? 'approved' : 'rejected'}`}>
                                                            {activeAction.type === 'APPROVED'
                                                                ? t('approvals.approveNotesLabel', 'Uwagi do akceptacji (opcjonalne):')
                                                                : t('approvals.rejectNotesLabel', 'Powód odrzucenia (opcjonalne):')}
                                                        </span>
                                                        <input
                                                            type="text"
                                                            className="field-input action-panel-input"
                                                            value={actionReason}
                                                            onChange={(e) => setActionReason(e.target.value)}
                                                            placeholder={t('approvals.inputPlaceholder', 'Wpisz tutaj...')}
                                                            autoFocus
                                                        />
                                                        <button
                                                            className={`action-panel-btn-confirm ${activeAction.type === 'APPROVED' ? 'approved' : 'rejected'}`}
                                                            onClick={submitAction}
                                                            disabled={processingId === r.id}
                                                        >
                                                            {processingId === r.id ? t('common.saving', 'Zapisywanie...') : t('approvals.confirmBtn', 'Potwierdź')}
                                                        </button>
                                                        <button
                                                            className="action-panel-btn-cancel"
                                                            onClick={() => setActiveAction({ id: null, type: null })}
                                                            disabled={processingId === r.id}
                                                        >
                                                            {t('common.cancel', 'Anuluj')}
                                                        </button>
                                                    </div>
                                                </td>
                                            </tr>
                                        )}
                                    </React.Fragment>
                                ))}
                            </tbody>
                        </table>

                        {totalPages > 1 && (
                            <div className="pagination">
                                <span
                                    className={currentPage === 1 ? 'page-disabled' : 'page-btn'}
                                    onClick={() => currentPage > 1 && handlePageChange(currentPage - 1)}
                                >
                                    «
                                </span>
                                {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                                    <span
                                        key={page}
                                        className={page === currentPage ? 'page-active' : 'page-btn'}
                                        onClick={() => handlePageChange(page)}
                                    >
                                        {page}
                                    </span>
                                ))}
                                <span
                                    className={currentPage === totalPages ? 'page-disabled' : 'page-btn'}
                                    onClick={() => currentPage < totalPages && handlePageChange(currentPage + 1)}
                                >
                                    »
                                </span>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default ApprovalList;
