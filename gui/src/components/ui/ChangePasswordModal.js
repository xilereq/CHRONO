import React, { useState } from 'react';
import { authService } from '../../services';
import { Eye, EyeOff } from 'lucide-react';
import '../../pages/Login/Login.css';

const ChangePasswordModal = ({ onPasswordChanged }) => {
    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showOld, setShowOld] = useState(false);
    const [showNew, setShowNew] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (newPassword !== confirmPassword) {
            setError('Nowe hasła nie są zgodne.');
            return;
        }
        if (newPassword.length < 8) {
            setError('Nowe hasło musi mieć co najmniej 8 znaków.');
            return;
        }
        if (newPassword === oldPassword) {
            setError('Nowe hasło nie może być takie samo jak tymczasowe.');
            return;
        }

        setLoading(true);
        try {
            await authService.changeFirstPassword(oldPassword, newPassword);
            onPasswordChanged();
        } catch (err) {
            setError(err.message || 'Nie udało się zmienić hasła.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">
            <div className="login-card">
                <div className="brand-wrap">
                    <div className="brand-icon" aria-hidden="true">
                        <svg width="34" height="34" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <circle cx="12" cy="12" r="9" stroke="#E6B800" strokeWidth="1.6" fill="none"/>
                            <path d="M12 7.5V12l3 1.8" stroke="#E6B800" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                    </div>
                </div>

                <h1 className="login-title">Ustaw nowe hasło</h1>
                <p className="login-subtitle">
                    To Twoje pierwsze logowanie. Zmień tymczasowe hasło, aby kontynuować.
                </p>

                {error && <div className="login-error" role="alert">{error}</div>}

                <form className="login-form" onSubmit={handleSubmit} noValidate>
                    <label className="field">
                        <span className="field-label">Tymczasowe hasło</span>
                        <div className="password-wrapper">
                            <input
                                className="field-input"
                                type={showOld ? 'text' : 'password'}
                                value={oldPassword}
                                onChange={(e) => setOldPassword(e.target.value)}
                                autoComplete="current-password"
                                placeholder="Wprowadź tymczasowe hasło"
                                required
                                autoFocus
                            />
                            <button type="button" className="password-toggle" onClick={() => setShowOld(v => !v)} aria-label={showOld ? 'Ukryj hasło' : 'Pokaż hasło'}>
                                {showOld ? <EyeOff size={16} /> : <Eye size={16} />}
                            </button>
                        </div>
                    </label>

                    <label className="field">
                        <span className="field-label">Nowe hasło</span>
                        <div className="password-wrapper">
                            <input
                                className="field-input"
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
                    </label>

                    <label className="field">
                        <span className="field-label">Powtórz nowe hasło</span>
                        <div className="password-wrapper">
                            <input
                                className="field-input"
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
                    </label>

                    <button className="btn-primary" type="submit" disabled={loading}>
                        {loading ? 'Zapisywanie...' : 'Ustaw nowe hasło'}
                    </button>
                </form>

                <div style={{ marginTop: '1rem', textAlign: 'center' }}>
                    <button
                        onClick={() => authService.logout()}
                        style={{
                            background: 'none',
                            border: 'none',
                            color: 'var(--text-muted)',
                            fontSize: '0.9rem',
                            cursor: 'pointer',
                            textDecoration: 'underline',
                        }}
                    >
                        Wyloguj się
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ChangePasswordModal;
