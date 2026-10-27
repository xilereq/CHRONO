import React from 'react';
import { Moon, Sun } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../context/LanguageContext';
import './ui.css';

const ThemeToggle = () => {
    const { theme, toggleTheme } = useTheme();
    const { t } = useLanguage();
    const isDark = theme === 'dark';
    const label = isDark
        ? t('topbar.switchToLight', 'Przełącz na jasny motyw')
        : t('topbar.switchToDark', 'Przełącz na ciemny motyw');

    return (
        <button type="button" className="theme-toggle-btn" onClick={toggleTheme} aria-label={label} title={label}>
            {isDark ? <Sun size={18} /> : <Moon size={18} />}
        </button>
    );
};

export default ThemeToggle;
