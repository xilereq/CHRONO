const API_URL = '../api/users/team-tree';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const ERROR_MESSAGES = {
    TEAM_TREE_FORBIDDEN: 'Brak dostępu do struktury tego zespołu.',
    USER_NOT_FOUND:      'Nie znaleziono użytkownika.',
};

const teamService = {
    getTeamTree: async ({ managerId, includeInactive = false } = {}) => {
        const params = new URLSearchParams();
        if (managerId) params.set('managerId', managerId);
        if (includeInactive) params.set('includeInactive', 'true');
        const query = params.toString();

        const response = await fetch(`${API_URL}${query ? `?${query}` : ''}`, { headers: getHeaders() });
        if (!response.ok) {
            const body = await response.json().catch(() => null);
            const error = new Error(ERROR_MESSAGES[body?.code] || body?.message || 'Nie udało się pobrać struktury zespołu.');
            error.code = body?.code;
            throw error;
        }
        return response.json();
    },
};

export default teamService;
