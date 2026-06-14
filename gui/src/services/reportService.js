const API_URL = '../api/reports';

const getHeaders = () => ({
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const parseTime = (str) => {
    if (!str || str.trim() === '-') return 0;
    const s = str.trim();
    const neg = s.startsWith('-');
    const [h, m] = s.replace('-', '').split(':').map(Number);
    return neg ? -(h * 3600 + (m || 0) * 60) : (h * 3600 + (m || 0) * 60);
};

const parseTimesheetCsv = (text) => {
    const clean = text.startsWith('﻿') ? text.slice(1) : text;
    const lines = clean.split('\n');
    const col = (line) => line.split(';');

    const totalWork     = parseTime(col(lines[2])[1]);
    const totalRequired = parseTime(col(lines[3])[1]);
    const totalBalance  = parseTime(col(lines[4])[1]);
    const totalNight    = parseTime(col(lines[5])[1]);

    const headerIdx = lines.findIndex(l => l.startsWith('Data;'));
    const dailyEntries = [];

    if (headerIdx >= 0) {
        for (let i = headerIdx + 1; i < lines.length; i++) {
            const line = lines[i].trim();
            if (!line) continue;
            const c = line.split(';');
            if (c.length < 7 || !c[0]) continue;
            dailyEntries.push({
                date:            c[0],
                start:           c[1] !== '-' ? c[1] : null,
                end:             c[2] !== '-' ? c[2] : null,
                workSeconds:     parseTime(c[3]),
                requiredSeconds: parseTime(c[4]),
                balanceSeconds:  parseTime(c[5]),
                nightSeconds:    parseTime(c[6]),
                applicationType: c[7]?.trim() || null,
            });
        }
    }

    return { totalWork, totalRequired, totalBalance, totalNight, dailyEntries };
};

const triggerDownload = (blob, filename) => {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
};

const EXT = { CSV: 'csv', PDF: 'pdf', XLSX: 'xlsx' };

const fetchAndDownload = async (url, filename) => {
    const response = await fetch(url, { headers: getHeaders() });
    if (!response.ok) {
        const body = await response.json().catch(() => null);
        throw new Error(body?.message || 'Błąd podczas generowania raportu');
    }
    triggerDownload(await response.blob(), filename);
};

const reportService = {
    fetchMyTimesheetData: async (startDate, endDate) => {
        const response = await fetch(
            `${API_URL}/timesheet/me?startDate=${startDate}&endDate=${endDate}&format=CSV`,
            { headers: getHeaders() }
        );
        if (!response.ok) throw new Error('Nie udało się pobrać danych raportu');
        return parseTimesheetCsv(await response.text());
    },

    downloadMyTimesheet: (startDate, endDate, format) =>
        fetchAndDownload(
            `${API_URL}/timesheet/me?startDate=${startDate}&endDate=${endDate}&format=${format}`,
            `karta_pracy_${startDate}_${endDate}.${EXT[format]}`
        ),

    downloadMyOvertime: (startDate, endDate, format) =>
        fetchAndDownload(
            `${API_URL}/overtime/me?startDate=${startDate}&endDate=${endDate}&format=${format}`,
            `nadgodziny_${startDate}_${endDate}.${EXT[format]}`
        ),

    downloadEmployeeTimesheet: (employeeId, startDate, endDate, format) =>
        fetchAndDownload(
            `${API_URL}/timesheet/employee/${employeeId}?startDate=${startDate}&endDate=${endDate}&format=${format}`,
            `karta_pracy_${employeeId}_${startDate}_${endDate}.${EXT[format]}`
        ),

    downloadTeamWorkload: (startDate, endDate, format) =>
        fetchAndDownload(
            `${API_URL}/workload/team?startDate=${startDate}&endDate=${endDate}&format=${format}`,
            `obciazenie_zespolu_${startDate}_${endDate}.${EXT[format]}`
        ),

    downloadTeamAbsences: (startDate, endDate, format) =>
        fetchAndDownload(
            `${API_URL}/absences/team?startDate=${startDate}&endDate=${endDate}&format=${format}`,
            `absencje_zespolu_${startDate}_${endDate}.${EXT[format]}`
        ),

    downloadTeamOvertime: (startDate, endDate, format) =>
        fetchAndDownload(
            `${API_URL}/overtime/team?startDate=${startDate}&endDate=${endDate}&format=${format}`,
            `nadgodziny_zespolu_${startDate}_${endDate}.${EXT[format]}`
        ),
};

export default reportService;
