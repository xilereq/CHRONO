import React, { useState, useEffect } from 'react';
import { Sidebar, Topbar } from './components';
import { Grafik, Dashboard, Requests, RequestDetail, Raporty} from './pages';
import Login from './pages/Login/Login';
import { Routes, Route, Navigate } from 'react-router-dom';
import { authService } from './services';
import './index.css';

const App = () => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [userEmail, setUserEmail] = useState('');

    useEffect(() => {
        const authStatus = authService.isAuthenticated();
        setIsAuthenticated(authStatus);

        if (authStatus) {
            setUserEmail(authService.getUserEmail());
        }

        setIsLoading(false);
    }, []);

    const handleLoginSuccess = () => {
        setIsAuthenticated(true);
        setUserEmail(authService.getUserEmail());
    };

    const handleLogout = () => {
        authService.logout();
        setIsAuthenticated(false);
        setUserEmail('');
    };

    if (isLoading) {
        return <div style={{ padding: '2rem' }}>Ładowanie aplikacji...</div>;
    }

    return (
        <>
            {!isAuthenticated ? (
                <Routes>
                    <Route path="/login" element={<Login onLoginSuccess={handleLoginSuccess} />} />
                    <Route path="*" element={<Navigate to="/login" replace />} />
                </Routes>
            ) : (
                <div className="app-container">
                    <Sidebar />
                    <div className="app-content">
                        <Topbar userEmail={userEmail} onLogout={handleLogout} />

                        <Routes>
                            <Route path="/" element={<Dashboard />} />
                            <Route path="/requests" element={<Requests />} />
                            <Route path="/requests/:id" element={<RequestDetail />} />
                            <Route path="/raporty" element={<Raporty/>} />
                            <Route path="/grafik" element={<Grafik />} />
                            <Route path="/ustawienia" element={<div style={{padding: '2rem'}}>Ustawienia (w budowie)</div>} />
                            <Route path="*" element={<Navigate to="/" replace />} />
                        </Routes>
                    </div>
                </div>
            )}
        </>
    );
}

export default App;
