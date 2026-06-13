import {handleApiResponse} from "../utils/errorHandler";

const API_URL = '../api/leave-log';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const leaveLogService = {
    getStatus: async () => {
        const response = await fetch(`${API_URL}/status`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania statusu przerwy');
        return response.json();
    },

    startLeave: async () => {
        const response = await fetch(`${API_URL}/start`, {
            method: 'POST',
            headers: getHeaders()
        });
        if (!response.ok) return handleApiResponse(response);
        return response.json();
    },
    endLeave: async () => {
        const response = await fetch(`${API_URL}/end`, {
            method: 'POST',
            headers: getHeaders()
        });
        if (!response.ok) return handleApiResponse(response);
        return response.json();
    }
};

export default leaveLogService;
