const API_URL = 'http://localhost:8080/api/work-log';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

export const workLogService = {
    getStatus: async (userId) => {
        const response = await fetch(`${API_URL}/status/${userId}`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania statusu pracy');
        return response.json();
    },
    startWork: async (userId) => {
        const response = await fetch(`${API_URL}/start/${userId}`, {
            method: 'POST',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd podczas rozpoczynania pracy');
        return response.json();
    },
    endWork: async (userId) => {
        const response = await fetch(`${API_URL}/end/${userId}`, {
            method: 'POST',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd podczas kończenia pracy');
        return response.json();
    }
};
