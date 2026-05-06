const API_URL = 'http://localhost:8080/api/leave-log';

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

    startLeave: async (leaveType = 'BREAK') => {
        const response = await fetch(`${API_URL}/start`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({ type: leaveType })
        });
        if (!response.ok) throw new Error('Błąd podczas rozpoczynania przerwy');
        return response.json();
    },
    endLeave: async () => {
        const response = await fetch(`${API_URL}/end`, {
            method: 'POST',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd podczas kończenia przerwy');
        return response.json();
    }
};

export default leaveLogService;
