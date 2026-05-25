const API_URL = 'http://localhost:8080/api/users';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const usersService = {
    getSubordinates: async () => {
        const response = await fetch(`${API_URL}/subordinates`, {
            headers: getHeaders()
        });
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
    }
};

export default usersService;
