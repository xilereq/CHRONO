const API_URL = 'http://localhost:8080/api/work-log';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

export const workLogService = {
    getStatus: async () => {
        const response = await fetch(`${API_URL}/status`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania statusu pracy');
        return response.json();
    },
    startWork: async () => {
        const response = await fetch(`${API_URL}/start`, {
            method: 'POST',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd podczas rozpoczynania pracy');
        return response.json();
    },
    getActiveTime: async () => {
        const response = await fetch(`${API_URL}/active-time`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania czasu pracy');
        return response.text();
    },
    endWork: async () => {
        const response = await fetch(`${API_URL}/end`, {
            method: 'POST',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd podczas kończenia pracy');
        return response.json();
    },
    getMonthlyStats: async () => {
        const response = await fetch(`${API_URL}/monthly-stats`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania statystyk miesięcznych');
        return response.json();
    }
};
