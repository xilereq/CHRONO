import React from 'react';
import { Sidebar, Topbar } from './components';
import { Grafik, Dashboard, MyRequests, RequestDetail, Raporty } from './pages';
import { Routes, Route } from 'react-router-dom';
import './index.css';

const App = () => {
    return (
        <div className="app-container">
            <Sidebar />
            <div className="app-content">
                <Topbar />
                <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/my-requests" element={<MyRequests />} />
                    <Route path="/my-requests/:id" element={<RequestDetail />} />
                    <Route path="/raporty" element={<Raporty />} />
                    <Route path="/grafik" element={<Grafik />} />
                    <Route path="/ustawienia" element={<div style={{padding: '2rem'}}>Ustawienia (w budowie)</div>} />
                </Routes>
            </div>
        </div>
    );
}

export default App;