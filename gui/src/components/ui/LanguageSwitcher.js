import React from 'react';
import { Globe } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';
import '../layout/layout.css';

export const LANGUAGES = [
    { code: 'pl', label: 'Polski' },
    { code: 'en', label: 'English' },
    { code: 'de', label: 'Deutsch' },
];

const LanguageSwitcher = () => {
    const { t, language, changeLanguage } = useLanguage();

    return (
        <div className="language-selector-pill">
            <Globe size={18} aria-hidden="true" />
            <select
                className="language-selector-select"
                value={language}
                onChange={(e) => changeLanguage(e.target.value)}
                aria-label={t('topbar.language', 'Język')}
            >
                {LANGUAGES.map(({ code, label }) => (
                    <option key={code} value={code}>{label}</option>
                ))}
            </select>
        </div>
    );
};

export default LanguageSwitcher;
