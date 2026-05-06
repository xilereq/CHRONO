const API_URL = 'http://localhost:8080/api/auth';

const authService = {
    login: async (email, password) => {
        const response = await fetch(`${API_URL}/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email, password }),
        });

        if (!response.ok) {
            throw new Error('Błędny email lub hasło');
        }

        const data = await response.json();

        if (data.token) {
            localStorage.setItem('accessToken', data.token);
            localStorage.setItem('refreshToken', data.refreshToken);
            localStorage.setItem('userEmail', email);
        }

        return data;
    },

    logout: () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userEmail');
        window.location.reload();
    },

    isAuthenticated: () => {
        return !!localStorage.getItem('accessToken');
    },

    getUserEmail: () => {
        return localStorage.getItem('userEmail');
    }
};

export default authService;
