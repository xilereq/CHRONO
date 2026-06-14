import React, { useState, useEffect } from 'react';
import policiesService from '../../services/policiesService';
import userService from '../../services/userService';
import { useLanguage } from '../../context/LanguageContext';

const AssignPolicyTab = () => {
    const { t } = useLanguage();
    const [users, setUsers] = useState([]);
    const [policies, setPolicies] = useState([]);

    const [formData, setFormData] = useState({ userIds: [], policyId: '', startDate: '' });

    const [loading, setLoading] = useState(false);
    const [isFetchingUsers, setIsFetchingUsers] = useState(false);
    const [roleFilter, setRoleFilter] = useState('ALL');

    useEffect(() => {
        const fetchPolicies = async () => {
            try {
                const policiesData = await policiesService.getAllPolicies();
                setPolicies(policiesData);
            } catch (error) {
                console.error(t('policies.fetchPoliciesConsoleError', "Błąd ładowania polityk:"), error);
            }
        };
        fetchPolicies();
    }, [t]);

    useEffect(() => {
        const fetchUsers = async () => {
            setIsFetchingUsers(true);
            try {
                let data = [];
                if (roleFilter === 'MANAGER') data = await userService.getManagers();
                else if (roleFilter === 'EMPLOYEE') data = await userService.getEmployees();
                else if (roleFilter === 'ADMIN') data = await userService.getAdmins();
                else if (roleFilter === 'HR') data = await userService.getHrs();
                else data = await userService.getAllUsers();

                setUsers(data);
                setFormData(prev => ({ ...prev, userIds: [] }));
            } catch (error) {
                console.error(t('policies.fetchUsersConsoleError', "Błąd pobierania użytkowników:"), error);
            } finally {
                setIsFetchingUsers(false);
            }
        };
        fetchUsers();
    }, [roleFilter, t]);

    const handleUserSelectChange = (e) => {
        const selectedOptions = Array.from(e.target.selectedOptions, option => option.value);
        setFormData({ ...formData, userIds: selectedOptions });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (formData.userIds.length === 0 || !formData.policyId || !formData.startDate) {
            alert(t('policies.fillAllFieldsAlert', "Wypełnij wszystkie pola: wybierz co najmniej jednego pracownika, politykę oraz datę początkową."));
            return;
        }

        setLoading(true);
        try {
            await policiesService.assignPolicyBulk({
                policyId: parseInt(formData.policyId),
                userIds: formData.userIds.map(id => parseInt(id)),
                startDate: formData.startDate
            });

            alert(`${t('policies.policyAssignedTo', 'Polityka została pomyślnie przypisana do')} ${formData.userIds.length} ${t('policies.employeesCount', 'pracownika(ów)!')}`);
            setFormData({ userIds: [], policyId: '', startDate: '' });
        } catch (error) {
            alert(t('policies.assignPolicyError', "Błąd przypisywania polityki: ") + error.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <h1 className="policies-header">{t('policies.assignPolicyBulkTitle', 'Przypisz Politykę (Masowo)')}</h1>

            <div className="policy-section">
                <h2 className="section-title">{t('policies.section1ParamsTitle', 'Sekcja 1: Wybór parametrów')}</h2>
                <p className="section-subtitle mb-3">
                    {t('policies.assignPolicySubtitle', 'Powiąż wybranych pracowników z konkretnym systemem czasu pracy od wybranej daty.')} <br/>
                    <strong style={{color: 'var(--primary-color)'}}>
                        {t('policies.multiSelectHint', 'Wskazówka: Przytrzymaj przycisk Ctrl (Windows) lub Cmd (Mac), aby zaznaczyć wielu pracowników naraz!')}
                    </strong>
                </p>

                <form className="grid-2-col" style={{ marginTop: '15px' }} onSubmit={handleSubmit}>

                    <div className="form-group" style={{ gridColumn: '1 / -1', marginBottom: '10px' }}>
                        <span className="form-label">{t('policies.filterRoleLabel', 'Filtruj pracowników po roli')}</span>
                        <select
                            className="form-input"
                            value={roleFilter}
                            onChange={(e) => setRoleFilter(e.target.value)}
                        >
                            <option value="ALL">{t('policies.roles.all', 'Wszyscy użytkownicy')}</option>
                            <option value="EMPLOYEE">{t('policies.roles.employee', 'Pracownicy (Zwykli)')}</option>
                            <option value="MANAGER">{t('policies.roles.manager', 'Kierownicy')}</option>
                            <option value="HR">{t('policies.roles.hr', 'Dział HR')}</option>
                            <option value="ADMIN">{t('policies.roles.admin', 'Administratorzy')}</option>
                        </select>
                    </div>

                    <div className="form-group">
                        <span className="form-label">{t('policies.employeeMultiSelectLabel', 'Pracownik (Można wybrać wielu)')}</span>
                        <select
                            multiple
                            size="6"
                            className="form-input"
                            value={formData.userIds}
                            onChange={handleUserSelectChange}
                            required
                            disabled={isFetchingUsers}
                            style={{ padding: '8px', height: '140px' }}
                        >
                            {isFetchingUsers ? (
                                <option disabled value="">{t('policies.fetchingList', 'Pobieranie listy...')}</option>
                            ) : (
                                users.map(user => (
                                    <option key={user.userID} value={user.userID} style={{ padding: '4px' }}>
                                        {user.name} {user.surname} ({user.email})
                                    </option>
                                ))
                            )}
                        </select>
                        <span style={{ fontSize: '0.75rem', color: '#666', marginTop: '4px', display: 'block' }}>
                            {t('policies.selectedCount', 'Zaznaczono:')} {formData.userIds.length}
                        </span>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                        <div className="form-group">
                            <span className="form-label">{t('policies.workPolicyLabel', 'Polityka pracy')}</span>
                            <select
                                className="form-input"
                                value={formData.policyId}
                                onChange={(e) => setFormData({...formData, policyId: e.target.value})}
                                required
                            >
                                <option value="">{t('policies.selectPolicyPlaceholder', '-- Wybierz politykę --')}</option>
                                {policies.map(policy => (
                                    <option key={policy.id} value={policy.id}>
                                        {policy.name} ({policy.dailyHours}h/{t('policies.daySuffix', 'dzień')})
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="form-group">
                            <span className="form-label">{t('policies.startDateLabel', 'Obowiązuje od dnia (Start Date)')}</span>
                            <input
                                type="date"
                                className="form-input"
                                value={formData.startDate}
                                onChange={(e) => setFormData({...formData, startDate: e.target.value})}
                                required
                            />
                        </div>
                    </div>

                    <div className="form-group" style={{ gridColumn: '1 / -1', marginTop: '1rem' }}>
                        <button type="submit" className="btn-yellow" disabled={loading} style={{ width: '100%', display: 'flex', justifyContent: 'center' }}>
                            {loading ? t('policies.assigningBtn', 'Przypisywanie...') : `${t('policies.saveAssignmentForBtn', 'Zapisz przypisanie dla')} ${formData.userIds.length || '0'} ${t('policies.peopleCountSuffix', 'osób')}`}
                        </button>
                    </div>
                </form>
            </div>
        </>
    );
};

export default AssignPolicyTab;
