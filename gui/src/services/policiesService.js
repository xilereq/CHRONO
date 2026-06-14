import {handleApiResponse} from "../utils/errorHandler";

const API_URL = '../api/policies';
const HOLIDAYS_API_URL = '../api/holidays';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const policiesService = {

    createPolicy: async (request) => {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(request)
        });
        if (!response.ok) throw new Error('Błąd podczas tworzenia polityki');
        return response.json();
    },

    assignPolicyToUser: async (userId, request) => {
        const response = await fetch(`${API_URL}/${userId}/assign`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(request)
        });
        if (!response.ok) return handleApiResponse(response);
        if (response.status === 204) return { success: true };
        return response.json();
    },

    getAllPolicies: async () => {
        const response = await fetch(API_URL, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać polityk');
        return response.json();
    },

    getPolicyById: async (policyId) => {
        const response = await fetch(`${API_URL}/${policyId}`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać polityki');
        return response.json();
    },

    deletePolicy: async (policyId) => {
        const response = await fetch(`${API_URL}/${policyId}`, {
            method: 'DELETE',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd podczas usuwania polityki');
        if (response.status === 204) return { success: true };
        return response.json();
    },


    getHolidaysForPolicy: async (policyId) => {
        const response = await fetch(`${API_URL}/${policyId}/holidays`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać dni wolnych dla tej polityki');
        return response.json();
    },

    assignHolidayToPolicy: async (policyId, request) => {
        const response = await fetch(`${API_URL}/${policyId}/holidays`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(request)
        });
        if (!response.ok) return handleApiResponse(response);
        if (response.status === 204) return { success: true };
        return response.json();
    },

    removeHolidayFromPolicy: async (policyId, holidayId) => {
        const response = await fetch(`${API_URL}/${policyId}/holidays/${holidayId}`, {
            method: 'DELETE',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd podczas usuwania dnia wolnego z polityki');
        if (response.status === 204) return { success: true };
        return response.json();
    },


    createHoliday: async (request) => {
        const response = await fetch(HOLIDAYS_API_URL, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(request)
        });
        if (!response.ok) throw new Error('Błąd podczas dodawania święta');
        return response.json();
    },

    getAllHolidays: async () => {
        const response = await fetch(HOLIDAYS_API_URL, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać świąt');
        return response.json();
    },

    getHolidayById: async (holidayId) => {
        const response = await fetch(`${HOLIDAYS_API_URL}/${holidayId}`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać święta');
        return response.json();
    },

    deleteHoliday: async (holidayId) => {
        const response = await fetch(`${HOLIDAYS_API_URL}/${holidayId}`, {
            method: 'DELETE',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd podczas usuwania święta');
        if (response.status === 204) return { success: true };
        return response.json();
    },

    assignPolicyBulk: async (payload) => {
        const response = await fetch(`${API_URL}/assign-bulk`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || 'Błąd podczas masowego przypisywania polityki.');
        }
        return true;
    },
    assignHolidaysToPolicyBulk: async (policyId, payload) => {
        const response = await fetch(`${API_URL}/${policyId}/holidays/bulk`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(payload)
        });
        if (!response.ok) throw new Error('Błąd podczas masowego dodawania świąt.');
        return true;
    },

};

export default policiesService;
