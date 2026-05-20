import React, { useState } from 'react';
import { authService } from '../../services';
import './Login.css';

const ForgotPasswordRequest = () => {
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setMessage('');
        if (!email) {
            setError('Wprowadź adres e-mail.');
            return;
        }
        setLoading(true);
        try {
            await authService.requestPasswordReset(email);
            setMessage('Jeśli konto z tym adresem istnieje, wysłaliśmy instrukcję resetu hasła.');
        } catch (err) {
            setError(err?.message || 'Błąd przy wysyłaniu żądania. Spróbuj ponownie.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">
            <div className="login-card" role="main">
                <h1 className="login-title">Resetowanie hasła</h1>
                <p className="login-subtitle">Wprowadź adres e-mail, aby otrzymać instrukcję resetu.</p>

                {error && <div className="login-error" role="alert">{error}</div>}
                {message && <div className="login-success" role="status">{message}</div>}

                <form className="login-form" onSubmit={handleSubmit} noValidate>
                    <label className="field">
                        <span className="field-label">Adres e-mail</span>
                        <input
                            className="field-input"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            autoComplete="email"
                            placeholder="admin@chrono.pl"
                            required
                        />
                    </label>

                    <button className="btn-primary" type="submit" disabled={loading}>
                        {loading ? 'Wysyłanie...' : 'Wyślij instrukcję'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default ForgotPasswordRequest;

