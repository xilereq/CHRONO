import React, { useState, useEffect } from 'react';
import { userService, authService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import '../main.css';
import '../Policies/policies.css';

const UserManagementPage = () => {
    const { t } = useLanguage();

    const [userFormData, setUserFormData] = useState({
        name: '', surname: '', email: '', roleName: "ROLE_EMPLOYEE", managerEmail: '', date: ''
    });
    const [userLoading, setUserLoading] = useState(false);
    const [userMessage, setUserMessage] = useState('');
    const [userError, setUserError] = useState('');

    const [managerFormData, setManagerFormData] = useState({ email: '', managerEmail: '' });
    const [managerLoading, setManagerLoading] = useState(false);
    const [managerMessage, setManagerMessage] = useState('');
    const [managerError, setManagerError] = useState('');

    const [employmentFormData, setEmploymentFormData] = useState({ email: '', startDate: '', endDate: '', active: '' });
    const [employmentLoading, setEmploymentLoading] = useState(false);
    const [employmentMessage, setEmploymentMessage] = useState('');
    const [employmentError, setEmploymentError] = useState('');

    const [managers, setManagers] = useState([]);
    const [users, setUsers] = useState([]);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const allManagers = await userService.getManagers();
                const allUsers = await userService.getAllUsers();
                setManagers(allManagers || []);
                setUsers(allUsers || []);
            } catch (err) {
                console.error(t('userManagement.fetchDataError', "Błąd podczas pobierania danych:"), err);
            }
        };
        fetchData();
    }, [t]);

    const handleUserFormChange = (e) => {
        const { name, value } = e.target;
        setUserFormData(prev => ({
            ...prev,
            [name]: value,
            ...(name === 'roleName' && !['ROLE_EMPLOYEE', 'ROLE_MANAGER'].includes(value) ? { managerEmail: '' } : {})
        }));
    };

    const handleUserFormSubmit = async (e) => {
        e.preventDefault();
        setUserError(''); setUserMessage(''); setUserLoading(true);

        const payload = {
            ...userFormData,
            managerEmail: userFormData.managerEmail || null
        };

        try {
            const response = await authService.register(payload);
            const generatedPassword = response.temporaryPassword || t('userManagement.noPasswordData', '[Brak danych o haśle]');

            setUserMessage(`${t('userManagement.userAddedPart1', 'Użytkownik')} ${payload.name} ${payload.surname} ${t('userManagement.userAddedPart2', 'został pomyślnie dodany. Tymczasowe hasło:')} ${generatedPassword}`);

            const updatedUsers = await userService.getAllUsers();
            const updatedManagers = await userService.getManagers();

            setUsers(updatedUsers || []);
            setManagers(updatedManagers || []);

            setUserFormData({ name: '', surname: '', email: '', roleName: 'ROLE_EMPLOYEE', managerEmail: '', date: '' });
        } catch (err) {
            setUserError(err.message || t('userManagement.addUserError', 'Wystąpił błąd podczas dodawania użytkownika.'));
        } finally {
            setUserLoading(false);
        }
    };

    const handleManagerFormSubmit = async (e) => {
        e.preventDefault();
        setManagerError(''); setManagerMessage('');

        if (!managerFormData.email || !managerFormData.managerEmail) {
            setManagerError(t('userManagement.selectEmpAndManagerError', "Wybierz pracownika i managera."));
            return;
        }
        if (managerFormData.email === managerFormData.managerEmail) {
            setManagerError(t('userManagement.selfAssignError', "Użytkownik nie może być swoim własnym managerem."));
            return;
        }

        setManagerLoading(true);
        try {
            await userService.setManager(managerFormData.email, managerFormData.managerEmail);
            setManagerMessage(t('userManagement.managerAssignedSuccess', "Manager został pomyślnie przypisany."));
            setManagerFormData({ email: '', managerEmail: '' });
            const updatedUsers = await userService.getAllUsers();
            setUsers(updatedUsers || []);
        } catch (err) {
            setManagerError(err.message || t('userManagement.assignManagerError', "Błąd podczas przypisywania managera."));
        } finally {
            setManagerLoading(false);
        }
    };

    const handleEmploymentFormSubmit = async (e) => {
        e.preventDefault();
        setEmploymentError(''); setEmploymentMessage('');

        if (!employmentFormData.email) {
            setEmploymentError(t('userManagement.selectEmployeeError', "Wybierz pracownika."));
            return;
        }

        setEmploymentLoading(true);
        try {
            let updated = false;

            if (employmentFormData.startDate) {
                await userService.setStartDate(employmentFormData.email, employmentFormData.startDate);
                updated = true;
            }
            if (employmentFormData.endDate) {
                await userService.setEndDate(employmentFormData.email, employmentFormData.endDate);
                updated = true;
            }
            if (employmentFormData.active !== '') {
                const isActive = employmentFormData.active === 'true';
                await userService.setActive(employmentFormData.email, isActive);
                updated = true;
            }

            if (updated) {
                setEmploymentMessage(t('userManagement.employmentUpdatedSuccess', "Dane zatrudnienia i status konta zostały zaktualizowane."));
                setEmploymentFormData({ email: '', startDate: '', endDate: '', active: '' });
            } else {
                setEmploymentError(t('userManagement.noDataProvided', "Wypełnij przynajmniej jedno pole, aby zapisać zmiany."));
            }

        } catch (err) {
            setEmploymentError(err.message || t('userManagement.employmentUpdateError', "Wystąpił błąd podczas aktualizacji danych."));
        } finally {
            setEmploymentLoading(false);
        }
    };

    return (
        <div className="page-container">
            <h2 className="page-title-no-margin">{t('userManagement.pageTitle', 'Zarządzanie Użytkownikami (HR)')}</h2>

            <div className="policy-section" style={{ marginTop: '1.5rem' }}>
                <h2 className="section-title">{t('userManagement.section1Title', 'Sekcja 1: Dodaj nowego użytkownika')}</h2>
                <p className="section-subtitle mb-3">{t('userManagement.section1Subtitle', 'Zarejestruj nowe konto pracownicze w systemie.')}</p>
                {userError && <div className="alert alert-error">{userError}</div>}
                {userMessage && <div className="alert alert-success"><strong>{userMessage}</strong></div>}

                <form onSubmit={handleUserFormSubmit}>
                    <div className="grid-2-col" style={{ marginTop: '15px' }}>
                        <div className="form-group"><span className="form-label">{t('userManagement.firstName', 'Imię')}</span><input className="form-input" type="text" name="name" value={userFormData.name} onChange={handleUserFormChange} required /></div>
                        <div className="form-group"><span className="form-label">{t('userManagement.lastName', 'Nazwisko')}</span><input className="form-input" type="text" name="surname" value={userFormData.surname} onChange={handleUserFormChange} required /></div>
                        <div className="form-group"><span className="form-label">{t('userManagement.emailLabel', 'Adres e-mail')}</span><input className="form-input" type="email" name="email" value={userFormData.email} onChange={handleUserFormChange} required /></div>
                        <div className="form-group"><span className="form-label">{t('userManagement.startDateLabel', 'Data rozpoczęcia pracy')}</span><input className="form-input" type="date" name="date" value={userFormData.date} onChange={handleUserFormChange} required /></div>

                        <div className="form-group"><span className="form-label">{t('userManagement.systemRole', 'Rola systemowa')}</span>
                            <select className="form-input" name="roleName" value={userFormData.roleName} onChange={handleUserFormChange}>
                                <option value="ROLE_EMPLOYEE">{t('userManagement.roles.employee', 'Pracownik')}</option>
                                <option value="ROLE_HR">{t('userManagement.roles.hr', 'HR')}</option>
                                <option value="ROLE_MANAGER">{t('userManagement.roles.manager', 'Kierownik')}</option>
                                <option value="ROLE_ADMIN">{t('userManagement.roles.admin', 'Administrator')}</option>
                            </select>
                        </div>

                        {(userFormData.roleName === 'ROLE_EMPLOYEE' || userFormData.roleName === 'ROLE_MANAGER') && (
                            <div className="form-group"><span className="form-label">{t('userManagement.assignManagerLabel', 'Przypisz managera')}</span>
                                <select className="form-input" name="managerEmail" value={userFormData.managerEmail} onChange={handleUserFormChange}>
                                    <option value="">{t('userManagement.selectManagerPlaceholder', '-- Wybierz managera --')}</option>
                                    {managers
                                        .filter(m => m.email !== userFormData.email)
                                        .map(m => <option key={m.email} value={m.email}>{m.name} {m.surname} ({m.email})</option>)
                                    }
                                </select>
                            </div>
                        )}
                    </div>
                    <div className="policies-footer">
                        <div style={{ flex: 1 }}></div>
                        <div className="footer-actions">
                            <button type="submit" className="btn-yellow" disabled={userLoading}>
                                {userLoading ? t('common.processing', 'Przetwarzanie...') : t('userManagement.registerUserBtn', 'Zarejestruj użytkownika')}
                            </button>
                        </div>
                    </div>
                </form>
            </div>

            <div className="policy-section" style={{ marginTop: '2rem' }}>
                <h2 className="section-title">{t('userManagement.section2Title', 'Sekcja 2: Przypisz Managera do istniejącego konta')}</h2>
                <p className="section-subtitle mb-3">{t('userManagement.section2Subtitle', 'Wybierz pracownika oraz managera, którego chcesz mu przypisać.')}</p>
                {managerError && <div className="alert alert-error">{managerError}</div>}
                {managerMessage && <div className="alert alert-success"><strong>{managerMessage}</strong></div>}

                <form className="grid-2-col" style={{ marginTop: '15px' }} onSubmit={handleManagerFormSubmit}>
                    <div className="form-group"><span className="form-label">{t('userManagement.employeeLabel', 'Pracownik')}</span>
                        <select className="form-input" value={managerFormData.email} onChange={(e) => setManagerFormData({...managerFormData, email: e.target.value})} required>
                            <option value="">{t('userManagement.selectEmployeePlaceholder', '-- Wybierz pracownika --')}</option>
                            {users.map(u => <option key={u.email} value={u.email}>{u.name} {u.surname} ({u.email})</option>)}
                        </select>
                    </div>
                    <div className="form-group"><span className="form-label">{t('userManagement.managerLabel', 'Manager')}</span>
                        <select className="form-input" value={managerFormData.managerEmail} onChange={(e) => setManagerFormData({...managerFormData, managerEmail: e.target.value})} required>
                            <option value="">{t('userManagement.selectManagerPlaceholder', '-- Wybierz managera --')}</option>
                            {managers
                                .filter(m => m.email !== managerFormData.email)
                                .map(m => <option key={m.email} value={m.email}>{m.name} {m.surname} ({m.email})</option>)
                            }
                        </select>
                    </div>
                    <div className="form-group" style={{ gridColumn: '1 / -1', marginTop: '1rem' }}>
                        <button type="submit" className="btn-yellow" disabled={managerLoading} style={{ width: '100%', display: 'flex', justifyContent: 'center' }}>
                            {managerLoading ? t('userManagement.assigningBtn', 'Przypisywanie...') : t('userManagement.saveManagerAssignBtn', 'Zapisz przypisanie managera')}
                        </button>
                    </div>
                </form>
            </div>

            <div className="policy-section" style={{ marginTop: '2rem' }}>
                <h2 className="section-title">{t('userManagement.section3Title', 'Sekcja 3: Zarządzaj zatrudnieniem i statusem konta')}</h2>
                <p className="section-subtitle mb-3">{t('userManagement.section3Subtitle', 'Zaktualizuj wybrane daty lub dezaktywuj konto pracownika. Pozostaw puste, by nie zmieniać.')}</p>
                {employmentError && <div className="alert alert-error">{employmentError}</div>}
                {employmentMessage && <div className="alert alert-success"><strong>{employmentMessage}</strong></div>}

                <form onSubmit={handleEmploymentFormSubmit} style={{ marginTop: '15px' }}>
                    <div className="grid-2-col">
                        <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                            <span className="form-label">{t('userManagement.employeeLabel', 'Pracownik')} <span style={{ color: 'red' }}>*</span></span>
                            <select className="form-input" value={employmentFormData.email} onChange={(e) => setEmploymentFormData({...employmentFormData, email: e.target.value})} required>
                                <option value="">{t('userManagement.selectEmployeePlaceholder', '-- Wybierz pracownika --')}</option>
                                {users.map(u => <option key={u.email} value={u.email}>{u.name} {u.surname} ({u.email})</option>)}
                            </select>
                        </div>

                        <div className="form-group">
                            <span className="form-label">{t('userManagement.updateStartDateLabel', 'Zmień datę rozpoczęcia')}</span>
                            <input className="form-input" type="date" value={employmentFormData.startDate} onChange={(e) => setEmploymentFormData({...employmentFormData, startDate: e.target.value})} />
                        </div>

                        <div className="form-group">
                            <span className="form-label">{t('userManagement.updateEndDateLabel', 'Ustaw datę zakończenia')}</span>
                            <input className="form-input" type="date" value={employmentFormData.endDate} onChange={(e) => setEmploymentFormData({...employmentFormData, endDate: e.target.value})} />
                        </div>

                        <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                            <span className="form-label">{t('userManagement.updateStatusLabel', 'Status konta w systemie')}</span>
                            <select className="form-input" value={employmentFormData.active} onChange={(e) => setEmploymentFormData({...employmentFormData, active: e.target.value})}>
                                <option value="">{t('userManagement.statusUnchanged', '-- Nie zmieniaj statusu --')}</option>
                                <option value="true">{t('userManagement.statusActive', 'Konto Aktywne')}</option>
                                <option value="false">{t('userManagement.statusInactive', 'Konto Nieaktywne')}</option>
                            </select>
                        </div>
                    </div>

                    <div className="form-group" style={{ marginTop: '1rem' }}>
                        <button type="submit" className="btn-yellow" disabled={employmentLoading} style={{ width: '100%', display: 'flex', justifyContent: 'center' }}>
                            {employmentLoading ? t('common.processing', 'Przetwarzanie...') : t('userManagement.saveEmploymentBtn', 'Zapisz zmiany w zatrudnieniu')}
                        </button>
                    </div>
                </form>
            </div>

        </div>
    );
};

export default UserManagementPage;
