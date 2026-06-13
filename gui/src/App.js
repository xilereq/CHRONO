import React, { useState, useEffect } from 'react';
import { Sidebar, Topbar } from './components';
import { Grafik, Dashboard, Requests, RequestDetail, Policies, Raporty, Login, ForgotPasswordRequest, ForgotPasswordReset, Settings, Logs, UserManagementPage, ManagerSchedulePage } from './pages';
import { Routes, Route, Navigate } from 'react-router-dom';
import { authService, userService } from './services';
import './index.css';

const App = () => {
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [userEmail, setUserEmail] = useState('');
    const [isAdmin, setIsAdmin] = useState(false);
    const [isHr, setIsHr] = useState(false);
    const [isManager, setIsManager] = useState(false);

    useEffect(() => {
        const initApp = async () => {
            const authStatus = authService.isAuthenticated();
            setIsAuthenticated(authStatus);

            if (authStatus) {
                setUserEmail(authService.getUserEmail());
                try {
                    const response = await userService.getMyRole();
                    setIsHr(response.role === "ROLE_HR");
                    setIsAdmin(response.role === "ROLE_ADMIN");
                    setIsManager(response.role === "ROLE_MANAGER");
                } catch (error) {
                    console.error("Błąd weryfikacji roli:", error);
                }
            }
            setIsLoading(false);
        };

        initApp();
    }, []);

    const handleLoginSuccess = async () => {
        setIsAuthenticated(true);
        setUserEmail(authService.getUserEmail());
        try {
            const response = await userService.getMyRole();
            setIsAdmin(response.role === "ROLE_ADMIN");
        } catch (error) {
            console.error("Błąd weryfikacji roli po logowaniu:", error);
        }
    };

    const handleLogout = () => {
        authService.logout();
        setIsAuthenticated(false);
        setUserEmail('');
        setIsAdmin(false);
        setIsHr(false);
        setIsManager(false);
    };

    if (isLoading) {
        return <div style={{ padding: '2rem' }}>Ładowanie aplikacji...</div>;
    }

    return (
        <>
            {!isAuthenticated ? (
                <Routes>
                    <Route path="/login" element={<Login onLoginSuccess={handleLoginSuccess} />} />
                    <Route path="/forgot-password" element={<ForgotPasswordRequest />} />
                    <Route path="/reset-password" element={<ForgotPasswordReset />} />
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
                            <Route path="/ustawienia" element={<Settings />} />
                            {(isHr || isAdmin ) && (
                                <Route path="/users" element={<UserManagementPage />} />
                            )}
                            {(isManager || isAdmin) && (
                                <Route path="/manager" element={<ManagerSchedulePage />} />
                            )}
                            {isAdmin && (
                                <>
                                    <Route path="/policies" element={<Policies />} />
                                    <Route path="/logs" element={<Logs />} />
                                </>
                            )}

                            <Route path="*" element={<Navigate to="/" replace />} />
                        </Routes>
                    </div>
                </div>
            )}
        </>
    );
}

export default App;
