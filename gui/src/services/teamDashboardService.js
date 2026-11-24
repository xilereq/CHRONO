const API_URL = '../api/team-dashboard';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const teamDashboardService = {

    getDashboard: async ({ date, managerId } = {}) => {
        const params = new URLSearchParams();
        if (date) params.set('date', date);
        if (managerId) params.set('managerId', managerId);
        const query = params.toString();

        const response = await fetch(`${API_URL}${query ? `?${query}` : ''}`, { headers: getHeaders() });
        if (!response.ok) {
            const body = await response.json().catch(() => null);
            const error = new Error(body?.message || 'Nie udało się pobrać danych zespołu.');
            error.code = body?.code;
            error.status = response.status;
            throw error;
        }
        return response.json();
    },
};

export default teamDashboardService;
