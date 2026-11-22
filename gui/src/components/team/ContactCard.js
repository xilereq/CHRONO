import React, { useState, useEffect } from 'react';
import { Check, Copy, Mail, X } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';
import { fullName, initials } from './teamTreeUtils';

const copyText = async (text) => {
    if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(text);
        return;
    }
    const area = document.createElement('textarea');
    area.value = text;
    area.setAttribute('readonly', '');
    area.style.position = 'absolute';
    area.style.left = '-9999px';
    document.body.appendChild(area);
    area.select();
    document.execCommand('copy');
    document.body.removeChild(area);
};

const ContactCard = ({ node, superior, onClose, onSelectPerson }) => {
    const { t } = useLanguage();
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        setCopied(false);
    }, [node]);

    if (!node) return null;

    const handleCopy = async () => {
        try {
            await copyText(node.email);
            setCopied(true);
        } catch {
            setCopied(false);
        }
    };

    return (
        <aside className="contact-card" aria-label={t('team.contactCard', 'Karta kontaktu')}>
            <button type="button" className="contact-card-close" onClick={onClose} aria-label={t('common.close', 'Zamknij')}>
                <X size={16} />
            </button>

            <div className="contact-card-head">
                <span className="org-avatar org-avatar--large" aria-hidden="true">{initials(node)}</span>
                <div>
                    <h3 className="contact-card-name">{fullName(node)}</h3>
                    <div className="contact-card-roles">
                        {(node.roles || []).map(role => (
                            <span key={role} className={`org-role org-role--${role.replace('ROLE_', '').toLowerCase()}`}>
                                {t(`team.roles.${role}`, role)}
                            </span>
                        ))}
                        {node.active === false && (
                            <span className="org-inactive-label">{t('team.inactive', 'Nieaktywny')}</span>
                        )}
                    </div>
                </div>
            </div>

            <dl className="contact-card-details">
                <div>
                    <dt>{t('team.email', 'E-mail')}</dt>
                    <dd>
                        {node.email ? (
                            <div className="contact-email">
                                <a href={`mailto:${node.email}`} className="contact-email-link">
                                    <Mail size={14} aria-hidden="true" /> {node.email}
                                </a>
                                <button type="button" className="contact-copy-btn" onClick={handleCopy}>
                                    {copied ? <Check size={14} aria-hidden="true" /> : <Copy size={14} aria-hidden="true" />}
                                    {copied ? t('team.copied', 'Skopiowano') : t('team.copy', 'Kopiuj')}
                                </button>
                            </div>
                        ) : '–'}
                    </dd>
                </div>
                <div>
                    <dt>{t('team.superior', 'Przełożony')}</dt>
                    <dd>
                        {superior ? (
                            <button type="button" className="contact-link-btn" onClick={() => onSelectPerson?.(superior)}>
                                {fullName(superior)}
                            </button>
                        ) : '–'}
                    </dd>
                </div>
                <div>
                    <dt>{t('team.directSubordinates', 'Bezpośredni podwładni')}</dt>
                    <dd>{node.directSubordinates ?? 0}</dd>
                </div>
                <div>
                    <dt>{t('team.totalSubordinates', 'Wszyscy podwładni')}</dt>
                    <dd>{node.totalSubordinates ?? 0}</dd>
                </div>
            </dl>
        </aside>
    );
};

export default ContactCard;
