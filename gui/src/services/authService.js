// Domyślny adres Twojego backendu Spring Boot. Zmień port, jeśli używasz innego.
const API_URL = 'http://localhost:8080/api/auth';

export const authService = {
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

        // Zapisujemy tokeny oraz e-mail użytkownika w pamięci przeglądarki
        if (data.accessToken) {
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken);
            localStorage.setItem('userEmail', email);
        }

        return data;
    },

    logout: () => {
        // Czyścimy pamięć przy wylogowywaniu
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userEmail');
        window.location.reload(); // Odświeża stronę, co przeniesie nas na ekran logowania
    },

    isAuthenticated: () => {
        // Sprawdzamy, czy użytkownik ma zapisany token dostępu
        return !!localStorage.getItem('accessToken');
    },

    getUserEmail: () => {
        // Pobieramy e-mail zalogowanego użytkownika
        return localStorage.getItem('userEmail');
    }
};
