import React, { useState } from 'react';
import { Card, Button } from '../../components';
import { useTheme } from '../../context/ThemeContext';
import { authService } from '../../services';
import { Eye, EyeOff } from 'lucide-react';
import '../main.css';
import '../Policies/policies.css';

const Settings = () => {
    const { theme, toggleTheme } = useTheme();

    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showOld, setShowOld] = useState(false);
    const [showNew, setShowNew] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const handlePasswordChange = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');

        if (newPassword !== confirmPassword) {
            setError('Nowe hasła nie są zgodne.');
            return;
        }
        if (newPassword.length < 8) {
            setError('Nowe hasło musi mieć co najmniej 8 znaków.');
            return;
        }
        if (newPassword === oldPassword) {
            setError('Nowe hasło nie może być takie samo jak obecne.');
            return;
        }

        setLoading(true);
        try {
            await authService.changePassword(oldPassword, newPassword);
            setSuccess('Hasło zostało zmienione pomyślnie.');
            setOldPassword('');
            setNewPassword('');
            setConfirmPassword('');
        } catch (err) {
            setError(err.message || 'Nie udało się zmienić hasła.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="page-container">
            <h2 className="page-title-no-margin">Ustawienia</h2>

            <Card title="Wygląd">
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <span style={{ color: 'var(--text-main)', width: '200px' }}>
                        Aktualny motyw: {theme === 'light' ? 'Jasny' : 'Ciemny'}
                    </span>
                    <Button onClick={toggleTheme} style={{ width: 'auto' }}>
                        Zmień motyw
                    </Button>
                </div>
            </Card>

            <div style={{ marginTop: '1.5rem' }}>
                <Card title="Zmiana hasła">
                    {error && (
                        <div style={{ background: '#fef2f2', color: '#991b1b', padding: '0.75rem', borderRadius: '6px', marginBottom: '1rem', border: '1px solid #fecaca' }}>
                            {error}
                        </div>
                    )}
                    {success && (
                        <div style={{ background: '#f0fdf4', color: '#16a34a', padding: '0.75rem', borderRadius: '6px', marginBottom: '1rem', border: '1px solid #bbf7d0' }}>
                            {success}
                        </div>
                    )}
                    <form onSubmit={handlePasswordChange}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', maxWidth: '400px' }}>
                            <div className="form-group">
                                <span className="form-label">Obecne hasło</span>
                                <div className="password-wrapper">
                                    <input
                                        className="form-input"
                                        type={showOld ? 'text' : 'password'}
                                        value={oldPassword}
                                        onChange={(e) => setOldPassword(e.target.value)}
                                        autoComplete="current-password"
                                        placeholder="Wprowadź obecne hasło"
                                        required
                                    />
                                    <button type="button" className="password-toggle" onClick={() => setShowOld(v => !v)} aria-label={showOld ? 'Ukryj hasło' : 'Pokaż hasło'}>
                                        {showOld ? <EyeOff size={16} /> : <Eye size={16} />}
                                    </button>
                                </div>
                            </div>
                            <div className="form-group">
                                <span className="form-label">Nowe hasło</span>
                                <div className="password-wrapper">
                                    <input
                                        className="form-input"
                                        type={showNew ? 'text' : 'password'}
                                        value={newPassword}
                                        onChange={(e) => setNewPassword(e.target.value)}
                                        autoComplete="new-password"
                                        placeholder="Minimum 8 znaków"
                                        required
                                    />
                                    <button type="button" className="password-toggle" onClick={() => setShowNew(v => !v)} aria-label={showNew ? 'Ukryj hasło' : 'Pokaż hasło'}>
                                        {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
                                    </button>
                                </div>
                            </div>
                            <div className="form-group">
                                <span className="form-label">Powtórz nowe hasło</span>
                                <div className="password-wrapper">
                                    <input
                                        className="form-input"
                                        type={showConfirm ? 'text' : 'password'}
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        autoComplete="new-password"
                                        placeholder="Powtórz nowe hasło"
                                        required
                                    />
                                    <button type="button" className="password-toggle" onClick={() => setShowConfirm(v => !v)} aria-label={showConfirm ? 'Ukryj hasło' : 'Pokaż hasło'}>
                                        {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
                                    </button>
                                </div>
                            </div>
                            <div>
                                <button type="submit" className="btn-yellow" disabled={loading} style={{ width: '100%' }}>
                                    {loading ? 'Zapisywanie...' : 'Zmień hasło'}
                                </button>
                            </div>
                        </div>
                    </form>
                </Card>
            </div>
        </div>
    );
};

export default Settings;
