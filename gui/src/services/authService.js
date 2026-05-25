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

    // Roles helpers - roles are stored in localStorage as JSON array under 'userRoles'
    getUserRoles: () => {
        try {
            const raw = localStorage.getItem('userRoles');
            if (!raw) return [];
            const parsed = JSON.parse(raw);
            return Array.isArray(parsed) ? parsed : [];
        } catch (e) {
            return [];
        }
    },

    hasRole: (role) => {
        const roles = authService.getUserRoles();
        return roles.includes(role);
    },

    hasAnyRole: (checkRoles) => {
        const roles = authService.getUserRoles();
        return checkRoles.some(r => roles.includes(r));
    },

    setUserRoles: (roles) => {
        if (!roles) {
            localStorage.removeItem('userRoles');
            return;
        }
        try {
            localStorage.setItem('userRoles', JSON.stringify(roles));
        } catch (e) {
            // ignore
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
    }
};

export default authService;
