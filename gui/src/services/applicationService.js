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

    // Sekcja zaktualizowana: wysyłamy JSON-a w polu body
    changeStatus: async (applicationId, newStatus, decisionReason) => {
        const payload = {
            newStatus: newStatus,
            decisionReason: decisionReason || ""
        };

        const response = await fetch(`${API_URL}/${applicationId}/status`, {
            method: 'PATCH',
            headers: getHeaders(),
            body: JSON.stringify(payload)
        });
        if (!response.ok) throw new Error('Błąd zmiany statusu');
        return response.json();
    },

    getAllApplications: async () => {
        const response = await fetch(`${API_URL}/all`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać wszystkich wniosków');
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
