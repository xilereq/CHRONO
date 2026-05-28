import React, { useState, useEffect } from 'react';
import { userService, authService } from '../../services';

const UserManagementTab = () => {
    const [userFormData, setUserFormData] = useState({
        name: '',
        surname: '',
        email: '',
        roleName: "ROLE_EMPLOYEE",
        managerEmail: ''
    });

    const [managers, setManagers] = useState([]);
    const [userLoading, setUserLoading] = useState(false);
    const [userMessage, setUserMessage] = useState('');
    const [userError, setUserError] = useState('');

    const [users, setUsers] = useState([]);
    const [managerFormData, setManagerFormData] = useState({ email: '', managerEmail: '' });
    const [managerLoading, setManagerLoading] = useState(false);
    const [managerMessage, setManagerMessage] = useState('');
    const [managerError, setManagerError] = useState('');

    useEffect(() => {
        const fetchData = async () => {
            try {
                const allManagers = await userService.getManagers();
                const allUsers = await userService.getAllUsers();

                setManagers(allManagers || []);
                setUsers(allUsers || []);
            } catch (err) {
                console.error("Błąd podczas pobierania danych:", err);
                setManagerError("Nie udało się pobrać listy użytkowników lub managerów.");
            }
        };
        fetchData();
    }, []);

    const handleUserFormChange = (e) => {
        const { name, value } = e.target;
        setUserFormData(prev => ({
            ...prev,
            [name]: value,
            ...(name === 'roleName' && value !== 'ROLE_EMPLOYEE' ? { managerEmail: '' } : {})
        }));
    };

    const handleUserFormSubmit = async (e) => {
        e.preventDefault();
        setUserError('');
        setUserMessage('');
        setUserLoading(true);

        const payload = { ...userFormData };
        if (!payload.managerEmail) {
            payload.managerEmail = null;
        }

        console.log(payload);

        try {
            const response = await authService.register(payload);
            const generatedPassword = response.temporaryPassword || '[Brak danych o haśle]';

            setUserMessage(`Użytkownik ${payload.name} ${payload.surname} został pomyślnie dodany. Tymczasowe hasło: ${generatedPassword}`);

            const updatedUsers = await userService.getAllUsers();
            setUsers(updatedUsers || []);

            setUserFormData({ name: '', surname: '', email: '', roleName: 'ROLE_EMPLOYEE', managerEmail: '' });
        } catch (err) {
            setUserError(err.message || 'Wystąpił błąd podczas dodawania użytkownika.');
        } finally {
            setUserLoading(false);
        }
    };

    const handleManagerFormSubmit = async (e) => {
        e.preventDefault();
        setManagerError('');
        setManagerMessage('');

        if (!managerFormData.email || !managerFormData.managerEmail) {
            setManagerError("Wybierz pracownika i managera.");
            return;
        }

        setManagerLoading(true);
        try {
            await userService.setManager(managerFormData.email, managerFormData.managerEmail);
            setManagerMessage("Manager został pomyślnie przypisany.");
            setManagerFormData({ email: '', managerEmail: '' });

            const updatedUsers = await userService.getAllUsers();
            setUsers(updatedUsers || []);
        } catch (err) {
            setManagerError(err.message || "Błąd podczas przypisywania managera.");
        } finally {
            setManagerLoading(false);
        }
    };

    return (
        <>
            <h1 className="policies-header">Zarządzanie Użytkownikami</h1>

            <div className="policy-section">
                <h2 className="section-title">Sekcja 1: Dodaj nowego użytkownika</h2>
                <p className="section-subtitle mb-3">Zarejestruj nowe konto pracownicze w systemie.</p>

                {userError && (
                    <div style={{ background: '#fef2f2', color: '#991b1b', padding: '0.75rem', borderRadius: '6px', marginBottom: '1rem', border: '1px solid #fecaca' }}>
                        {userError}
                    </div>
                )}
                {userMessage && (
                    <div style={{ background: '#f0fdf4', color: '#16a34a', padding: '0.75rem', borderRadius: '6px', marginBottom: '1rem', border: '1px solid #bbf7d0' }}>
                        <strong>{userMessage}</strong>
                    </div>
                )}

                <div className="grid-2-col" style={{ marginTop: '15px' }}>
                    <div className="form-group">
                        <span className="form-label">Imię</span>
                        <input
                            className="form-input" type="text" name="name"
                            value={userFormData.name} onChange={handleUserFormChange} required
                        />
                    </div>
                    <div className="form-group">
                        <span className="form-label">Nazwisko</span>
                        <input
                            className="form-input" type="text" name="surname"
                            value={userFormData.surname} onChange={handleUserFormChange} required
                        />
                    </div>
                    <div className="form-group">
                        <span className="form-label">Adres e-mail</span>
                        <input
                            className="form-input" type="email" name="email"
                            value={userFormData.email} onChange={handleUserFormChange} required
                        />
                    </div>
                    <div className="form-group">
                        <span className="form-label">Rola systemowa</span>
                        <select
                            className="form-input" name="roleName"
                            value={userFormData.roleName} onChange={handleUserFormChange}
                        >
                            <option value="ROLE_EMPLOYEE">Pracownik</option>
                            <option value="ROLE_HR">HR</option>
                            <option value="ROLE_MANAGER">Kierownik</option>
                            <option value="ROLE_ADMIN">Administrator</option>
                        </select>
                    </div>

                    {userFormData.roleName === 'ROLE_EMPLOYEE' && (
                        <div className="form-group">
                            <span className="form-label">Przypisz managera</span>
                            <select
                                className="form-input" name="managerEmail"
                                value={userFormData.managerEmail} onChange={handleUserFormChange}
                            >
                                <option value="">-- Wybierz managera --</option>
                                {managers.map(manager => (
                                    <option key={manager.id} value={manager.email}>
                                        {manager.name} {manager.surname} ({manager.email})
                                    </option>
                                ))}
                            </select>
                        </div>
                    )}
                </div>

                <div className="policies-footer">
                    <div style={{ flex: 1 }}></div>
                    <div className="footer-actions">
                        <button className="btn-yellow" onClick={handleUserFormSubmit} disabled={userLoading}>
                            {userLoading ? 'Przetwarzanie...' : 'Zarejestruj użytkownika'}
                        </button>
                    </div>
                </div>
            </div>

            <div className="policy-section" style={{ marginTop: '2rem' }}>
                <h2 className="section-title">Sekcja 2: Przypisz Managera</h2>
                <p className="section-subtitle mb-3">Wybierz pracownika oraz managera, którego chcesz mu przypisać.</p>

                {managerError && (
                    <div style={{ background: '#fef2f2', color: '#991b1b', padding: '0.75rem', borderRadius: '6px', marginBottom: '1rem', border: '1px solid #fecaca' }}>
                        {managerError}
                    </div>
                )}
                {managerMessage && (
                    <div style={{ background: '#f0fdf4', color: '#16a34a', padding: '0.75rem', borderRadius: '6px', marginBottom: '1rem', border: '1px solid #bbf7d0' }}>
                        <strong>{managerMessage}</strong>
                    </div>
                )}

                <form className="grid-2-col" style={{ marginTop: '15px' }} onSubmit={handleManagerFormSubmit}>
                    <div className="form-group">
                        <span className="form-label">Pracownik</span>
                        <select
                            className="form-input"
                            value={managerFormData.email}
                            onChange={(e) => setManagerFormData({...managerFormData, email: e.target.value})}
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
                            value={managerFormData.managerEmail}
                            onChange={(e) => setManagerFormData({...managerFormData, managerEmail: e.target.value})}
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
                        <button type="submit" className="btn-yellow" disabled={managerLoading} style={{ width: '100%', display: 'flex', justifyContent: 'center' }}>
                            {managerLoading ? 'Przypisywanie...' : 'Zapisz przypisanie managera'}
                        </button>
                    </div>
                </form>
            </div>
        </>
    );
};

export default UserManagementTab;
