import React, { useState } from 'react';
import { authService } from '../../services';
import { Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import './Login.css';

const Login = ({ onLoginSuccess }) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        if (!email || !password) {
            setError('Wprowadź email i hasło.');
            return;
        }
        setLoading(true);
        try {
            await authService.login(email, password);
            onLoginSuccess();
        } catch (err) {
            setError(err?.message || 'Błąd logowania. Sprawdź dane i spróbuj ponownie.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">
            <div className="login-card" role="main" aria-labelledby="login-title">
                <div className="brand-wrap">
                    <div className="brand-icon" aria-hidden="true">
                        {/* prosty SVG zegara */}
                        <svg width="34" height="34" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <circle cx="12" cy="12" r="9" stroke="#E6B800" strokeWidth="1.6" fill="none"/>
                            <path d="M12 7.5V12l3 1.8" stroke="#E6B800" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                    </div>
                </div>

                <h1 id="login-title" className="login-title">Logowanie do Chrono</h1>
                <p className="login-subtitle">Wprowadź swoje dane, aby kontynuować</p>

                {error && <div className="login-error" role="alert">{error}</div>}

                <form className="login-form" onSubmit={handleSubmit} noValidate>
                    <label className="field">
                        <span className="field-label">Adres e-mail</span>
                        <input
                            className="field-input"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            autoComplete="username"
                            placeholder="admin@chrono.pl"
                            required
                        />
                    </label>

                    <label className="field">
                        <span className="field-label">Hasło</span>
                        <div className="password-wrapper">
                            <input
                                className="field-input"
                                type={showPassword ? 'text' : 'password'}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                autoComplete="current-password"
                                placeholder="●●●●●●●"
                                required
                            />
                            <button
                                type="button"
                                className="password-toggle"
                                onClick={() => setShowPassword(v => !v)}
                                aria-label={showPassword ? 'Ukryj hasło' : 'Pokaż hasło'}
                            >
                                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                            </button>
                        </div>
                    </label>

                    <button className="btn-primary" type="submit" disabled={loading}>
                        {loading ? 'Logowanie...' : 'Zaloguj się'}
                    </button>
                </form>
                <div style={{ marginTop: '1rem', textAlign: 'center' }}>
                    <Link to="/forgot-password" className="login-forgot-link">Zapomniałem hasła</Link>
                </div>
            </div>
        </div>
    );
};

export default Login;
