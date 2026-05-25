import React, { useState, useEffect } from 'react';
import { Trash2 } from 'lucide-react';
import policiesService from "../../services/policiesService";

const HolidayTabs = () => {
    const [globalHolidays, setGlobalHolidays] = useState([]);
    const [newHoliday, setNewHoliday] = useState({ name: '', date: '', occursYearly: true });
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        loadGlobalHolidays();
    }, []);

    const loadGlobalHolidays = async () => {
        try {
            const data = await policiesService.getAllGlobalHolidays();
            setGlobalHolidays(data);
        } catch (err) {
            console.error("Błąd pobierania świąt:", err, err.message, err.type);
        }
    };

    const handleSaveHoliday = async () => {
        if (!newHoliday.name || !newHoliday.date) {
            alert("Nazwa oraz data są wymagane!");
            return;
        }

        setIsSaving(true);
        try {
            await policiesService.createGlobalHoliday(newHoliday);
            alert("Święto zostało dodane!");
            setNewHoliday({ name: '', date: '', occursYearly: true });
            await loadGlobalHolidays();
        } catch (err) {
            alert("Wystąpił błąd: " + err.message);
        } finally {
            setIsSaving(false);
        }
    };

    const handleDeleteHoliday = async (id) => {
        if (!window.confirm("Na pewno chcesz usunąć ten dzień wolny?")) return;
        try {
            await policiesService.deleteGlobalHoliday(id);
            await loadGlobalHolidays();
        } catch (err) {
            alert("Błąd usuwania: " + err.message);
        }
    };

    return (
        <>
            <h1 className="policies-header">Zarządzanie Świętami (Holidays)</h1>

            <div className="policy-section">
                <h2 className="section-title">Sekcja 1: Dodaj nowy dzień wolny</h2>
                <p className="section-subtitle mb-3">Dni ustawowo wolne dla całej organizacji.</p>

                <div className="grid-3-col" style={{ marginTop: '20px' }}>
                    <label className="form-group">
                        <span className="form-label">Nazwa święta</span>
                        <input
                            type="text" className="form-input" placeholder="np. Boże Narodzenie"
                            value={newHoliday.name} onChange={(e) => setNewHoliday({...newHoliday, name: e.target.value})}
                        />
                    </label>
                    <label className="form-group">
                        <span className="form-label">Data</span>
                        <input
                            type="date" className="form-input"
                            value={newHoliday.date} onChange={(e) => setNewHoliday({...newHoliday, date: e.target.value})}
                        />
                    </label>
                    <label className="form-group">
                        <span >Czy coroczne?</span>
                        <input
                            type="checkbox"
                            className="form-input"
                            checked={newHoliday.occursYearly}
                            onChange={(e) => setNewHoliday({...newHoliday, occursYearly: e.target.checked})}
                        />
                    </label>
                </div>
            </div>

            <div className="policy-section">
                <h2 className="section-title mb-3">Sekcja 2: Lista świąt w systemie</h2>

                <div style={{ border: '1px solid #eee', borderRadius: '8px', overflow: 'hidden' }}>
                    {globalHolidays.length === 0 ? (
                        <div style={{ padding: '20px', textAlign: 'center', color: '#888' }}>
                            Brak dodanych świąt.
                        </div>
                    ) : (
                        globalHolidays.map(holiday => (
                            <div key={holiday.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '15px', borderBottom: '1px solid #f5f5f5', alignItems: 'center' }}>
                                <div>
                                    <strong style={{ display: 'block', fontSize: '15px' }}>{holiday.name}</strong>
                                    <span style={{ color: '#666', fontSize: '13px' }}>{holiday.date}</span>
                                </div>
                                <button
                                    className="btn-outline-rounded"
                                    style={{ color: 'red', borderColor: '#ffcdd2', backgroundColor: '#ffebee', padding: '5px 10px' }}
                                    onClick={() => handleDeleteHoliday(holiday.id)}
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
                        {isSaving ? 'Zapisywanie...' : 'Zapisz święto do bazy'}
                    </button>
                </div>
            </div>
        </>
    );
};

export default HolidayTabs;
