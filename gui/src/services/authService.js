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
            if (data.role) {
                try { localStorage.setItem('userRoles', JSON.stringify([data.role])); } catch(e) {}
            }
        }

        return data;
    },

    logout: () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('userRoles');
        window.location.reload();
    },

    isAuthenticated: () => {
        return !!localStorage.getItem('accessToken');
    },

    getUserEmail: () => {
        return localStorage.getItem('userEmail');
    },

    requestPasswordReset: async (email) => {
        const response = await fetch(`${API_URL}/forgot-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email }),
        });

        if (!response.ok) {
            const errText = await response.text();
            throw new Error(errText || 'Nie udało się wysłać instrukcji resetu.');
        }

        return await response.json().catch(() => ({}));
    },

    resetPassword: async (token, password) => {
        const response = await fetch(`${API_URL}/reset-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ token, password }),
        });

        if (!response.ok) {
            const errText = await response.text();
            throw new Error(errText || 'Nie udało się zresetować hasła.');
        }

        return await response.json().catch(() => ({}));
    },

    register: async (userData) => {
        const response = await fetch(`${API_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(userData),
        });
    }
};

export default authService;
