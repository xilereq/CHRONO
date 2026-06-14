const API_URL = '../api/work-log-requests';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const workLogRequestService = {
    getPendingRequests: async () => {
        const response = await fetch(`${API_URL}/pending`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd pobierania wniosków');
        return response.json();
    },

    approveRequest: async (requestId, resolveDto = null) => {
        const response = await fetch(`${API_URL}/${requestId}/approve`, {
            method: 'POST',
            headers: getHeaders(),
            body: resolveDto ? JSON.stringify(resolveDto) : null
        });
        if (!response.ok) throw new Error('Błąd akceptacji wniosku');
        return true;
    },

    rejectRequest: async (requestId, resolveDto = null) => {
        const response = await fetch(`${API_URL}/${requestId}/reject`, {
            method: 'POST',
            headers: getHeaders(),
            body: resolveDto ? JSON.stringify(resolveDto) : null
        });
        if (!response.ok) throw new Error('Błąd odrzucenia wniosku');
        return true;
    },

    submitRequest: async (requestDto) => {
        const response = await fetch(`${API_URL}`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(requestDto)
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Wystąpił błąd podczas wysyłania wniosku.');
        }
        return true;
    },
};

export default workLogRequestService;
