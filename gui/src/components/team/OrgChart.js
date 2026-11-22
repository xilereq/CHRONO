import React, { useEffect, useRef } from 'react';
import { Building2, ChevronDown, ChevronRight } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';
import { ORGANIZATION_ROOT_ID, fullName, initials, primaryRole } from './teamTreeUtils';

const OrgNode = ({ node, expanded, onToggle, selectedId, onSelect, matches }) => {
    const { t } = useLanguage();
    const children = node.subordinates || [];
    const hasChildren = children.length > 0;
    const isOpen = expanded.has(node.userId);
    const isOrganization = node.userId === ORGANIZATION_ROOT_ID;
    const role = primaryRole(node);

    const classes = [
        'org-card',
        isOrganization ? 'org-card--organization' : '',
        selectedId === node.userId ? 'org-card--selected' : '',
        matches.has(node.userId) ? 'org-card--match' : '',
        node.active === false ? 'org-card--inactive' : '',
    ].filter(Boolean).join(' ');

    return (
        <li>
            <div className="org-node">
                <button
                    type="button"
                    className={classes}
                    onClick={() => (isOrganization ? onToggle(node.userId) : onSelect(node))}
                    aria-pressed={!isOrganization ? selectedId === node.userId : undefined}
                    data-user-id={node.userId}
                >
                    <span className="org-avatar" aria-hidden="true">
                        {isOrganization ? <Building2 size={18} /> : initials(node)}
                    </span>
                    <span className="org-card-text">
                        <span className="org-card-name">{fullName(node)}</span>
                        {!isOrganization && role && (
                            <span className={`org-role org-role--${role.replace('ROLE_', '').toLowerCase()}`}>
                                {t(`team.roles.${role}`, role)}
                            </span>
                        )}
                        {node.active === false && (
                            <span className="org-inactive-label">{t('team.inactive', 'Nieaktywny')}</span>
                        )}
                    </span>
                </button>

                {hasChildren && (
                    <button
                        type="button"
                        className="org-toggle"
                        onClick={() => onToggle(node.userId)}
                        aria-expanded={isOpen}
                        aria-label={`${isOpen ? t('team.collapse', 'Zwiń') : t('team.expand', 'Rozwiń')}: ${fullName(node)}`}
                    >
                        {isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                        {node.directSubordinates ?? children.length}
                    </button>
                )}
            </div>

            {hasChildren && isOpen && (
                <ul>
                    {children.map(child => (
                        <OrgNode
                            key={child.userId}
                            node={child}
                            expanded={expanded}
                            onToggle={onToggle}
                            selectedId={selectedId}
                            onSelect={onSelect}
                            matches={matches}
                        />
                    ))}
                </ul>
            )}
        </li>
    );
};

const OrgChart = ({ root, expanded, onToggle, selectedId, onSelect, matches = new Set(), focusId = null }) => {
    const containerRef = useRef(null);

    useEffect(() => {
        const container = containerRef.current;
        if (container) container.scrollLeft = (container.scrollWidth - container.clientWidth) / 2;
    }, [root]);

    const pendingFocus = useRef(null);

    useEffect(() => {
        pendingFocus.current = focusId;
    }, [focusId]);

    useEffect(() => {
        const id = pendingFocus.current;
        if (id === null || id === undefined) return;
        const target = containerRef.current?.querySelector(`[data-user-id="${id}"]`);
        if (!target) return;
        target.scrollIntoView?.({ behavior: 'smooth', block: 'nearest', inline: 'center' });
        pendingFocus.current = null;
    }, [focusId, expanded]);

    if (!root) return null;
    return (
        <div className="org-chart" ref={containerRef}>
            <ul className="org-tree">
                <OrgNode
                    node={root}
                    expanded={expanded}
                    onToggle={onToggle}
                    selectedId={selectedId}
                    onSelect={onSelect}
                    matches={matches}
                />
            </ul>
        </div>
    );
};

export default OrgChart;
