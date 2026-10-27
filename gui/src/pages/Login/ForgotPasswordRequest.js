import React, { useState } from 'react';
import { authService } from '../../services';
import { useLanguage } from '../../context/LanguageContext';
import LanguageSwitcher from '../../components/ui/LanguageSwitcher';
import ThemeToggle from '../../components/ui/ThemeToggle';
import './Login.css';

const ForgotPasswordRequest = () => {
    const { t } = useLanguage();

    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setMessage('');
        if (!email) {
            setError(t('forgotPassword.invalidEmail', 'Wprowadź adres e-mail.'));
            return;
        }
        setLoading(true);
        try {
            await authService.requestPasswordReset(email);
            setMessage(t('forgotPassword.emailSent', 'Jeśli konto z tym adresem istnieje, wysłaliśmy instrukcję resetu hasła.'));
        } catch (err) {
            setError(err?.message || t('errors.genericError', 'Błąd przy wysyłaniu żądania. Spróbuj ponownie.'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">

            <div className="login-toolbar">
                <LanguageSwitcher />
                <ThemeToggle />
            </div>

            <div className="login-card" role="main">
                <h1 className="login-title">{t('forgotPassword.title', 'Resetowanie hasła')}</h1>
                <p className="login-subtitle">{t('forgotPassword.subtitle', 'Wprowadź adres e-mail, aby otrzymać instrukcję resetu.')}</p>

                {error && <div className="login-error" role="alert">{error}</div>}
                {message && <div className="login-success" role="status">{message}</div>}

                <form className="login-form" onSubmit={handleSubmit} noValidate>
                    <label className="field">
                        <span className="field-label">{t('login.email', 'Adres e-mail')}</span>
                        <input
                            className="field-input"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            autoComplete="email"
                            placeholder={t('login.emailPlaceholder', 'admin@chrono.pl')}
                            required
                        />
                    </label>

                    <button className="btn-primary" type="submit" disabled={loading}>
                        {loading ? t('common.loading_data', 'Wysyłanie...') : t('forgotPassword.sendButton', 'Wyślij instrukcję')}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default ForgotPasswordRequest;
