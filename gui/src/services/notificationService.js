const API_URL = '../api/notifications';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const notificationService = {
    getUnread: async () => {
        const response = await fetch(`${API_URL}/unread`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania powiadomień');
        return response.json();
    },

    markAsRead: async (id) => {
        const response = await fetch(`${API_URL}/${id}/read`, {
            method: 'PATCH',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd oznaczania powiadomienia');
        return response.ok;
    },

    getSseUrl: () => {
        const token = localStorage.getItem('accessToken');
        return `${API_URL}/subscribe?token=${token}`;
    }
};

export default notificationService;
