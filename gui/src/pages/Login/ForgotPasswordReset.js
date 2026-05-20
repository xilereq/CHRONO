import React, { useState, useEffect } from 'react';
import { authService } from '../../services';
import { useLocation, useNavigate } from 'react-router-dom';
import './Login.css';

function useQuery() {
    return new URLSearchParams(useLocation().search);
}

const ForgotPasswordReset = () => {
    const query = useQuery();
    const navigate = useNavigate();
    const tokenFromQuery = query.get('token') || '';

    const [token, setToken] = useState(tokenFromQuery);
    const [password, setPassword] = useState('');
    const [confirm, setConfirm] = useState('');
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
            setError('Brak tokenu resetującego.');
            return;
        }
        if (!password || password.length < 6) {
            setError('Hasło musi mieć co najmniej 6 znaków.');
            return;
        }
        if (password !== confirm) {
            setError('Hasła nie są zgodne.');
            return;
        }

        setLoading(true);
        try {
            await authService.resetPassword(token, password);
            setMessage('Hasło zostało zresetowane. Możesz się teraz zalogować.');
            setTimeout(() => navigate('/login'), 2000);
        } catch (err) {
            setError(err?.message || 'Błąd podczas resetowania hasła.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">
            <div className="login-card" role="main">
                <h1 className="login-title">Ustaw nowe hasło</h1>
                <p className="login-subtitle">Wprowadź nowe hasło dla konta.</p>

                {error && <div className="login-error" role="alert">{error}</div>}
                {message && <div className="login-success" role="status">{message}</div>}

                <form className="login-form" onSubmit={handleSubmit} noValidate>
                    {!token && (
                        <label className="field">
                            <span className="field-label">Token resetujący</span>
                            <input className="field-input" value={token} onChange={(e) => setToken(e.target.value)} />
                        </label>
                    )}

                    <label className="field">
                        <span className="field-label">Nowe hasło</span>
                        <input className="field-input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" />
                    </label>

                    <label className="field">
                        <span className="field-label">Potwierdź hasło</span>
                        <input className="field-input" type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" />
                    </label>

                    <button className="btn-primary" type="submit" disabled={loading}>
                        {loading ? 'Przetwarzanie...' : 'Ustaw nowe hasło'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default ForgotPasswordReset;

