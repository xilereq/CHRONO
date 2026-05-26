import React, { useState, useEffect } from 'react';
import { Trash2, ChevronDown, ChevronUp, PlusCircle } from 'lucide-react';
import { policiesService } from '../../services';

const PolicyListTab = () => {
    const [policies, setPolicies] = useState([]);
    const [globalHolidays, setGlobalHolidays] = useState([]);
    const [expandedPolicyId, setExpandedPolicyId] = useState(null);
    const [policyHolidays, setPolicyHolidays] = useState([]);
    const [selectedHolidayId, setSelectedHolidayId] = useState('');
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
            console.error("Błąd ładowania danych:", error);
        }
    };

    const handleDeletePolicy = async (id) => {
        if (!window.confirm("Czy na pewno chcesz usunąć tę politykę?")) return;
        try {
            await policiesService.deletePolicy(id);
            setPolicies(policies.filter(p => p.id !== id));
            if (expandedPolicyId === id) setExpandedPolicyId(null);
        } catch (error) {
            alert("Błąd podczas usuwania polityki: " + error.message);
        }
    };

    const toggleExpand = async (policyId) => {
        if (expandedPolicyId === policyId) {
            setExpandedPolicyId(null);
            return;
        }
        setExpandedPolicyId(policyId);
        setLoading(true);
        try {
            const holidays = await policiesService.getHolidaysForPolicy(policyId);
            setPolicyHolidays(holidays);
        } catch (error) {
            console.error("Błąd ładowania świąt polityki:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleAssignHoliday = async () => {
        if (!selectedHolidayId) return;
        try {
            await policiesService.assignHolidayToPolicy(expandedPolicyId, { holidayId: selectedHolidayId });
            const updatedHolidays = await policiesService.getHolidaysForPolicy(expandedPolicyId);
            setPolicyHolidays(updatedHolidays);
            setSelectedHolidayId('');
        } catch (error) {
            alert("Błąd przypisywania święta: " + error.message);
        }
    };

    const handleRemoveHoliday = async (holidayId) => {
        try {
            await policiesService.removeHolidayFromPolicy(expandedPolicyId, holidayId);
            setPolicyHolidays(policyHolidays.filter(h => h.id !== holidayId && h.holidayId !== holidayId));
        } catch (error) {
            alert("Błąd usuwania święta: " + error.message);
        }
    };

    return (
        <>
            <h1 className="policies-header">Lista Polityk Systemowych</h1>

            <div className="policy-section">
                <h2 className="section-title mb-3">Sekcja 1: Zapisane polityki w systemie</h2>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {policies.length === 0 ? (
                        <div style={{ padding: '20px', textAlign: 'center', color: '#888' }}>
                            Brak polityk. Dodaj nową w zakładce "Nowa Polityka".
                        </div>
                    ) : (
                        policies.map(policy => (
                            <div key={policy.id} style={{ border: '1px solid #eee', borderRadius: '8px', overflow: 'hidden', background: '#fff' }}>
                                {/* Główny pasek polityki */}
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '15px', background: expandedPolicyId === policy.id ? '#f8f9fa' : 'transparent' }}>
                                    <div style={{ flex: 1, cursor: 'pointer' }} onClick={() => toggleExpand(policy.id)}>
                                        <strong style={{ fontSize: '15px', color: 'var(--text-main)' }}>{policy.name}</strong>
                                        <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '4px' }}>
                                            Dni: {policy.workingDays?.join(', ')} | Godziny: {policy.dailyHours}h dziennie, {policy.maxWeeklyHours}h max
                                        </div>
                                    </div>
                                    <div style={{ display: 'flex', gap: '10px' }}>
                                        <button className="btn-outline-rounded" onClick={() => toggleExpand(policy.id)}>
                                            {expandedPolicyId === policy.id ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                                            Święta
                                        </button>
                                        <button className="btn-outline-rounded" style={{ color: '#ef4444', borderColor: '#fecaca', background: '#fef2f2' }} onClick={() => handleDeletePolicy(policy.id)}>
                                            <Trash2 size={16} />
                                        </button>
                                    </div>
                                </div>

                                {expandedPolicyId === policy.id && (
                                    <div style={{ padding: '15px', borderTop: '1px solid #eee', background: '#fafafa' }}>
                                        <h4 style={{ fontSize: '13px', margin: '0 0 10px 0', color: 'var(--text-main)' }}>Święta przypisane do tej polityki:</h4>
                                        {loading ? (
                                            <div style={{ fontSize: '12px', color: '#666' }}>Ładowanie...</div>
                                        ) : (
                                            <ul style={{ margin: '0 0 15px 0', paddingLeft: '20px', fontSize: '13px', color: '#555' }}>
                                                {policyHolidays.length === 0 ? <li>Brak przypisanych świąt specyficznych.</li> :
                                                    policyHolidays.map(h => (
                                                        <li key={h.id || h.holidayId} style={{ marginBottom: '5px' }}>
                                                            {h.name} ({h.date})
                                                            <span style={{ color: '#ef4444', marginLeft: '10px', cursor: 'pointer', textDecoration: 'underline' }} onClick={() => handleRemoveHoliday(h.id || h.holidayId)}>
                                                                Usuń z polityki
                                                            </span>
                                                        </li>
                                                    ))
                                                }
                                            </ul>
                                        )}

                                        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                                            <select className="form-input" value={selectedHolidayId} onChange={(e) => setSelectedHolidayId(e.target.value)} style={{ flex: 1 }}>
                                                <option value="">-- Wybierz święto z globalnej listy --</option>
                                                {globalHolidays.map(gh => (
                                                    <option key={gh.id || gh.holidayId} value={gh.id || gh.holidayId}>{gh.name} ({gh.date})</option>
                                                ))}
                                            </select>
                                            <button className="btn-yellow" onClick={handleAssignHoliday} style={{ padding: '0.4rem 1rem', fontSize: '13px' }}>
                                                <PlusCircle size={14} style={{ marginRight: '5px' }} /> Przypisz
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
