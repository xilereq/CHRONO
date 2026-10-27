import React, { useState, useEffect } from 'react';
import { Trash2, ChevronDown, ChevronUp, PlusCircle } from 'lucide-react';
import { policiesService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';

const PolicyListTab = () => {
    const { t } = useLanguage();
    const [policies, setPolicies] = useState([]);
    const [globalHolidays, setGlobalHolidays] = useState([]);
    const [expandedPolicyId, setExpandedPolicyId] = useState(null);
    const [policyHolidays, setPolicyHolidays] = useState([]);

    const [selectedHolidayIds, setSelectedHolidayIds] = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        try {
            const [policiesData, holidaysData] = await Promise.all([
                policiesService.getAllPolicies(),
                policiesService.getAllHolidays()
            ]);
            setPolicies(policiesData);
            setGlobalHolidays(holidaysData);
        } catch (error) {
            console.error(t('policies.fetchDataConsoleError', "Błąd ładowania danych:"), error);
        }
    };

    const handleDeletePolicy = async (id) => {
        if (!window.confirm(t('policies.deletePolicyConfirm', "Czy na pewno chcesz usunąć tę politykę?"))) return;
        try {
            await policiesService.deletePolicy(id);
            setPolicies(policies.filter(p => p.id !== id));
            if (expandedPolicyId === id) setExpandedPolicyId(null);
        } catch (error) {
            alert(t('policies.deletePolicyErrorAlert', "Błąd podczas usuwania polityki: ") + error.message);
        }
    };

    const toggleExpand = async (policyId) => {
        if (expandedPolicyId === policyId) {
            setExpandedPolicyId(null);
            setSelectedHolidayIds([]);
            return;
        }
        setExpandedPolicyId(policyId);
        setLoading(true);
        setSelectedHolidayIds([]);
        try {
            const holidays = await policiesService.getHolidaysForPolicy(policyId);
            setPolicyHolidays(holidays);
        } catch (error) {
            console.error(t('policies.fetchPolicyHolidaysError', "Błąd ładowania świąt polityki:"), error);
        } finally {
            setLoading(false);
        }
    };

    const handleHolidaySelectChange = (e) => {
        const selectedOptions = Array.from(e.target.selectedOptions, option => option.value);
        setSelectedHolidayIds(selectedOptions);
    };

    const handleAssignHoliday = async () => {
        if (selectedHolidayIds.length === 0) return;
        try {
            await policiesService.assignHolidaysToPolicyBulk(expandedPolicyId, {
                holidayIds: selectedHolidayIds.map(id => parseInt(id))
            });

            const updatedHolidays = await policiesService.getHolidaysForPolicy(expandedPolicyId);
            setPolicyHolidays(updatedHolidays);

            setSelectedHolidayIds([]);
        } catch (error) {
            alert(t('policies.assignHolidayErrorAlert', "Błąd przypisywania świąt: ") + error.message);
        }
    };

    const handleRemoveHoliday = async (holidayId) => {
        try {
            await policiesService.removeHolidayFromPolicy(expandedPolicyId, holidayId);
            setPolicyHolidays(policyHolidays.filter(h => h.id !== holidayId && h.holidayId !== holidayId));
        } catch (error) {
            alert(t('policies.removeHolidayErrorAlert', "Błąd usuwania święta: ") + error.message);
        }
    };

    return (
        <>
            <h1 className="policies-header">{t('policies.policyListTitle', 'Lista Polityk Systemowych')}</h1>

            <div className="policy-section">
                <h2 className="section-title mb-3">{t('policies.savedPoliciesSectionTitle', 'Sekcja 1: Zapisane polityki w systemie')}</h2>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {policies.length === 0 ? (
                        <div style={{ padding: '20px', textAlign: 'center', color: 'var(--text-muted)' }}>
                            {t('policies.noPoliciesText', 'Brak polityk. Dodaj nową w zakładce "Nowa Polityka".')}
                        </div>
                    ) : (
                        policies.map(policy => (
                            <div key={policy.id} style={{ border: '1px solid #eee', borderRadius: '8px', overflow: 'hidden', background: 'var(--bg-card)' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '15px', background: expandedPolicyId === policy.id ? '#f8f9fa' : 'transparent' }}>
                                    <div style={{ flex: 1, cursor: 'pointer' }} onClick={() => toggleExpand(policy.id)}>
                                        <strong style={{ fontSize: '15px', color: 'var(--text-main)' }}>{policy.name}</strong>
                                        <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '4px' }}>
                                            {t('policies.daysLabel', 'Dni:')} {policy.workingDays?.join(', ')} | {t('policies.hoursLabel', 'Godziny:')} {policy.dailyHours}h {t('policies.dailySuffix', 'dziennie,')} {policy.maxWeeklyHours}h {t('policies.maxSuffix', 'max')}
                                        </div>
                                    </div>
                                    <div style={{ display: 'flex', gap: '10px' }}>
                                        <button className="btn-outline-rounded" onClick={() => toggleExpand(policy.id)}>
                                            {expandedPolicyId === policy.id ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                                            {t('policies.holidaysBtn', 'Święta')}
                                        </button>
                                        <button className="btn-outline-rounded" style={{ color: 'var(--danger)', borderColor: 'var(--danger-border)', background: 'var(--danger-bg)' }} onClick={() => handleDeletePolicy(policy.id)}>
                                            <Trash2 size={16} />
                                        </button>
                                    </div>
                                </div>

                                {expandedPolicyId === policy.id && (
                                    <div style={{ padding: '15px', borderTop: '1px solid #eee', background: 'var(--bg-main)' }}>
                                        <h4 style={{ fontSize: '13px', margin: '0 0 10px 0', color: 'var(--text-main)' }}>{t('policies.assignedHolidaysLabel', 'Święta przypisane do tej polityki:')}</h4>
                                        {loading ? (
                                            <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{t('common.loading_data', 'Ładowanie...')}</div>
                                        ) : (
                                            <ul style={{ margin: '0 0 15px 0', paddingLeft: '20px', fontSize: '13px', color: 'var(--text-muted)' }}>
                                                {policyHolidays.length === 0 ? <li>{t('policies.noSpecificHolidays', 'Brak przypisanych świąt specyficznych.')}</li> :
                                                    policyHolidays.map(h => (
                                                        <li key={h.id || h.holidayId} style={{ marginBottom: '5px' }}>
                                                            {h.name} ({h.date})
                                                            <span style={{ color: 'var(--danger)', marginLeft: '10px', cursor: 'pointer', textDecoration: 'underline' }} onClick={() => handleRemoveHoliday(h.id || h.holidayId)}>
                                                                {t('policies.removeFromPolicyAction', 'Usuń z polityki')}
                                                            </span>
                                                        </li>
                                                    ))
                                                }
                                            </ul>
                                        )}

                                        <div style={{ display: 'flex', gap: '10px', alignItems: 'flex-start', marginTop: '1rem' }}>
                                            <div style={{ flex: 1 }}>
                                                <select
                                                    multiple
                                                    size="4"
                                                    className="form-input"
                                                    value={selectedHolidayIds}
                                                    onChange={handleHolidaySelectChange}
                                                    style={{ width: '100%', padding: '8px', minHeight: '100px' }}
                                                >
                                                    {globalHolidays.map(gh => (
                                                        <option key={gh.id || gh.holidayId} value={gh.id || gh.holidayId} style={{ padding: '4px' }}>
                                                            {gh.name} ({gh.date})
                                                        </option>
                                                    ))}
                                                </select>
                                                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px', display: 'block' }}>
                                                    {t('policies.selectedCount', 'Zaznaczono:')} {selectedHolidayIds.length} ({t('policies.multiSelectHoldHint', 'Przytrzymaj Ctrl/Cmd, aby zaznaczyć wiele')})
                                                </span>
                                            </div>

                                            <button
                                                className="btn-yellow"
                                                onClick={handleAssignHoliday}
                                                disabled={selectedHolidayIds.length === 0}
                                                style={{ padding: '0.6rem 1rem', fontSize: '13px', height: 'fit-content' }}
                                            >
                                                <PlusCircle size={14} style={{ marginRight: '5px' }} />
                                                {t('policies.assignBtnText', 'Przypisz')} ({selectedHolidayIds.length})
                                            </button>
                                        </div>
                                    </div>
                                )}
                            </div>
                        ))
                    )}
                </div>
            </div>
        </>
    );
};

export default PolicyListTab;
