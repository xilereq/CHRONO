import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight, Search, X } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';
import {
    PAGE_SIZES,
    applyTableState,
    distinctValues,
    filterableColumns,
    formatCell,
    nextSort,
} from './reportTableUtils';

const NO_SORT = { index: null, direction: 'asc' };

const ReportTable = ({ report, loading = false, error = '' }) => {
    const { t, language } = useLanguage();

    const [query, setQuery] = useState('');
    const [columnFilters, setColumnFilters] = useState({});
    const [sort, setSort] = useState(NO_SORT);
    const [pageSize, setPageSize] = useState(PAGE_SIZES[0]);
    const [page, setPage] = useState(1);

    const columns = useMemo(() => report?.columns || [], [report]);
    const rows = useMemo(() => report?.rows || [], [report]);

    const label = useCallback((text) => t(`reports.columnLabels.${text}`, text), [t]);
    const translateValue = useCallback((value) => t(`reports.valueLabels.${value}`, value), [t]);

    useEffect(() => {
        setQuery('');
        setColumnFilters({});
        setSort(NO_SORT);
        setPage(1);
    }, [report]);

    const filterIndexes = useMemo(() => filterableColumns(columns, rows), [columns, rows]);

    const visibleRows = useMemo(
        () => applyTableState({ columns, rows, query, columnFilters, sort, locale: language, translateValue }),
        [columns, rows, query, columnFilters, sort, language, translateValue]
    );

    const pageCount = Math.max(1, Math.ceil(visibleRows.length / pageSize));
    const currentPage = Math.min(page, pageCount);
    const firstIndex = (currentPage - 1) * pageSize;
    const pageRows = visibleRows.slice(firstIndex, firstIndex + pageSize);

    const filtersActive = query.trim() !== '' || Object.values(columnFilters).some(Boolean);

    const resetFilters = () => {
        setQuery('');
        setColumnFilters({});
        setPage(1);
    };

    if (loading) {
        return <div className="report-table-state">{t('reports.loading', 'Ładowanie danych…')}</div>;
    }
    if (error) {
        return <div className="alert alert-error" role="alert">{error}</div>;
    }
    if (!report) {
        return null;
    }

    const sortIcon = (index) => {
        if (sort.index !== index) return <ArrowUpDown size={14} aria-hidden="true" className="report-sort-icon report-sort-icon--idle" />;
        return sort.direction === 'asc'
            ? <ArrowUp size={14} aria-hidden="true" className="report-sort-icon" />
            : <ArrowDown size={14} aria-hidden="true" className="report-sort-icon" />;
    };

    return (
        <div className="report-table">
            <div className="report-table-header">
                <h3 className="report-table-title">{t(`reports.reportTitles.${report.title}`, report.title)}</h3>
                {report.fields?.length > 0 && (
                    <dl className="report-summary">
                        {report.fields.map(field => (
                            <div key={field.label} className="report-summary-item">
                                <dt>{label(field.label)}</dt>
                                <dd className={field.type === 'DURATION' && Number(field.value) < 0 ? 'report-negative' : undefined}>
                                    {formatCell(field.value, field.type, translateValue)}
                                </dd>
                            </div>
                        ))}
                    </dl>
                )}
            </div>

            {rows.length === 0 ? (
                <div className="report-table-state">{t('reports.tableEmpty', 'Raport nie zawiera danych dla wybranego okresu.')}</div>
            ) : (
                <>
                    <div className="report-toolbar">
                        <label className="report-search">
                            <Search size={16} aria-hidden="true" />
                            <input
                                type="search"
                                value={query}
                                onChange={e => { setQuery(e.target.value); setPage(1); }}
                                placeholder={t('reports.tableSearch', 'Szukaj w raporcie…')}
                                aria-label={t('reports.tableSearch', 'Szukaj w raporcie…')}
                            />
                        </label>

                        {filterIndexes.map(index => (
                            <select
                                key={index}
                                className="filter-select report-column-filter"
                                value={columnFilters[index] || ''}
                                onChange={e => { setColumnFilters(prev => ({ ...prev, [index]: e.target.value })); setPage(1); }}
                                aria-label={label(columns[index].label)}
                            >
                                <option value="">{label(columns[index].label)}: {t('reports.tableAll', 'wszystkie')}</option>
                                {distinctValues(rows, index).map(value => (
                                    <option key={value} value={String(value)}>{translateValue(value)}</option>
                                ))}
                            </select>
                        ))}

                        {filtersActive && (
                            <button type="button" className="report-reset-btn" onClick={resetFilters}>
                                <X size={14} aria-hidden="true" /> {t('reports.tableResetFilters', 'Wyczyść filtry')}
                            </button>
                        )}

                        <span className="report-count" aria-live="polite">
                            {filtersActive
                                ? t('reports.tableCountFiltered', '{shown} z {total} wierszy')
                                    .replace('{shown}', visibleRows.length).replace('{total}', rows.length)
                                : t('reports.tableCount', '{total} wierszy').replace('{total}', rows.length)}
                        </span>
                    </div>

                    <div className="report-table-scroll">
                        <table className="report-data-table">
                            <thead>
                                <tr>
                                    {columns.map((column, index) => (
                                        <th
                                            key={`${column.label}-${index}`}
                                            className={`report-col--${column.type.toLowerCase()}`}
                                            aria-sort={sort.index === index ? (sort.direction === 'asc' ? 'ascending' : 'descending') : 'none'}
                                        >
                                            <button
                                                type="button"
                                                className="report-sort-btn"
                                                onClick={() => { setSort(prev => nextSort(prev, index)); setPage(1); }}
                                                title={t('reports.tableSortHint', 'Kliknij, aby sortować')}
                                            >
                                                {label(column.label)}
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
                                            {t('reports.tableNoResults', 'Brak wierszy spełniających kryteria.')}
                                        </td>
                                    </tr>
                                ) : pageRows.map((row, rowIndex) => (
                                    <tr key={firstIndex + rowIndex}>
                                        {columns.map((column, index) => (
                                            <td
                                                key={index}
                                                className={[
                                                    `report-col--${column.type.toLowerCase()}`,
                                                    column.type === 'DURATION' && Number(row[index]) < 0 ? 'report-negative' : '',
                                                ].join(' ').trim()}
                                            >
                                                {formatCell(row[index], column.type, translateValue)}
                                            </td>
                                        ))}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <div className="report-pagination">
                        <label className="report-page-size">
                            {t('reports.tableRowsPerPage', 'Wierszy na stronie:')}
                            <select
                                className="report-page-size-select"
                                value={pageSize}
                                onChange={e => { setPageSize(Number(e.target.value)); setPage(1); }}
                            >
                                {PAGE_SIZES.map(size => <option key={size} value={size}>{size}</option>)}
                            </select>
                        </label>

                        <span className="report-page-info">
                            {visibleRows.length === 0
                                ? '0'
                                : `${firstIndex + 1}–${Math.min(firstIndex + pageSize, visibleRows.length)}`}
                            {' '}{t('reports.tableOf', 'z')} {visibleRows.length}
                        </span>

                        <div className="report-page-nav">
                            <button
                                type="button"
                                className="report-page-btn"
                                onClick={() => setPage(currentPage - 1)}
                                disabled={currentPage <= 1}
                                aria-label={t('reports.tablePrev', 'Poprzednia strona')}
                            >
                                <ChevronLeft size={16} />
                            </button>
                            <span className="report-page-current">{currentPage} / {pageCount}</span>
                            <button
                                type="button"
                                className="report-page-btn"
                                onClick={() => setPage(currentPage + 1)}
                                disabled={currentPage >= pageCount}
                                aria-label={t('reports.tableNext', 'Następna strona')}
                            >
                                <ChevronRight size={16} />
                            </button>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default ReportTable;
