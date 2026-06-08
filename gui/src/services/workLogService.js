import {handleApiResponse} from "../utils/errorHandler";

const API_URL = 'http://localhost:8080/api/work-log';

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
    startWork: async () => {
        const response = await fetch(`${API_URL}/start`, {
            method: 'POST',
            headers: getHeaders()
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
    }
};

export default workLogService;
