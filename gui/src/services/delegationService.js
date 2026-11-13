const API_URL = '../api/delegations';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const readError = async (response, fallback) => {
    const body = await response.json().catch(() => null);
    const error = new Error(body?.message || fallback);
    error.code = body?.code;
    return error;
};

const get = async (path, fallback) => {
    const response = await fetch(`${API_URL}${path}`, { headers: getHeaders() });
    if (!response.ok) throw await readError(response, fallback);
    return response.json();
};

const delegationService = {
    getGiven: () => get('/given', 'Nie udało się pobrać zastępstw.'),

    getAll: () => get('', 'Nie udało się pobrać zastępstw.'),

    getReceived: () => get('/received', 'Nie udało się pobrać zastępstw.'),

    create: async ({ managerId, deputyId, startDate, endDate, reason }) => {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({
                managerId: managerId ? Number(managerId) : null,
                deputyId: Number(deputyId),
                startDate,
                endDate,
                reason: reason?.trim() || null
            })
        });
        if (!response.ok) throw await readError(response, 'Nie udało się przypisać zastępstwa.');
        return response.json();
    },

    revoke: async (id) => {
        const response = await fetch(`${API_URL}/${id}/revoke`, {
            method: 'PATCH',
            headers: getHeaders()
        });
        if (!response.ok) throw await readError(response, 'Nie udało się cofnąć zastępstwa.');
        return response.json();
    },

    getActiveReceived: async () => {
        try {
            const received = await delegationService.getReceived();
            return (received || []).filter(d => d.status === 'ACTIVE');
        } catch {
            return [];
        }
    },
};

export default delegationService;
