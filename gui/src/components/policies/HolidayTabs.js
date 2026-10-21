import React, { useState, useEffect } from 'react';
import { Trash2, Download } from 'lucide-react';
import policiesService from "../../services/policiesService";
import { useLanguage } from '../../context/LanguageContext';

const HolidayTabs = () => {
    const { t } = useLanguage();
    const [globalHolidays, setGlobalHolidays] = useState([]);
    const [newHoliday, setNewHoliday] = useState({ name: '', date: '' });
    const [isSaving, setIsSaving] = useState(false);

    // ---- import z zewnętrznego API ----
    const [importYear, setImportYear] = useState(new Date().getFullYear());
    const [importCountry, setImportCountry] = useState('PL');
    const [importRows, setImportRows] = useState([]);
    const [policies, setPolicies] = useState([]);
    const [selectedPolicyIds, setSelectedPolicyIds] = useState([]);
    const [isFetchingExternal, setIsFetchingExternal] = useState(false);
    const [isImporting, setIsImporting] = useState(false);
    const [importError, setImportError] = useState('');

    useEffect(() => {
        loadHolidays();
        policiesService.getAllPolicies()
            .then(data => setPolicies(Array.isArray(data) ? data : []))
            .catch(err => console.error(t('policies.fetchPoliciesConsoleError', "Błąd ładowania polityk:"), err));
    }, []);

    const loadHolidays = async () => {
        try {
            const data = await policiesService.getAllHolidays();
            setGlobalHolidays(data);
        } catch (err) {
            console.error(t('policies.fetchHolidaysConsoleError', "Błąd pobierania świąt:"), err, err.message, err.type);
        }
    };

    const handleSaveHoliday = async () => {
        if (!newHoliday.name || !newHoliday.date) {
            alert(t('policies.nameAndDateRequiredAlert', "Nazwa oraz data są wymagane!"));
            return;
        }

        setIsSaving(true);
        try {
            await policiesService.createHoliday(newHoliday);
            alert(t('policies.holidayAddedAlert', "Święto zostało dodane!"));
            setNewHoliday({ name: '', date: '' });
            await loadHolidays();
        } catch (err) {
            alert(t('common.errorOccurred', "Wystąpił błąd: ") + err.message);
        } finally {
            setIsSaving(false);
        }
    };

    const handleFetchExternal = async () => {
        setImportError('');
        const year = parseInt(importYear, 10);
        const code = importCountry.trim().toUpperCase();

        if (!year || year < 1976 || year > 2076) {
            setImportError(t('policies.importYearError', 'Podaj rok z zakresu 1976–2076.'));
            return;
        }
        if (!/^[A-Z]{2}$/.test(code)) {
            setImportError(t('policies.importCountryError', 'Podaj 2-literowy kod kraju ISO, np. PL.'));
            return;
        }

        setIsFetchingExternal(true);
        try {
            const data = await policiesService.previewExternalHolidays(year, code);
            setImportRows((data || []).map((h, i) => ({
                key: `${h.date}-${i}`,
                name: h.name,
                originalName: h.originalName,
                date: h.date,
                alreadyExists: !!h.alreadyExists,
                selected: !h.alreadyExists
            })));
        } catch (err) {
            setImportRows([]);
            setImportError(err.message);
        } finally {
            setIsFetchingExternal(false);
        }
    };

    const updateImportRow = (key, patch) => {
        setImportRows(rows => rows.map(r => (r.key === key ? { ...r, ...patch } : r)));
    };

    const toggleAllImportRows = (checked) => {
        setImportRows(rows => rows.map(r => ({ ...r, selected: checked })));
    };

    const togglePolicy = (id) => {
        setSelectedPolicyIds(ids => (ids.includes(id) ? ids.filter(x => x !== id) : [...ids, id]));
    };

    const selectedRows = importRows.filter(r => r.selected);

    const handleImportSave = async () => {
        setImportError('');
        if (selectedRows.length === 0) {
            setImportError(t('policies.importNoneSelected', 'Zaznacz co najmniej jedno święto.'));
            return;
        }
        if (selectedRows.some(r => !r.name.trim() || !r.date)) {
            setImportError(t('policies.nameAndDateRequiredAlert', "Nazwa oraz data są wymagane!"));
            return;
        }

        setIsImporting(true);
        try {
            const payload = {
                holidays: selectedRows.map(r => ({ name: r.name.trim(), date: r.date })),
                ...(selectedPolicyIds.length > 0 ? { policyIds: selectedPolicyIds } : {})
            };
            const saved = await policiesService.createHolidaysBatch(payload);
            const count = Array.isArray(saved) ? saved.length : selectedRows.length;
            alert(`${t('policies.importSuccess', 'Zapisano święta:')} ${count}` +
                (selectedPolicyIds.length > 0
                    ? ` ${t('policies.importAssignedTo', '— przypisano do polityk:')} ${selectedPolicyIds.length}`
                    : ''));
            setImportRows([]);
            await loadHolidays();
        } catch (err) {
            setImportError(err.message);
        } finally {
            setIsImporting(false);
        }
    };

    const handleDeleteHoliday = async (holidayId) => {
        if (!window.confirm(t('policies.deleteHolidayConfirm', "Na pewno chcesz usunąć ten dzień wolny?"))) return;
        try {
            await policiesService.deleteHoliday(holidayId);
            await loadHolidays();
        } catch (err) {
            alert(t('policies.deleteErrorAlert', "Błąd usuwania: ") + err.message);
        }
    };

    return (
        <>
            <h1 className="policies-header">{t('policies.holidayManagementTitle', 'Zarządzanie Świętami (Holidays)')}</h1>

            <div className="policy-section">
                <h2 className="section-title">{t('policies.addHolidaySectionTitle', 'Sekcja 1: Dodaj nowy dzień wolny')}</h2>
                <p className="section-subtitle mb-3">{t('policies.addHolidaySubtitle', 'Dni ustawowo wolne dla całej organizacji.')}</p>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginTop: '20px' }}>
                    <label className="form-group">
                        <span className="form-label">{t('policies.holidayNameLabel', 'Nazwa święta')}</span>
                        <input
                            type="text" className="form-input" placeholder={t('policies.holidayNamePlaceholder', 'np. Boże Narodzenie')}
                            value={newHoliday.name} onChange={(e) => setNewHoliday({...newHoliday, name: e.target.value})}
                        />
                    </label>
                    <label className="form-group">
                        <span className="form-label">{t('policies.dateLabel', 'Data')}</span>
                        <input
                            type="date" className="form-input"
                            value={newHoliday.date} onChange={(e) => setNewHoliday({...newHoliday, date: e.target.value})}
                        />
                    </label>
                </div>
            </div>

            <div className="policy-section">
                <h2 className="section-title">{t('policies.importSectionTitle', 'Import świąt z zewnętrznego serwisu')}</h2>
                <p className="section-subtitle mb-3">
                    {t('policies.importSectionSubtitle', 'Pobierz święta dla wybranego roku i kraju, popraw nazwy lub daty, a następnie zapisz je w bazie i opcjonalnie przypisz do polityk.')}
                </p>

                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '16px', alignItems: 'flex-end', marginTop: '20px' }}>
                    <label className="form-group" style={{ width: '140px' }}>
                        <span className="form-label">{t('policies.importYearLabel', 'Rok')}</span>
                        <input
                            type="number" className="form-input" min="1976" max="2076"
                            value={importYear} onChange={(e) => setImportYear(e.target.value)}
                        />
                    </label>
                    <label className="form-group" style={{ width: '180px' }}>
                        <span className="form-label">{t('policies.importCountryLabel', 'Kod kraju (ISO, 2 znaki)')}</span>
                        <input
                            type="text" className="form-input" maxLength={2} list="holiday-country-codes"
                            value={importCountry}
                            onChange={(e) => setImportCountry(e.target.value.toUpperCase())}
                            placeholder="PL"
                        />
                        <datalist id="holiday-country-codes">
                            {['PL', 'DE', 'CZ', 'SK', 'GB', 'US', 'FR', 'ES', 'IT', 'NL', 'UA', 'LT'].map(c => <option key={c} value={c} />)}
                        </datalist>
                    </label>
                    <button
                        type="button" className="btn-yellow" onClick={handleFetchExternal}
                        disabled={isFetchingExternal}
                        style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                    >
                        <Download size={16} />
                        {isFetchingExternal ? t('policies.importFetching', 'Pobieranie...') : t('policies.importFetchBtn', 'Pobierz święta')}
                    </button>
                </div>

                {importError && (
                    <div className="login-error" style={{ marginTop: '16px' }}>{importError}</div>
                )}

                {importRows.length > 0 && (
                    <>
                        <div style={{ marginTop: '20px', border: '1px solid #eee', borderRadius: '8px', overflow: 'hidden' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '10px 15px', background: '#fafafa', borderBottom: '1px solid #eee', fontSize: '13px' }}>
                                <input
                                    type="checkbox"
                                    checked={importRows.every(r => r.selected)}
                                    onChange={(e) => toggleAllImportRows(e.target.checked)}
                                />
                                <span>{t('policies.importSelectAll', 'Zaznacz wszystkie')} ({selectedRows.length}/{importRows.length})</span>
                            </div>
                            {importRows.map(row => (
                                <div key={row.key} style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '12px', padding: '10px 15px', borderBottom: '1px solid #f5f5f5', opacity: row.selected ? 1 : 0.55 }}>
                                    <input
                                        type="checkbox" checked={row.selected}
                                        onChange={(e) => updateImportRow(row.key, { selected: e.target.checked })}
                                    />
                                    <input
                                        type="text" className="form-input" style={{ flex: '1 1 220px', minWidth: 0 }}
                                        value={row.name} title={row.originalName}
                                        onChange={(e) => updateImportRow(row.key, { name: e.target.value })}
                                    />
                                    <input
                                        type="date" className="form-input" style={{ flex: '0 0 160px' }}
                                        value={row.date}
                                        onChange={(e) => updateImportRow(row.key, { date: e.target.value })}
                                    />
                                    {row.alreadyExists && (
                                        <span style={{ fontSize: '12px', color: '#92400e', background: '#fde68a', borderRadius: '999px', padding: '2px 10px' }}>
                                            {t('policies.importAlreadyExists', 'Już w bazie')}
                                        </span>
                                    )}
                                </div>
                            ))}
                        </div>

                        <div style={{ marginTop: '20px' }}>
                            <span className="form-label">{t('policies.importPoliciesLabel', 'Przypisz do polityk (opcjonalnie)')}</span>
                            {policies.length === 0 ? (
                                <div style={{ color: '#888', fontSize: '13px', marginTop: '8px' }}>
                                    {t('policies.importNoPolicies', 'Brak polityk – święta zostaną tylko zapisane w bazie.')}
                                </div>
                            ) : (
                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px 20px', marginTop: '8px' }}>
                                    {policies.map(p => (
                                        <label key={p.id} style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}>
                                            <input
                                                type="checkbox"
                                                checked={selectedPolicyIds.includes(p.id)}
                                                onChange={() => togglePolicy(p.id)}
                                            />
                                            {p.name}
                                        </label>
                                    ))}
                                </div>
                            )}
                        </div>

                        <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end' }}>
                            <button className="btn-yellow" onClick={handleImportSave} disabled={isImporting}>
                                {isImporting
                                    ? t('common.saving', 'Zapisywanie...')
                                    : `${t('policies.importSaveBtn', 'Zapisz zaznaczone')} (${selectedRows.length})`}
                            </button>
                        </div>
                    </>
                )}
            </div>

            <div className="policy-section">
                <h2 className="section-title mb-3">{t('policies.holidayListSectionTitle', 'Sekcja 2: Lista świąt w systemie')}</h2>

                <div style={{ border: '1px solid #eee', borderRadius: '8px', overflow: 'hidden' }}>
                    {globalHolidays.length === 0 ? (
                        <div style={{ padding: '20px', textAlign: 'center', color: '#888' }}>
                            {t('policies.noHolidaysText', 'Brak dodanych świąt.')}
                        </div>
                    ) : (
                        globalHolidays.map(holiday => (
                            <div key={holiday.holidayId} style={{ display: 'flex', justifyContent: 'space-between', padding: '15px', borderBottom: '1px solid #f5f5f5', alignItems: 'center' }}>
                                <div>
                                    <strong style={{ display: 'block', fontSize: '15px' }}>{holiday.name}</strong>
                                    <span style={{ color: '#666', fontSize: '13px' }}>{holiday.date}</span>
                                </div>
                                <button
                                    className="btn-outline-rounded"
                                    style={{ color: 'red', borderColor: '#ffcdd2', backgroundColor: '#ffebee', padding: '5px 10px' }}
                                    onClick={() => handleDeleteHoliday(holiday.holidayId)}
                                >
                                    <Trash2 size={16} />
                                </button>
                            </div>
                        ))
                    )}
                </div>
            </div>

            <div className="policies-footer">
                <div style={{ flex: 1 }}></div>
                <div className="footer-actions">
                    <button className="btn-yellow" onClick={handleSaveHoliday} disabled={isSaving}>
                        {isSaving ? t('common.saving', 'Zapisywanie...') : t('policies.saveHolidayBtn', 'Zapisz święto do bazy')}
                    </button>
                </div>
            </div>
        </>
    );
};

export default HolidayTabs;
