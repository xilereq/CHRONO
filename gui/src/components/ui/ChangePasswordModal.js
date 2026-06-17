import React, { useState } from 'react';
import { authService } from '../../services';
import { Eye, EyeOff } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';
import '../../pages/Login/Login.css';

const ChangePasswordModal = ({ onPasswordChanged }) => {
    const { t } = useLanguage();
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
            setError(t('changePasswordModal.newPasswordsMismatch', 'Nowe hasła nie są zgodne.'));
            return;
        }
        if (newPassword.length < 8) {
            setError(t('changePasswordModal.passwordMin', 'Nowe hasło musi mieć co najmniej 8 znaków.'));
            return;
        }
        if (newPassword === oldPassword) {
            setError(t('changePasswordModal.passwordSameAsTemp', 'Nowe hasło nie może być takie samo jak tymczasowe.'));
            return;
        }

        setLoading(true);
        try {
            await authService.changeFirstPassword(oldPassword, newPassword);
            onPasswordChanged();
        } catch (err) {
            setError(err.message || t('changePasswordModal.changeError', 'Nie udało się zmienić hasła.'));
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

                <h1 className="login-title">{t('changePasswordModal.firstLoginTitle', 'Ustaw nowe hasło')}</h1>
                <p className="login-subtitle">
                    {t('changePasswordModal.firstLoginSubtitle', 'To Twoje pierwsze logowanie. Zmień tymczasowe hasło, aby kontynuować.')}
                </p>

                {error && <div className="login-error" role="alert">{error}</div>}

                <form className="login-form" onSubmit={handleSubmit} noValidate>

                    <label className="field">
                        <span className="field-label">{t('changePasswordModal.newPasswordLabel', 'Nowe hasło')}</span>
                        <div className="password-wrapper">
                            <input
                                className="field-input"
                                type={showNew ? 'text' : 'password'}
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                autoComplete="new-password"
                                placeholder={t('changePasswordModal.placeholderMin8', 'Minimum 8 znaków')}
                                required
                            />
                            <button type="button" className="password-toggle" onClick={() => setShowNew(v => !v)} aria-label={showNew ? t('topbar.hidePassword', 'Ukryj hasło') : t('topbar.showPassword', 'Pokaż hasło')}>
                                {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
                            </button>
                        </div>
                    </label>

                    <label className="field">
                        <span className="field-label">{t('changePasswordModal.repeatNewPasswordLabel', 'Powtórz nowe hasło')}</span>
                        <div className="password-wrapper">
                            <input
                                className="field-input"
                                type={showConfirm ? 'text' : 'password'}
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                autoComplete="new-password"
                                placeholder={t('changePasswordModal.placeholderRepeat', 'Powtórz nowe hasło')}
                                required
                            />
                            <button type="button" className="password-toggle" onClick={() => setShowConfirm(v => !v)} aria-label={showConfirm ? t('topbar.hidePassword', 'Ukryj hasło') : t('topbar.showPassword', 'Pokaż hasło')}>
                                {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
                            </button>
                        </div>
                    </label>

                    <button className="btn-primary" type="submit" disabled={loading}>
                        {loading ? t('changePasswordModal.saving', 'Zapisywanie...') : t('changePasswordModal.setPasswordBtn', 'Ustaw nowe hasło')}
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
                        {t('common.logout', 'Wyloguj się')}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ChangePasswordModal;
