const API_URL = '../api/auth';

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
            if (data.roleName) {
                try { localStorage.setItem('userRoles', JSON.stringify([data.roleName])); } catch(e) {}
            }
            if (data.forcePasswordChange) {
                localStorage.setItem('forcePasswordChange', 'true');
            } else {
                localStorage.removeItem('forcePasswordChange');
            }
        }

        return data;
    },

    logout: () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('userRoles');
        localStorage.removeItem('forcePasswordChange');
        window.location.reload();
    },

    isAuthenticated: () => {
        return !!localStorage.getItem('accessToken');
    },

    getUserEmail: () => {
        return localStorage.getItem('userEmail');
    },

    isForcePasswordChange: () => {
        return localStorage.getItem('forcePasswordChange') === 'true';
    },

    changeFirstPassword: async (oldPassword, newPassword) => {
        const token = localStorage.getItem('accessToken');
        const response = await fetch(`${API_URL}/change-first-password`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ oldPassword, newPassword }),
        });
        if (!response.ok) {
            const errText = await response.text();
            throw new Error(errText || 'Nie udało się zmienić hasła. Sprawdź poprawność tymczasowego hasła.');
        }
        localStorage.removeItem('forcePasswordChange');
    },

    changePassword: async (oldPassword, newPassword) => {
        const token = localStorage.getItem('accessToken');
        const response = await fetch(`${API_URL}/change-password`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ oldPassword, newPassword }),
        });
        if (!response.ok) {
            const errText = await response.text();
            throw new Error(errText || 'Nie udało się zmienić hasła. Sprawdź poprawność obecnego hasła.');
        }
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
        const token = localStorage.getItem('accessToken');

        const response = await fetch(`${API_URL}/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(userData),
        });

        if (!response.ok) {
            const errText = await response.text();
            throw new Error(errText || 'Nie udało się zarejestrować użytkownika. Upewnij się, że masz uprawnienia Administratora.');
        }

        return await response.json().catch(() => ({}));
    }
};

export default authService;
