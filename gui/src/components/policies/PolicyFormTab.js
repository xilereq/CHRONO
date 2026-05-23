import React, { useState } from 'react';
import { RotateCcw } from 'lucide-react';
import policiesService from "../../services/policiesService";

const DAYS_OF_WEEK = [
    { value: 'MONDAY', label: 'Poniedziałek' },
    { value: 'TUESDAY', label: 'Wtorek' },
    { value: 'WEDNESDAY', label: 'Środa' },
    { value: 'THURSDAY', label: 'Czwartek' },
    { value: 'FRIDAY', label: 'Piątek' },
    { value: 'SATURDAY', label: 'Sobota' },
    { value: 'SUNDAY', label: 'Niedziela' }
];

const PolicyFormTab = () => {
    const [isSaving, setIsSaving] = useState(false);
    const [policyData, setPolicyData] = useState({
        name: '',
        dailyHours: 8,
        maxDailyHours: 12,
        maxWeeklyHours: 40,
        workingDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
    });

    const handleWorkingDayToggle = (dayValue) => {
        setPolicyData(prev => {
            const isSelected = prev.workingDays.includes(dayValue);
            if (isSelected) {
                return { ...prev, workingDays: prev.workingDays.filter(d => d !== dayValue) };
            } else {
                return { ...prev, workingDays: [...prev.workingDays, dayValue] };
            }
        });
    };

    const handleSavePolicy = async () => {
        if (!policyData.name) {
            alert("Nazwa polityki jest wymagana!");
            return;
        }

        setIsSaving(true);
        try {
            await policiesService.create(policyData);
            alert("Polityka została pomyślnie utworzona i zapisana w bazie!");
            setPolicyData({
                name: '', dailyHours: 8, maxDailyHours: 12, maxWeeklyHours: 40,
                workingDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
            });
        } catch (err) {
            alert("Wystąpił błąd podczas zapisu: " + err.message);
        } finally {
            setIsSaving(false);
        }
    };

    const handleReset = () => {
        setPolicyData({
            name: '', dailyHours: 8, maxDailyHours: 12, maxWeeklyHours: 40,
            workingDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
        });
    };

    return (
        <>
            <h1 className="policies-header">Nowa Polityka Pracownicza</h1>

            <div className="policy-section">
                <h2 className="section-title">Sekcja 1: Identyfikacja</h2>
                <div className="hours-config" style={{ marginTop: '15px' }}>
                    <div className="form-group">
                        <span className="form-label">Nazwa polityki</span>
                        <input
                            type="text" className="form-input" placeholder="np. Zmiana poranna (8h)"
                            value={policyData.name} onChange={(e) => setPolicyData({...policyData, name: e.target.value})}
                        />
                    </div>
                </div>
            </div>

            <div className="policy-section">
                <h2 className="section-title">Sekcja 2: Wymiar dzienny</h2>
                <div className="hours-config" style={{ marginTop: '15px' }}>
                    <div className="form-row">
                        <div className="inline-input-group">
                            <span>Godziny bazowe:</span>
                            <input
                                type="number" className="form-input short-input"
                                value={policyData.dailyHours} onChange={(e) => setPolicyData({...policyData, dailyHours: parseInt(e.target.value) || 0})}
                            />
                        </div>
                        <div className="inline-input-group" style={{ marginLeft: '20px' }}>
                            <span>Max. dziennie (z nadg.):</span>
                            <input
                                type="number" className="form-input short-input"
                                value={policyData.maxDailyHours} onChange={(e) => setPolicyData({...policyData, maxDailyHours: parseInt(e.target.value) || 0})}
                            />
                        </div>
                    </div>
                </div>
            </div>

            <div className="policy-section">
                <h2 className="section-title">Sekcja 3: Wymiar tygodniowy</h2>
                <div className="hours-config" style={{ marginTop: '15px' }}>
                    <div className="form-row">
                        <div className="inline-input-group">
                            <span>Max. tygodniowo:</span>
                            <input
                                type="number" className="form-input short-input"
                                value={policyData.maxWeeklyHours} onChange={(e) => setPolicyData({...policyData, maxWeeklyHours: parseInt(e.target.value) || 0})}
                            />
                        </div>
                    </div>
                </div>
            </div>

            <div className="policy-section">
                <h2 className="section-title mb-3">Sekcja 4: Dni robocze</h2>
                <div style={{ display: 'flex', gap: '10px', marginTop: '15px', flexWrap: 'wrap' }}>
                    {DAYS_OF_WEEK.map(day => {
                        const isActive = policyData.workingDays.includes(day.value);
                        return (
                            <button
                                key={day.value} type="button" onClick={() => handleWorkingDayToggle(day.value)}
                                className="btn-outline-rounded"
                                style={{
                                    borderColor: isActive ? '#007bff' : '#ccc',
                                    backgroundColor: isActive ? '#007bff' : 'transparent',
                                    color: isActive ? '#fff' : '#333'
                                }}
                            >
                                {day.label}
                            </button>
                        );
                    })}
                </div>
            </div>

            <div className="policies-footer">
                <button className="btn-text history-btn" onClick={handleReset}>
                    <RotateCcw size={16} /> Wyczyść formularz
                </button>
                <div className="footer-actions">
                    <button className="btn-yellow" onClick={handleSavePolicy} disabled={isSaving}>
                        {isSaving ? 'Zapisywanie...' : 'Zapisz nową politykę'}
                    </button>
                </div>
            </div>
        </>
    );
};

export default PolicyFormTab;
