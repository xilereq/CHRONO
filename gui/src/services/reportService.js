const API_URL = '../api/reports';

const getHeaders = () => ({
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const readError = async (response, fallback) => {
    const body = await response.json().catch(() => null);
    const error = new Error(body?.message || fallback);
    error.code = body?.code;
    return error;
};

const fetchReportData = async (path, startDate, endDate) => {
    const response = await fetch(
        `${API_URL}/${path}/data?startDate=${startDate}&endDate=${endDate}`,
        { headers: getHeaders() }
    );
    if (!response.ok) throw await readError(response, 'Nie udało się pobrać danych raportu');
    return response.json();
};

const fieldValue = (document, label) => document.fields?.find(f => f.label === label)?.value ?? 0;

const toTimesheetSummary = (document) => {
    const index = Object.fromEntries((document.columns || []).map((c, i) => [c.label, i]));
    const cell = (row, label) => (index[label] === undefined ? null : row[index[label]]);
    return {
        totalWork:     fieldValue(document, 'Przepracowano'),
        totalRequired: fieldValue(document, 'Wymagano'),
        totalBalance:  fieldValue(document, 'Bilans'),
        totalNight:    fieldValue(document, 'Godziny Nocne'),
        dailyEntries: (document.rows || []).map(row => ({
            date:            cell(row, 'Data'),
            start:           cell(row, 'Start'),
            end:             cell(row, 'Koniec'),
            workSeconds:     cell(row, 'Efektywny Czas') || 0,
            requiredSeconds: cell(row, 'Wymagany Czas') || 0,
            balanceSeconds:  cell(row, 'Bilans') || 0,
            nightSeconds:    cell(row, 'Godziny Nocne') || 0,
            applicationType: cell(row, 'Wniosek'),
        })),
    };
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
    fetchMyTimesheetData: async (startDate, endDate) =>
        toTimesheetSummary(await fetchReportData('timesheet/me', startDate, endDate)),

    getMyTimesheetData:       (startDate, endDate) => fetchReportData('timesheet/me', startDate, endDate),
    getMyOvertimeData:        (startDate, endDate) => fetchReportData('overtime/me', startDate, endDate),
    getEmployeeTimesheetData: (employeeId, startDate, endDate) =>
        fetchReportData(`timesheet/employee/${employeeId}`, startDate, endDate),
    getTeamWorkloadData:      (startDate, endDate) => fetchReportData('workload/team', startDate, endDate),
    getTeamAbsencesData:      (startDate, endDate) => fetchReportData('absences/team', startDate, endDate),
    getTeamOvertimeData:      (startDate, endDate) => fetchReportData('overtime/team', startDate, endDate),
    getTeamAnomaliesData:     (startDate, endDate) => fetchReportData('anomalies/team', startDate, endDate),
    getTeamMatrixData:        (startDate, endDate) => fetchReportData('matrix/team', startDate, endDate),
    getTeamDailyOvertimeData: (startDate, endDate) => fetchReportData('overtime/team/daily', startDate, endDate),
    getTeamNightHoursData:    (startDate, endDate) => fetchReportData('night-hours/team', startDate, endDate),

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

    downloadTeamAnomalies: (startDate, endDate, format) =>
        fetchAndDownload(
            `${API_URL}/anomalies/team?startDate=${startDate}&endDate=${endDate}&format=${format}`,
            `anomalie_zespolu_${startDate}_${endDate}.${EXT[format]}`
        ),

    downloadTeamMatrix: (startDate, endDate, format) =>
        fetchAndDownload(
            `${API_URL}/matrix/team?startDate=${startDate}&endDate=${endDate}&format=${format}`,
            `macierz_zespolu_${startDate}_${endDate}.${EXT[format]}`
        ),

    downloadTeamDailyOvertime: (startDate, endDate, format) =>
        fetchAndDownload(
            `${API_URL}/overtime/team/daily?startDate=${startDate}&endDate=${endDate}&format=${format}`,
            `nadgodziny_dzienne_zespolu_${startDate}_${endDate}.${EXT[format]}`
        ),

    downloadTeamNightHours: (startDate, endDate, format) =>
        fetchAndDownload(
            `${API_URL}/night-hours/team?startDate=${startDate}&endDate=${endDate}&format=${format}`,
            `godziny_nocne_zespolu_${startDate}_${endDate}.${EXT[format]}`
        ),
};

export default reportService;
