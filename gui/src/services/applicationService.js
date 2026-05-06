const API_URL = 'http://localhost:8080/api/applications';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const applicationService = {
    // Pobiera wszystkie wnioski danego pracownika (GET /employee/{id})
    getUserApplications: async (employeeId) => {
        const response = await fetch(`${API_URL}/employee/${employeeId}`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać wniosków');
        return response.json();
    },

    // Tworzy nowy wniosek (POST /employee/{id})
    createApplication: async (employeeId, applicationData) => {
        const response = await fetch(`${API_URL}/employee/${employeeId}`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(applicationData)
        });
        if (!response.ok) throw new Error('Błąd podczas składania wniosku');
        return response.json();
    },

    // Zmienia status (PATCH /{id}/status) - przydatne dla panelu managera
    changeStatus: async (applicationId, newStatus, reviewerId, decisionReason) => {
        const url = `${API_URL}/${applicationId}/status?newStatus=${newStatus}&reviewerId=${reviewerId}&decisionReason=${decisionReason}`;
        const response = await fetch(url, {
            method: 'PATCH',
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Błąd zmiany statusu');
        return response.json();
    }
};

export default applicationService;
