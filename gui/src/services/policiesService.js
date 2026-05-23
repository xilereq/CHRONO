const API_URL = 'http://localhost:8080/api/policies';
const HOLIDAYS_API_URL = 'http://localhost:8080/api/holidays';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const policiesService = {
    getAll: async () => {
        const response = await fetch(API_URL, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać polityk');
        return response.json();
    },

    getById: async (id) => {
        const response = await fetch(`${API_URL}/${id}`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać polityki');
        return response.json();
    },

    create: async (policyData) => {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(policyData)
        });
        if (!response.ok) throw new Error('Błąd podczas tworzenia polityki');
        return response.json();
    },

    remove: async (id) => {
        const response = await fetch(`${API_URL}/${id}`, {
            method: 'DELETE',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd podczas usuwania polityki');
        if (response.status === 204) return { success: true };
        return response.json();
    },

    assignToUser: async (userId, assignRequest) => {
        const response = await fetch(`${API_URL}/${userId}/assign`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(assignRequest)
        });
        if (!response.ok) throw new Error('Błąd podczas przypisywania polityki');
        if (response.status === 204) return { success: true };
        return response.json();
    },

    getHolidays: async (policyId) => {
        const response = await fetch(`${API_URL}/${policyId}/holidays`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać dni wolnych dla tej polityki');
        return response.json();
    },

    assignHoliday: async (policyId, holidayRequest) => {
        const response = await fetch(`${API_URL}/${policyId}/holidays`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(holidayRequest)
        });
        if (!response.ok) throw new Error('Błąd podczas przypisywania dnia wolnego');
        if (response.status === 204) return { success: true };
        return response.json();
    },

    removeHoliday: async (policyId, holidayId) => {
        const response = await fetch(`${API_URL}/${policyId}/holidays/${holidayId}`, {
            method: 'DELETE',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd podczas usuwania dnia wolnego z polityki');
        if (response.status === 204) return { success: true };
        return response.json();
    },

    getAllGlobalHolidays: async () => {
        const response = await fetch(HOLIDAYS_API_URL, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać globalnych świąt');
        return response.json();
    },

    createGlobalHoliday: async (holidayData) => {
        const response = await fetch(HOLIDAYS_API_URL, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(holidayData)
        });
        if (!response.ok) throw new Error('Błąd podczas dodawania globalnego święta');
        return response.json();
    },

    deleteGlobalHoliday: async (id) => {
        const response = await fetch(`${HOLIDAYS_API_URL}/${id}`, {
            method: 'DELETE',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd podczas usuwania święta');
        if (response.status === 204) return { success: true };
        return response.json();
    }
};

export default policiesService;
