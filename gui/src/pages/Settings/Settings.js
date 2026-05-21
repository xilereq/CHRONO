import React from 'react';
import { Card, Button } from '../../components';
import { useTheme } from '../../context/ThemeContext';
import '../main.css';

const Settings = () => {
    const { theme, toggleTheme } = useTheme();

    return (
        <div className="page-container">
            <h2 className="page-title-no-margin">Ustawienia</h2>
            <Card title="Wygląd">
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <span style={{ color: 'var(--text-main)', width: '200px' }}>
                        Aktualny motyw: {theme === 'light' ? 'Jasny' : 'Ciemny'}
                    </span>
                    <Button onClick={toggleTheme} style={{ width: 'auto' }}>
                        Zmień motyw
                    </Button>
                </div>
            </Card>
        </div>
    );
};

export default Settings;
