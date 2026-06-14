import React, { useState } from 'react';
import { RotateCcw } from 'lucide-react';
import policiesService from "../../services/policiesService";
import { useLanguage } from '../../context/LanguageContext';

const PolicyFormTab = () => {
    const { t } = useLanguage();
    const [isSaving, setIsSaving] = useState(false);
    const [policyData, setPolicyData] = useState({
        name: '',
        dailyHours: 8,
        maxDailyHours: 12,
        maxWeeklyHours: 40,
        roundingMinutes: 15,
        workingDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
    });

    const DAYS_OF_WEEK = [
        { value: 'MONDAY', label: t('common.days.monday', 'Poniedziałek') },
        { value: 'TUESDAY', label: t('common.days.tuesday', 'Wtorek') },
        { value: 'WEDNESDAY', label: t('common.days.wednesday', 'Środa') },
        { value: 'THURSDAY', label: t('common.days.thursday', 'Czwartek') },
        { value: 'FRIDAY', label: t('common.days.friday', 'Piątek') },
        { value: 'SATURDAY', label: t('common.days.saturday', 'Sobota') },
        { value: 'SUNDAY', label: t('common.days.sunday', 'Niedziela') }
    ];

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
            alert(t('policies.policyNameRequiredAlert', "Nazwa polityki jest wymagana!"));
            return;
        }

        setIsSaving(true);
        try {
            await policiesService.createPolicy(policyData);
            alert(t('policies.policyCreatedSuccess', "Polityka została pomyślnie utworzona i zapisana w bazie!"));
            setPolicyData({
                name: '', dailyHours: 8, maxDailyHours: 12, maxWeeklyHours: 40, roundingMinutes: 15,
                workingDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
            });
        } catch (err) {
            alert(t('policies.saveErrorAlert', "Wystąpił błąd podczas zapisu: ") + err.message);
        } finally {
            setIsSaving(false);
        }
    };

    const handleReset = () => {
        setPolicyData({
            name: '', dailyHours: 8, maxDailyHours: 12, maxWeeklyHours: 40, roundingMinutes: 15,
            workingDays: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
        });
    };

    return (
        <>
            <h1 className="policies-header">{t('policies.newPolicyTitle', 'Nowa Polityka Pracownicza')}</h1>

            <div className="policy-section">
                <h2 className="section-title">{t('policies.idSectionTitle', 'Sekcja 1: Identyfikacja')}</h2>
                <div className="hours-config" style={{ marginTop: '15px' }}>
                    <div className="form-group">
                        <span className="form-label">{t('policies.policyNameLabel', 'Nazwa polityki')}</span>
                        <input
                            type="text" className="form-input" placeholder={t('policies.policyNamePlaceholder', 'np. Zmiana poranna (8h)')}
                            value={policyData.name} onChange={(e) => setPolicyData({...policyData, name: e.target.value})}
                        />
                    </div>
                </div>
            </div>

            <div className="policy-section">
                <h2 className="section-title">{t('policies.dailyHoursSectionTitle', 'Sekcja 2: Wymiar dzienny')}</h2>
                <div className="hours-config" style={{ marginTop: '15px' }}>
                    <div className="form-row">
                        <div className="inline-input-group">
                            <span>{t('policies.baseHoursLabel', 'Godziny bazowe:')}</span>
                            <input
                                type="number" className="form-input short-input"
                                value={policyData.dailyHours} onChange={(e) => setPolicyData({...policyData, dailyHours: parseInt(e.target.value) || 0})}
                            />
                        </div>
                        <div className="inline-input-group" style={{ marginLeft: '20px' }}>
                            <span>{t('policies.maxDailyHoursLabel', 'Max. dziennie (z nadg.):')}</span>
                            <input
                                type="number" className="form-input short-input"
                                value={policyData.maxDailyHours} onChange={(e) => setPolicyData({...policyData, maxDailyHours: parseInt(e.target.value) || 0})}
                            />
                        </div>
                    </div>
                </div>
            </div>

            <div className="policy-section">
                <h2 className="section-title">{t('policies.weeklyHoursSectionTitle', 'Sekcja 3: Wymiar tygodniowy')}</h2>
                <div className="hours-config" style={{ marginTop: '15px' }}>
                    <div className="form-row">
                        <div className="inline-input-group">
                            <span>{t('policies.maxWeeklyHoursLabel', 'Max. tygodniowo:')}</span>
                            <input
                                type="number" className="form-input short-input"
                                value={policyData.maxWeeklyHours} onChange={(e) => setPolicyData({...policyData, maxWeeklyHours: parseInt(e.target.value) || 0})}
                            />
                        </div>
                    </div>
                </div>
            </div>

            <div className="policy-section">
                <h2 className="section-title mb-3">{t('policies.workingDaysSectionTitle', 'Sekcja 4: Dni robocze')}</h2>
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

            <div className="policy-section">
                <h2 className="section-title">{t('policies.roundingSectionTitle', 'Sekcja 5: Zaokrąglanie czasu pracy')}</h2>
                <div className="hours-config" style={{ marginTop: '15px' }}>
                    <div className="form-row">
                        <div className="inline-input-group">
                            <span>{t('policies.roundingMinutesLabel', 'Zaokrąglanie (minuty):')}</span>
                            <input
                                type="number" className="form-input short-input"
                                value={policyData.roundingMinutes} onChange={(e) => setPolicyData({...policyData, roundingMinutes: parseInt(e.target.value) || 0})}
                            />
                        </div>
                    </div>
                </div>
            </div>

            <div className="policies-footer">
                <button className="btn-text history-btn" onClick={handleReset}>
                    <RotateCcw size={16} /> {t('policies.clearFormBtn', 'Wyczyść formularz')}
                </button>
                <div className="footer-actions">
                    <button className="btn-yellow" onClick={handleSavePolicy} disabled={isSaving}>
                        {isSaving ? t('common.saving', 'Zapisywanie...') : t('policies.savePolicyBtn', 'Zapisz nową politykę')}
                    </button>
                </div>
            </div>
        </>
    );
};

export default PolicyFormTab;
