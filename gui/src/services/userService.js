const API_URL = 'http://localhost:8080/api/users';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const userService = {
    getSubordinates: async () => {
        const response = await fetch(`${API_URL}/subordinates`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Nie udało się pobrać listy podwładnych');
        return response.json();
    },
    createUser: async (userData) => {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(userData)
        });
        if (!response.ok) {
            const errText = await response.text();
            throw new Error(errText || 'Błąd podczas tworzenia użytkownika');
        }
        return response.json();
    },

    getMyRole: async () => {
        const response = await fetch(`${API_URL}/my-role`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania roli');
        return response.json();
    },

    getAllUsers: async () => {
        const response = await fetch(`${API_URL}/users`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania użytkowników');
        return response.json();
    },
    getManagers: async () => {
        const response = await fetch(`${API_URL}/managers`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania kierowników');
        return response.json();
    },
    getEmployees: async () => {
        const response = await fetch(`${API_URL}/employees`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania pracowników');
        return response.json();
    },
    getAdmins: async () => {
        const response = await fetch(`${API_URL}/admins`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania administratorów');
        return response.json();
    },
    getHrs: async () => {
        const response = await fetch(`${API_URL}/hrs`, { headers: getHeaders() });
        if (!response.ok) throw new Error('Błąd pobierania działu HR');
        return response.json();
    }
};

export default userService;
