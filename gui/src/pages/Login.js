import React, { useState } from 'react';
import { authService } from '../services/authService';
import './Login.css';

const Login = ({ onLoginSuccess }) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            await authService.login(email, password);
            onLoginSuccess();
        } catch (err) {
            setError(err.message || 'Wystąpił błąd podczas logowania.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <h2>Logowanie do Chrono2</h2>
                <p>Wprowadź swoje dane, aby kontynuować</p>

                {error && <div className="login-error">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Adres e-mail</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="np. pracownik@chrono.pl"
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Hasło</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Wpisz hasło"
                            required
                        />
                    </div>
                    <button type="submit" className="login-button" disabled={isLoading}>
                        {isLoading ? 'Logowanie...' : 'Zaloguj się'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default Login;
