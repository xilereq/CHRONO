const API_URL = 'http://localhost:8080/api/applications';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const applicationService = {
    getUserApplications: async () => {
        const response = await fetch(`${API_URL}/my`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać wniosków');
        return response.json();
    },

    createApplication: async (applicationData) => {
        const response = await fetch(`${API_URL}/employee`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(applicationData)
        });
        if (!response.ok) throw new Error('Błąd podczas składania wniosku');
        return response.json();
    },

    changeStatus: async (applicationId, newStatus, reviewerId, decisionReason) => {
        const url = `${API_URL}/${applicationId}/status?newStatus=${newStatus}&reviewerId=${reviewerId}&decisionReason=${decisionReason}`;
        const response = await fetch(url, {
            method: 'PATCH',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd zmiany statusu');
        return response.json();
    },

    getPendingForManager: async () => {
        const response = await fetch(`${API_URL}/manager/pending`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać wniosków do zatwierdzenia');
        return response.json();
    }
};

export default applicationService;
