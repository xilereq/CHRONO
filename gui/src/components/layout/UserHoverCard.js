import React, { useState, useRef } from 'react';
import { Mail, Briefcase, Building2 } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';
import './layout.css';

const UserHoverCard = ({
                           user,
                           children,
                           placement = 'top' // 'top' | 'bottom'
                       }) => {
    const { t } = useLanguage();
    const [isVisible, setIsVisible] = useState(false);
    const timeoutRef = useRef(null);

    if (!user) return <>{children}</>;

    const {
        name,
        position,
        email = '',
        department = '',
        avatarUrl = null
    } = user;

    const displayName = name || t('userHoverCard.noData', 'Brak danych');
    const displayPosition = position || t('userHoverCard.noPosition', 'Stanowisko nieokreślone');

    // Generowanie inicjałów jeśli brak zdjęcia awatara
    const getInitials = (fullName) => {
        if (!fullName || fullName === t('userHoverCard.noData', 'Brak danych')) return '?';
        const parts = fullName.trim().split(' ');
        if (parts.length >= 2) {
            return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
        }
        return fullName.slice(0, 2).toUpperCase();
    };

    const handleMouseEnter = () => {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = setTimeout(() => {
            setIsVisible(true);
        }, 180);
    };

    const handleMouseLeave = () => {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = setTimeout(() => {
            setIsVisible(false);
        }, 120);
    };

    return (
        <span
            className="hover-card-trigger-wrapper"
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
        >
            <span className="hover-card-trigger">
                {children || displayName}
            </span>

            {isVisible && (
                <div
                    className={`hover-card-popover placement-${placement}`}
                    role="tooltip"
                    onMouseEnter={() => clearTimeout(timeoutRef.current)}
                    onMouseLeave={handleMouseLeave}
                >
                    <div className="hover-card-header">
                        {avatarUrl ? (
                            <img src={avatarUrl} alt={displayName} className="hover-card-avatar" />
                        ) : (
                            <div className="hover-card-avatar initials">
                                {getInitials(displayName)}
                            </div>
                        )}
                        <div className="hover-card-name-group">
                            <span className="hover-card-name">{displayName}</span>
                            <span className="hover-card-position">
                                <Briefcase size={12} className="inline-icon" />
                                {displayPosition}
                            </span>
                        </div>
                    </div>

                    <div className="hover-card-body">
                        {department && (
                            <div className="hover-card-info-row">
                                <Building2 size={13} className="info-icon" />
                                <span className="hover-card-dept">{department}</span>
                            </div>
                        )}

                        {email && (
                            <div className="hover-card-info-row">
                                <Mail size={13} className="info-icon" />
                                <a
                                    href={`mailto:${email}`}
                                    className="hover-card-email"
                                    onClick={(e) => e.stopPropagation()}
                                >
                                    {email}
                                </a>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </span>
    );
};

export default UserHoverCard;
