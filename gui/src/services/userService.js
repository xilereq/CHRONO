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
    },
    setManager: async (email, managerEmail) => {
        const response = await fetch(`${API_URL}/set-manager`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({
                email: email,
                managerEmail: managerEmail
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Nie udało się przypisać managera.');
        }

        return true;
    },

};

export default userService;
