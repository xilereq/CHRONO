import React, { useState } from 'react';
import { authService } from '../../services';

const UserFormTab = () => {
    const [formData, setFormData] = useState({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        role: 'USER'
    });

    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setMessage('');
        setLoading(true);

        try {
            await authService.register(formData);
            setMessage(`Użytkownik ${formData.firstName} ${formData.lastName} został pomyślnie dodany.`);
            setFormData({ firstName: '', lastName: '', email: '', password: '', role: 'USER' });
        } catch (err) {
            setError(err.message || 'Wystąpił błąd podczas dodawania użytkownika.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <h1 className="policies-header">Zarządzanie Użytkownikami</h1>

            <div className="policy-section">
                <h2 className="section-title">Sekcja 1: Dodaj nowego użytkownika</h2>
                <p className="section-subtitle mb-3">Zarejestruj nowe konto pracownicze w systemie.</p>

                {error && (
                    <div style={{ background: '#fef2f2', color: '#991b1b', padding: '0.75rem', borderRadius: '6px', marginBottom: '1rem', border: '1px solid #fecaca' }}>
                        {error}
                    </div>
                )}
                {message && (
                    <div style={{ background: '#f0fdf4', color: '#16a34a', padding: '0.75rem', borderRadius: '6px', marginBottom: '1rem', border: '1px solid #bbf7d0' }}>
                        {message}
                    </div>
                )}

                <div className="grid-2-col" style={{ marginTop: '15px' }}>
                    <div className="form-group">
                        <span className="form-label">Imię</span>
                        <input
                            className="form-input" type="text" name="firstName"
                            value={formData.firstName} onChange={handleChange} required
                        />
                    </div>
                    <div className="form-group">
                        <span className="form-label">Nazwisko</span>
                        <input
                            className="form-input" type="text" name="lastName"
                            value={formData.lastName} onChange={handleChange} required
                        />
                    </div>
                    <div className="form-group">
                        <span className="form-label">Adres e-mail</span>
                        <input
                            className="form-input" type="email" name="email"
                            value={formData.email} onChange={handleChange} required
                        />
                    </div>
                    <div className="form-group">
                        <span className="form-label">Hasło początkowe</span>
                        <input
                            className="form-input" type="password" name="password"
                            value={formData.password} onChange={handleChange} required
                        />
                    </div>
                    <div className="form-group">
                        <span className="form-label">Rola systemowa</span>
                        <select
                            className="form-input" name="role"
                            value={formData.role} onChange={handleChange}
                        >
                            <option value="USER">Pracownik (USER)</option>
                            <option value="MANAGER">Kierownik (MANAGER)</option>
                            <option value="ADMIN">Administrator (ADMIN)</option>
                        </select>
                    </div>
                </div>
            </div>

            <div className="policies-footer">
                <div style={{ flex: 1 }}></div>
                <div className="footer-actions">
                    <button className="btn-yellow" onClick={handleSubmit} disabled={loading}>
                        {loading ? 'Przetwarzanie...' : 'Zarejestruj użytkownika'}
                    </button>
                </div>
            </div>
        </>
    );
};

export default UserFormTab;
