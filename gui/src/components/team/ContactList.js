import React, { useState, useMemo, useEffect } from 'react';
import { ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight, Mail } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';
import { PAGE_SIZES, compareCells, nextSort } from '../reports/reportTableUtils';
import { fullName, primaryRole } from './teamTreeUtils';

const ContactList = ({ people, query, selectedId, onSelect }) => {
    const { t, language } = useLanguage();
    const [sort, setSort] = useState({ index: 0, direction: 'asc' });
    const [pageSize, setPageSize] = useState(PAGE_SIZES[1]);
    const [page, setPage] = useState(1);

    const columns = useMemo(() => [
        { key: 'name', label: t('team.columnName', 'Imię i nazwisko'), type: 'TEXT', value: p => fullName(p.node) },
        { key: 'role', label: t('team.columnRole', 'Rola'), type: 'TEXT', value: p => {
            const role = primaryRole(p.node);
            return role ? t(`team.roles.${role}`, role) : '';
        } },
        { key: 'superior', label: t('team.superior', 'Przełożony'), type: 'TEXT', value: p => (p.superior ? fullName(p.superior) : '') },
        { key: 'email', label: t('team.email', 'E-mail'), type: 'TEXT', value: p => p.node.email || '' },
        { key: 'subordinates', label: t('team.columnSubordinates', 'Podwładni'), type: 'NUMBER', value: p => p.node.directSubordinates ?? 0 },
    ], [t]);

    const rows = useMemo(() => {
        const needle = query.trim().toLocaleLowerCase(language);
        const filtered = needle
            ? people.filter(p => columns.some(c => String(c.value(p)).toLocaleLowerCase(language).includes(needle)))
            : people;
        if (sort.index === null) return filtered;
        const column = columns[sort.index];
        const direction = sort.direction === 'desc' ? -1 : 1;
        return [...filtered].sort((a, b) => direction * compareCells(column.value(a), column.value(b), column.type, language));
    }, [people, query, sort, columns, language]);

    useEffect(() => {
        setPage(1);
    }, [query, people, pageSize]);

    const pageCount = Math.max(1, Math.ceil(rows.length / pageSize));
    const currentPage = Math.min(page, pageCount);
    const first = (currentPage - 1) * pageSize;
    const pageRows = rows.slice(first, first + pageSize);

    const sortIcon = (index) => {
        if (sort.index !== index) return <ArrowUpDown size={14} aria-hidden="true" className="report-sort-icon report-sort-icon--idle" />;
        return sort.direction === 'asc'
            ? <ArrowUp size={14} aria-hidden="true" className="report-sort-icon" />
            : <ArrowDown size={14} aria-hidden="true" className="report-sort-icon" />;
    };

    return (
        <div className="contact-list">
            <div className="report-table-scroll">
                <table className="report-data-table">
                    <thead>
                        <tr>
                            {columns.map((column, index) => (
                                <th
                                    key={column.key}
                                    className={`report-col--${column.type.toLowerCase()}`}
                                    aria-sort={sort.index === index ? (sort.direction === 'asc' ? 'ascending' : 'descending') : 'none'}
                                >
                                    <button type="button" className="report-sort-btn" onClick={() => setSort(prev => nextSort(prev, index))}>
                                        {column.label}
                                        {sortIcon(index)}
                                    </button>
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {pageRows.length === 0 ? (
                            <tr>
                                <td colSpan={columns.length} className="report-table-state">
                                    {t('team.noResults', 'Brak osób spełniających kryteria.')}
                                </td>
                            </tr>
                        ) : pageRows.map(person => (
                            <tr
                                key={person.node.userId}
                                className={selectedId === person.node.userId ? 'contact-row--selected' : undefined}
                            >
                                <td>
                                    <button type="button" className="contact-link-btn" onClick={() => onSelect(person.node)}>
                                        {fullName(person.node)}
                                    </button>
                                    {person.node.active === false && (
                                        <span className="org-inactive-label">{t('team.inactive', 'Nieaktywny')}</span>
                                    )}
                                </td>
                                <td>{columns[1].value(person) || '–'}</td>
                                <td>{columns[2].value(person) || '–'}</td>
                                <td>
                                    {person.node.email ? (
                                        <a href={`mailto:${person.node.email}`} className="contact-email-link">
                                            <Mail size={14} aria-hidden="true" /> {person.node.email}
                                        </a>
                                    ) : '–'}
                                </td>
                                <td className="report-col--number">{person.node.directSubordinates ?? 0}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <div className="report-pagination">
                <label className="report-page-size">
                    {t('reports.tableRowsPerPage', 'Wierszy na stronie:')}
                    <select className="report-page-size-select" value={pageSize} onChange={e => setPageSize(Number(e.target.value))}>
                        {PAGE_SIZES.map(size => <option key={size} value={size}>{size}</option>)}
                    </select>
                </label>
                <span className="report-page-info">
                    {rows.length === 0 ? '0' : `${first + 1}–${Math.min(first + pageSize, rows.length)}`}
                    {' '}{t('reports.tableOf', 'z')} {rows.length}
                </span>
                <div className="report-page-nav">
                    <button type="button" className="report-page-btn" onClick={() => setPage(currentPage - 1)}
                        disabled={currentPage <= 1} aria-label={t('reports.tablePrev', 'Poprzednia strona')}>
                        <ChevronLeft size={16} />
                    </button>
                    <span className="report-page-current">{currentPage} / {pageCount}</span>
                    <button type="button" className="report-page-btn" onClick={() => setPage(currentPage + 1)}
                        disabled={currentPage >= pageCount} aria-label={t('reports.tableNext', 'Następna strona')}>
                        <ChevronRight size={16} />
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ContactList;
