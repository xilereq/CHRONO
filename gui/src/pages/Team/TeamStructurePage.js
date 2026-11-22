import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { ChevronsDownUp, ChevronsUpDown, List, Network, Search } from 'lucide-react';
import { userService, delegationService } from '../../services';
import teamService from '../../services/teamService';
import { useLanguage } from '../../context/LanguageContext';
import OrgChart from '../../components/team/OrgChart';
import ContactCard from '../../components/team/ContactCard';
import ContactList from '../../components/team/ContactList';
import {
    ORGANIZATION_ROOT_ID,
    ancestorsOf,
    buildOrganization,
    findMatches,
    flattenTree,
    fullName,
    initiallyExpanded,
} from '../../components/team/teamTreeUtils';
import '../Reports/reports.css';
import './team.css';

const ME = 'me';

const TeamStructurePage = () => {
    const { t } = useLanguage();

    const [role, setRole] = useState(null);
    const [managers, setManagers] = useState([]);
    const [represented, setRepresented] = useState([]);
    const [rootChoice, setRootChoice] = useState(null);
    const [includeInactive, setIncludeInactive] = useState(false);

    const [tree, setTree] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [view, setView] = useState('chart');
    const [query, setQuery] = useState('');
    const [expanded, setExpanded] = useState(() => new Set());
    const [selectedId, setSelectedId] = useState(null);

    const privileged = role === 'ROLE_ADMIN' || role === 'ROLE_HR';

    useEffect(() => {
        const init = async () => {
            let myRole = null;
            try {
                myRole = (await userService.getMyRole()).role;
            } catch {
                myRole = null;
            }
            setRole(myRole);

            if (myRole === 'ROLE_ADMIN' || myRole === 'ROLE_HR') {
                try {
                    setManagers((await userService.getManagers()) || []);
                } catch {
                    setManagers([]);
                }
                setRootChoice(ORGANIZATION_ROOT_ID);
                return;
            }

            if (myRole !== 'ROLE_MANAGER') {
                const active = await delegationService.getActiveReceived();
                setRepresented(active);
                setRootChoice(active.length > 0 ? String(active[0].managerId) : ME);
                return;
            }
            setRootChoice(ME);
        };
        init();
    }, []);

    const loadTree = useCallback(async () => {
        if (rootChoice === null) return;
        setLoading(true);
        setError('');
        try {
            let result;
            if (rootChoice === ORGANIZATION_ROOT_ID) {
                const trees = await Promise.all(managers.map(m =>
                    teamService.getTeamTree({ managerId: m.userID, includeInactive }).catch(() => null)));
                result = buildOrganization(trees, t('team.organization', 'Cała organizacja'));
            } else {
                result = await teamService.getTeamTree({
                    managerId: rootChoice === ME ? null : rootChoice,
                    includeInactive,
                });
            }
            setTree(result);
            setExpanded(initiallyExpanded(result, 2));
            setSelectedId(null);
        } catch (err) {
            setTree(null);
            setError(err.message || t('team.loadError', 'Nie udało się pobrać struktury zespołu.'));
        } finally {
            setLoading(false);
        }
    }, [rootChoice, includeInactive, managers, t]);

    useEffect(() => {
        loadTree();
    }, [loadTree]);

    const people = useMemo(() => flattenTree(tree), [tree]);
    const matches = useMemo(() => new Set(query.trim() ? findMatches(tree, query) : []), [tree, query]);

    useEffect(() => {
        if (matches.size === 0) return;
        const toOpen = ancestorsOf(tree, [...matches]);
        setExpanded(prev => {
            const next = new Set(prev);
            toOpen.forEach(id => next.add(id));
            return next;
        });
    }, [matches, tree]);

    const selected = useMemo(() => people.find(p => p.node.userId === selectedId) || null, [people, selectedId]);

    const toggle = (id) => setExpanded(prev => {
        const next = new Set(prev);
        if (next.has(id)) next.delete(id); else next.add(id);
        return next;
    });

    const expandAll = () => setExpanded(new Set(
        [tree, ...people.map(p => p.node)].filter(n => n?.subordinates?.length).map(n => n.userId)));
    const collapseAll = () => setExpanded(new Set(tree ? [tree.userId] : []));

    const selectPerson = (node) => {
        setSelectedId(node.userId);
        const toOpen = ancestorsOf(tree, [node.userId]);
        setExpanded(prev => new Set([...prev, ...toOpen]));
    };

    const peopleCount = people.length;

    return (
        <div className="page-container team-page">
            <div className="team-header">
                <div>
                    <h2 className="page-title-no-margin">{t('team.title', 'Struktura zespołu')}</h2>
                    <p className="team-subtitle">
                        {t('team.subtitle', 'Hierarchia podległości i dane kontaktowe członków zespołu.')}
                    </p>
                </div>
                <div className="team-view-switch" role="tablist" aria-label={t('team.view', 'Widok')}>
                    <button
                        type="button"
                        role="tab"
                        aria-selected={view === 'chart'}
                        className={`period-tab ${view === 'chart' ? 'active' : ''}`}
                        onClick={() => setView('chart')}
                    >
                        <Network size={15} aria-hidden="true" /> {t('team.viewChart', 'Diagram')}
                    </button>
                    <button
                        type="button"
                        role="tab"
                        aria-selected={view === 'list'}
                        className={`period-tab ${view === 'list' ? 'active' : ''}`}
                        onClick={() => setView('list')}
                    >
                        <List size={15} aria-hidden="true" /> {t('team.viewList', 'Lista kontaktów')}
                    </button>
                </div>
            </div>

            <div className="team-toolbar">
                {privileged && (
                    <select
                        className="team-select"
                        value={rootChoice || ''}
                        onChange={e => setRootChoice(e.target.value)}
                        aria-label={t('team.rootLabel', 'Pokaż strukturę od')}
                    >
                        <option value={ORGANIZATION_ROOT_ID}>{t('team.organization', 'Cała organizacja')}</option>
                        {managers.map(m => (
                            <option key={m.userID} value={String(m.userID)}>
                                {t('team.teamOf', 'Zespół:')} {fullName(m)}
                            </option>
                        ))}
                    </select>
                )}

                {!privileged && represented.length > 0 && (
                    <select
                        className="team-select"
                        value={rootChoice || ''}
                        onChange={e => setRootChoice(e.target.value)}
                        aria-label={t('team.rootLabel', 'Pokaż strukturę od')}
                    >
                        {represented.map(d => (
                            <option key={d.managerId} value={String(d.managerId)}>
                                {t('team.teamOf', 'Zespół:')} {d.managerName}
                            </option>
                        ))}
                        <option value={ME}>{t('team.myPosition', 'Moja pozycja')}</option>
                    </select>
                )}

                <label className="report-search team-search">
                    <Search size={16} aria-hidden="true" />
                    <input
                        type="search"
                        value={query}
                        onChange={e => setQuery(e.target.value)}
                        placeholder={t('team.search', 'Szukaj osoby lub e-maila…')}
                        aria-label={t('team.search', 'Szukaj osoby lub e-maila…')}
                    />
                </label>

                {view === 'chart' && query.trim() && (
                    <span className="team-match-count" aria-live="polite">
                        {t('team.matches', 'Znaleziono: {n}').replace('{n}', matches.size)}
                    </span>
                )}

                <label className="team-checkbox">
                    <input
                        type="checkbox"
                        checked={includeInactive}
                        onChange={e => setIncludeInactive(e.target.checked)}
                    />
                    {t('team.showInactive', 'Pokaż nieaktywnych')}
                </label>

                {view === 'chart' && tree && (
                    <div className="team-expand-actions">
                        <button type="button" className="team-icon-btn" onClick={expandAll}>
                            <ChevronsUpDown size={15} aria-hidden="true" /> {t('team.expandAll', 'Rozwiń wszystko')}
                        </button>
                        <button type="button" className="team-icon-btn" onClick={collapseAll}>
                            <ChevronsDownUp size={15} aria-hidden="true" /> {t('team.collapseAll', 'Zwiń wszystko')}
                        </button>
                    </div>
                )}
            </div>

            {loading ? (
                <div className="report-table-state">{t('team.loading', 'Ładowanie struktury…')}</div>
            ) : error ? (
                <div className="alert alert-error" role="alert">{error}</div>
            ) : peopleCount === 0 && (!tree || !tree.subordinates?.length) && tree?.userId === ORGANIZATION_ROOT_ID ? (
                <div className="report-table-state">{t('team.empty', 'Brak danych o strukturze zespołu.')}</div>
            ) : (
                <div className={`team-content ${selected ? 'team-content--with-card' : ''}`}>
                    <div className="team-main">
                        {view === 'chart' ? (
                            <OrgChart
                                root={tree}
                                expanded={expanded}
                                onToggle={toggle}
                                selectedId={selectedId}
                                onSelect={selectPerson}
                                matches={matches}
                                focusId={matches.size > 0 ? [...matches][0] : selectedId}
                            />
                        ) : (
                            <ContactList
                                people={people}
                                query={query}
                                selectedId={selectedId}
                                onSelect={selectPerson}
                            />
                        )}
                        {tree && tree.userId !== ORGANIZATION_ROOT_ID && !tree.subordinates?.length && (
                            <p className="team-hint">{t('team.noSubordinates', 'Ta osoba nie ma podwładnych.')}</p>
                        )}
                    </div>
                    {selected && (
                        <ContactCard
                            node={selected.node}
                            superior={selected.superior}
                            onClose={() => setSelectedId(null)}
                            onSelectPerson={selectPerson}
                        />
                    )}
                </div>
            )}
        </div>
    );
};

export default TeamStructurePage;
