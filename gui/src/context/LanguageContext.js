import React, { createContext, useState, useEffect, useContext } from 'react';
import translations from './translations';

const LanguageContext = createContext();

export const LanguageProvider = ({ children }) => {
    const [language, setLanguage] = useState(() => {
        return localStorage.getItem('app-language') || 'pl';
    });

    useEffect(() => {
        localStorage.setItem('app-language', language);
        document.documentElement.lang = language;
    }, [language]);

    const t = (key, defaultValue = key) => {
        const keys = key.split('.');
        let value = translations[language];

        for (const k of keys) {
            if (value && typeof value === 'object') {
                value = value[k];
            } else {
                return defaultValue;
            }
        }

        return value || defaultValue;
    };

    const changeLanguage = (lang) => {
        if (['pl', 'en'].includes(lang)) {
            setLanguage(lang);
        }
    };

    return (
        <LanguageContext.Provider value={{ language, changeLanguage, t }}>
            {children}
        </LanguageContext.Provider>
    );
};

export const useLanguage = () => {
    const context = useContext(LanguageContext);
    if (!context) {
        throw new Error('useLanguage musi być używany w obrębie LanguageProvider');
    }
    return context;
};
