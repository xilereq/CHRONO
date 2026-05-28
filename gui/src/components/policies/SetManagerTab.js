import React, { useState, useEffect } from 'react';
import { userService } from '../../services';

const SetManagerTab = () => {
    const [users, setUsers] = useState([]);
    const [managers, setManagers] = useState([]);
    const [formData, setFormData] = useState({ email: '', managerEmail: '' });
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchData = async () => {
            try {
                const allUsers = await userService.getAllUsers();
                const allManagers = await userService.getManagers();

                setUsers(allUsers || []);
                setManagers(allManagers || []);
            } catch (err) {
                console.error("Błąd podczas pobierania danych:", err);
                setError("Nie udało się pobrać listy użytkowników lub managerów.");
            }
        };
        fetchData();
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setMessage('');

        if (!formData.email || !formData.managerEmail) {
            setError("Wybierz pracownika i managera.");
            return;
        }

        setLoading(true);
        try {
            await userService.setManager(formData.email, formData.managerEmail);
            setMessage("Manager został pomyślnie przypisany.");
            setFormData({ email: '', managerEmail: '' });
        } catch (err) {
            setError(err.message || "Błąd podczas przypisywania managera.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <h1 className="policies-header">Przypisz Managera</h1>

            <div className="policy-section">
                <h2 className="section-title">Sekcja 1: Wybór powiązania</h2>
                <p className="section-subtitle mb-3">Wybierz pracownika oraz managera, którego chcesz mu przypisać.</p>

                {error && (
                    <div style={{ background: '#fef2f2', color: '#991b1b', padding: '0.75rem', borderRadius: '6px', marginBottom: '1rem', border: '1px solid #fecaca' }}>
                        {error}
                    </div>
                )}
                {message && (
                    <div style={{ background: '#f0fdf4', color: '#16a34a', padding: '0.75rem', borderRadius: '6px', marginBottom: '1rem', border: '1px solid #bbf7d0' }}>
                        <strong>{message}</strong>
                    </div>
                )}

                <form className="grid-2-col" style={{ marginTop: '15px' }} onSubmit={handleSubmit}>
                    <div className="form-group">
                        <span className="form-label">Pracownik</span>
                        <select
                            className="form-input"
                            value={formData.email}
                            onChange={(e) => setFormData({...formData, email: e.target.value})}
                            required
                        >
                            <option value="">-- Wybierz pracownika --</option>
                            {users.map(user => (
                                <option key={user.userId || user.email} value={user.email}>
                                    {user.name} {user.surname} ({user.email})
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group">
                        <span className="form-label">Manager</span>
                        <select
                            className="form-input"
                            value={formData.managerEmail}
                            onChange={(e) => setFormData({...formData, managerEmail: e.target.value})}
                            required
                        >
                            <option value="">-- Wybierz managera --</option>
                            {managers.map(manager => (
                                <option key={manager.email} value={manager.email}>
                                    {manager.name} {manager.surname} ({manager.email})
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="form-group" style={{ gridColumn: '1 / -1', marginTop: '1rem' }}>
                        <button type="submit" className="btn-yellow" disabled={loading} style={{ width: '100%', display: 'flex', justifyContent: 'center' }}>
                            {loading ? 'Przypisywanie...' : 'Zapisz przypisanie managera'}
                        </button>
                    </div>
                </form>
            </div>
        </>
    );
};

export default SetManagerTab;
