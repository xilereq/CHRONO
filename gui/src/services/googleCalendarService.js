const API_URL = '../api/google/calendar';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const ERROR_MESSAGES = {
    GOOGLE_NOT_CONFIGURED:     'Integracja z Kalendarzem Google nie jest skonfigurowana na serwerze.',
    GOOGLE_EXTERNAL_API_ERROR: 'Nie udało się połączyć z usługą Google. Spróbuj ponownie później.',
};

const readError = async (response, fallback) => {
    const body = await response.json().catch(() => null);
    const error = new Error(ERROR_MESSAGES[body?.code] || fallback);
    error.code = body?.code;
    return error;
};

const googleCalendarService = {
    getStatus: async () => {
        const response = await fetch(`${API_URL}/status`, { headers: getHeaders() });
        if (!response.ok) throw await readError(response, 'Nie udało się sprawdzić połączenia z Kalendarzem Google.');
        return response.json();
    },

    getAuthorizationUrl: async () => {
        const response = await fetch(`${API_URL}/connect`, { headers: getHeaders() });
        if (!response.ok) throw await readError(response, 'Nie udało się rozpocząć łączenia z Kalendarzem Google.');
        const body = await response.json();
        if (!body?.url) throw new Error('Serwer nie zwrócił adresu logowania Google.');
        return body.url;
    },
};

export default googleCalendarService;
