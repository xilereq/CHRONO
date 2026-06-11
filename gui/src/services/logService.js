const API_URL = '../api/logs/operational';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const logService = {
    getAllLogs: async () => {
        const response = await fetch(API_URL, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania logów systemowych');
        return response.json();
    }
};

export default logService;
