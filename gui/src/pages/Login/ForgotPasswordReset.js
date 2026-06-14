import React, { useState, useEffect } from 'react';
import { authService } from '../../services';
import { useLocation, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Globe } from 'lucide-react'; // Dodano Globe
import { useLanguage } from '../../context/LanguageContext';
import './Login.css';

function useQuery() {
    return new URLSearchParams(useLocation().search);
}

const ForgotPasswordReset = () => {
    const { t, language, changeLanguage } = useLanguage();

    const query = useQuery();
    const navigate = useNavigate();
    const tokenFromQuery = query.get('token') || '';

    const [token, setToken] = useState(tokenFromQuery);
    const [password, setPassword] = useState('');
    const [confirm, setConfirm] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirm, setShowConfirm] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    useEffect(() => {
        if (tokenFromQuery) {
            setToken(tokenFromQuery);
        }
    }, [tokenFromQuery]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setMessage('');
        if (!token) {
            setError(t('errors.genericError', 'Brak tokenu resetującego.'));
            return;
        }
        if (!password || password.length < 6) {
            setError(t('resetPassword.passwordTooShort', 'Hasło musi mieć co najmniej 6 znaków.'));
            return;
        }
        if (password !== confirm) {
            setError(t('resetPassword.passwordMismatch', 'Hasła nie pasują do siebie.'));
            return;
        }

        setLoading(true);
        try {
            await authService.resetPassword(token, password);
            setMessage(t('resetPassword.resetSuccess', 'Hasło zostało zresetowane. Możesz się teraz zalogować.'));
            setTimeout(() => navigate('/login'), 2000);
        } catch (err) {
            setError(err?.message || t('errors.genericError', 'Błąd podczas resetowania hasła.'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">

            <div style={{ position: 'absolute', top: '20px', right: '20px' }}>
                <div className="language-selector-pill">
                    <Globe size={18} style={{ color: '#666' }} />
                    <select
                        className="language-selector-select"
                        value={language}
                        onChange={(e) => changeLanguage(e.target.value)}
                    >
                        <option value="pl">PL</option>
                        <option value="en">EN</option>
                    </select>
                </div>
            </div>

            <div className="login-card" role="main">
                <h1 className="login-title">{t('resetPassword.title', 'Ustaw nowe hasło')}</h1>
                <p className="login-subtitle">{t('resetPassword.subtitle', 'Wprowadź nowe hasło dla konta.')}</p>

                {error && <div className="login-error" role="alert">{error}</div>}
                {message && <div className="login-success" role="status">{message}</div>}

                <form className="login-form" onSubmit={handleSubmit} noValidate>
                    {!token && (
                        <label className="field">
                            <span className="field-label">{t('resetPassword.token', 'Token resetujący')}</span>
                            <input className="field-input" value={token} onChange={(e) => setToken(e.target.value)} />
                        </label>
                    )}

                    <label className="field">
                        <span className="field-label">{t('resetPassword.newPassword', 'Nowe hasło')}</span>
                        <div className="password-wrapper">
                            <input
                                className="field-input"
                                type={showPassword ? 'text' : 'password'}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                autoComplete="new-password"
                                placeholder={t('resetPassword.passwordTooShort', 'Minimum 6 znaków')}
                            />
                            <button
                                type="button"
                                className="password-toggle"
                                onClick={() => setShowPassword(v => !v)}
                                aria-label={showPassword ? t('topbar.hidePassword', 'Ukryj hasło') : t('topbar.showPassword', 'Pokaż hasło')}
                            >
                                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                            </button>
                        </div>
                    </label>

                    <label className="field">
                        <span className="field-label">{t('resetPassword.confirmPassword', 'Potwierdź hasło')}</span>
                        <div className="password-wrapper">
                            <input
                                className="field-input"
                                type={showConfirm ? 'text' : 'password'}
                                value={confirm}
                                onChange={(e) => setConfirm(e.target.value)}
                                autoComplete="new-password"
                                placeholder={t('resetPassword.confirmPassword', 'Powtórz nowe hasło')}
                            />
                            <button
                                type="button"
                                className="password-toggle"
                                onClick={() => setShowConfirm(v => !v)}
                                aria-label={showConfirm ? t('topbar.hidePassword', 'Ukryj hasło') : t('topbar.showPassword', 'Pokaż hasło')}
                            >
                                {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
                            </button>
                        </div>
                    </label>

                    <button className="btn-primary" type="submit" disabled={loading}>
                        {loading ? t('common.loading_data', 'Przetwarzanie...') : t('resetPassword.resetButton', 'Ustaw nowe hasło')}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default ForgotPasswordReset;
