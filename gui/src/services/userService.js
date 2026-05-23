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
    }
};

export default usersService;
