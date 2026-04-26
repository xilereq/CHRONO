import React, { useState, useEffect } from 'react';
import { Sidebar, Topbar } from './components';
import { Grafik, Dashboard, MyRequests, RequestDetail } from './pages';
import Login from './pages/Login';
import { Routes, Route, Navigate } from 'react-router-dom';
import { authService } from './services/authService';
import './index.css';

const App = () => {
    // Stany do zarządzania sesją użytkownika
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [userEmail, setUserEmail] = useState('');

    // Sprawdzanie czy użytkownik jest zalogowany przy uruchomieniu aplikacji
    useEffect(() => {
        const authStatus = authService.isAuthenticated();
        setIsAuthenticated(authStatus);

        // Jeśli jest zalogowany, pobieramy jego e-mail z authService
        if (authStatus) {
            setUserEmail(authService.getUserEmail());
        }

        setIsLoading(false);
    }, []);

    // Funkcja wywoływana po udanym logowaniu w formularzu Login.js
    const handleLoginSuccess = () => {
        setIsAuthenticated(true);
        setUserEmail(authService.getUserEmail()); // Aktualizujemy stan e-maila po zalogowaniu
    };

    // Funkcja wywoływana po kliknięciu przycisku wyloguj w Topbarze
    const handleLogout = () => {
        authService.logout();
        setIsAuthenticated(false);
        setUserEmail('');
    };

    // Ekran ładowania (aby uniknąć "mignięcia" strony logowania u zalogowanych osób)
    if (isLoading) {
        return <div style={{ padding: '2rem' }}>Ładowanie aplikacji...</div>;
    }

    return (
        <>
            {!isAuthenticated ? (
                // WIDOK DLA NIEZALOGOWANYCH (Tylko logowanie)
                <Routes>
                    <Route path="/login" element={<Login onLoginSuccess={handleLoginSuccess} />} />
                    <Route path="*" element={<Navigate to="/login" replace />} />
                </Routes>
            ) : (
                // WIDOK DLA ZALOGOWANYCH (Główna aplikacja Chrono2)
                <div className="app-container">
                    <Sidebar />
                    <div className="app-content">

                        {/* Przekazujemy e-mail i funkcję wylogowania do paska górnego */}
                        <Topbar userEmail={userEmail} onLogout={handleLogout} />

                        <Routes>
                            <Route path="/" element={<Dashboard />} />
                            <Route path="/my-requests" element={<MyRequests />} />
                            <Route path="/my-requests/:id" element={<RequestDetail />} />
                            <Route path="/raporty" element={<div style={{padding: '2rem'}}>Raporty (w budowie)</div>} />
                            <Route path="/grafik" element={<Grafik />} />
                            <Route path="/ustawienia" element={<div style={{padding: '2rem'}}>Ustawienia (w budowie)</div>} />

                            {/* Zabezpieczenie: jeśli ktoś wpisze zły adres, wróć do głównego panelu */}
                            <Route path="*" element={<Navigate to="/" replace />} />
                        </Routes>
                    </div>
                </div>
            )}
        </>
    );
}

export default App;
