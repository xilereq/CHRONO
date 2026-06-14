import {handleApiResponse} from "../utils/errorHandler";

const API_URL = '../api/work-log';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const workLogService = {
    getStatus: async () => {
        const response = await fetch(`${API_URL}/status`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania statusu pracy');
        return response.json();
    },
    startWork: async (request) => {
        const response = await fetch(`${API_URL}/start`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(request)
        });
        return handleApiResponse(response);
    },
    endWork: async () => {
        const response = await fetch(`${API_URL}/end`, {
            method: 'POST',
            headers: getHeaders()
        });
        return handleApiResponse(response);
    },
    getActiveTime: async () => {
        const response = await fetch(`${API_URL}/active-time`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania czasu pracy');
        return response.text();
    },
    getMonthlyStats: async () => {
        const response = await fetch(`${API_URL}/monthly-stats`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania statystyk miesięcznych');
        return response.json();
    },
    cancelWork: async () => {
        const response = await fetch(`${API_URL}/cancel`, {
            method: 'POST',
            headers: getHeaders()
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || 'Błąd podczas anulowania rozpoczęcia pracy');
        }
        return true;
    },
};

export default workLogService;
