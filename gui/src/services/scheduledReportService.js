import { handleApiResponse } from '../utils/errorHandler';

const API_URL = '../api/reports/schedules';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const request = async (url, options = {}) => {
    const response = await fetch(url, { headers: getHeaders(), ...options });
    return handleApiResponse(response);
};

const normalize = (s) => s && ({ ...s, isActive: s.isActive ?? s.active ?? false });

const scheduledReportService = {
    getAll: async () => {
        const data = await request(API_URL);
        return Array.isArray(data) ? data.map(normalize) : [];
    },

    getById: async (scheduleId) => normalize(await request(`${API_URL}/${scheduleId}`)),

    create: async (payload) =>
        normalize(await request(API_URL, { method: 'POST', body: JSON.stringify(payload) })),

    update: async (scheduleId, payload) =>
        normalize(await request(`${API_URL}/${scheduleId}`, { method: 'PUT', body: JSON.stringify(payload) })),

    toggle: async (scheduleId) =>
        normalize(await request(`${API_URL}/${scheduleId}/toggle`, { method: 'PATCH' })),

    trigger: (scheduleId) => request(`${API_URL}/${scheduleId}/trigger`, { method: 'POST' }),

    remove: (scheduleId) => request(`${API_URL}/${scheduleId}`, { method: 'DELETE' }),
};

export default scheduledReportService;
