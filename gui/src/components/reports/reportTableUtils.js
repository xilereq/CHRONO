export const PAGE_SIZES = [10, 25, 50, 100];
export const MAX_FILTER_OPTIONS = 15;

const pad = (n) => String(n).padStart(2, '0');

export const formatDuration = (seconds) => {
    if (seconds === null || seconds === undefined || seconds === '') return '–';
    const total = Number(seconds);
    if (Number.isNaN(total)) return String(seconds);
    const abs = Math.abs(Math.round(total));
    const sign = total < 0 ? '-' : '';
    return `${sign}${Math.floor(abs / 3600)}:${pad(Math.floor((abs % 3600) / 60))}`;
};

export const formatTime = (value) => {
    if (value === null || value === undefined || value === '') return '–';
    if (Array.isArray(value)) return `${pad(value[0] ?? 0)}:${pad(value[1] ?? 0)}`;
    return String(value).slice(0, 5);
};

export const formatDate = (value) => {
    if (value === null || value === undefined || value === '') return '–';
    if (Array.isArray(value)) return `${value[0]}-${pad(value[1])}-${pad(value[2])}`;
    return String(value);
};

export const formatCell = (value, type, translateValue = (v) => v) => {
    switch (type) {
        case 'DURATION': return formatDuration(value);
        case 'TIME':     return formatTime(value);
        case 'DATE':     return formatDate(value);
        case 'NUMBER':   return value === null || value === undefined ? '–' : String(value);
        default:         return value === null || value === undefined || value === '' ? '–' : String(translateValue(value));
    }
};

const sortKey = (value, type) => {
    if (value === null || value === undefined || value === '') return null;
    switch (type) {
        case 'DURATION':
        case 'NUMBER': {
            const n = Number(value);
            return Number.isNaN(n) ? null : n;
        }
        case 'DATE': return formatDate(value);
        case 'TIME': return formatTime(value);
        default:     return String(value);
    }
};

export const compareCells = (a, b, type, locale = 'pl') => {
    const ka = sortKey(a, type);
    const kb = sortKey(b, type);
    if (ka === null && kb === null) return 0;
    if (ka === null) return 1;
    if (kb === null) return -1;
    if (typeof ka === 'number' && typeof kb === 'number') return ka - kb;
    return String(ka).localeCompare(String(kb), locale, { numeric: true, sensitivity: 'base' });
};

export const filterableColumns = (columns, rows) =>
    columns
        .map((column, index) => ({ column, index }))
        .filter(({ column, index }) => {
            if (column.type !== 'TEXT') return false;
            const distinct = new Set(rows.map(r => r[index]).filter(v => v !== null && v !== undefined && v !== ''));
            return distinct.size >= 2 && distinct.size <= MAX_FILTER_OPTIONS && distinct.size < rows.length;
        })
        .map(({ index }) => index);

export const distinctValues = (rows, index) =>
    [...new Set(rows.map(r => r[index]).filter(v => v !== null && v !== undefined && v !== ''))];

export const applyTableState = ({ columns, rows, query, columnFilters, sort, locale, translateValue }) => {
    const needle = query.trim().toLocaleLowerCase(locale);

    let result = rows.filter(row =>
        Object.entries(columnFilters).every(([index, value]) => !value || String(row[index]) === value));

    if (needle) {
        result = result.filter(row => row.some((cell, i) =>
            formatCell(cell, columns[i]?.type, translateValue).toLocaleLowerCase(locale).includes(needle)));
    }

    if (sort && sort.index !== null && sort.index !== undefined) {
        const type = columns[sort.index]?.type;
        const direction = sort.direction === 'desc' ? -1 : 1;
        result = result
            .map((row, position) => ({ row, position }))
            .sort((x, y) => {
                const byValue = compareCells(x.row[sort.index], y.row[sort.index], type, locale);
                if (byValue === 0) return x.position - y.position;
                const nullFirst = x.row[sort.index] === null || x.row[sort.index] === undefined;
                const nullSecond = y.row[sort.index] === null || y.row[sort.index] === undefined;
                return nullFirst || nullSecond ? byValue : byValue * direction;
            })
            .map(({ row }) => row);
    }

    return result;
};

export const nextSort = (sort, index) => {
    if (!sort || sort.index !== index) return { index, direction: 'asc' };
    if (sort.direction === 'asc') return { index, direction: 'desc' };
    return { index: null, direction: 'asc' };
};
