const API_URL = '../api/users';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const NOT_AVAILABLE_STATUSES = [404, 405, 501];

const notAvailableError = () => {
    const error = new Error('Obsługa zastępstw nie jest jeszcze dostępna na serwerze.');
    error.notAvailable = true;
    return error;
};

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
        console.log('Response status:', response.status);
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
    setStartDate: async (email, date) => {
        const response = await fetch(`${API_URL}/set-start-date`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({
                email: email,
                date: date
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Nie udało się ustawić daty rozpoczęcia pracy.');
        }

        return true;
    },

    setEndDate: async (email, date) => {
        const response = await fetch(`${API_URL}/set-end-date`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({
                email: email,
                date: date
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Nie udało się ustawić daty zakończenia pracy.');
        }

        return true;
    },

    getMyDeputies: async () => {
        const response = await fetch(`${API_URL}/deputy`, { headers: getHeaders() });
        if (NOT_AVAILABLE_STATUSES.includes(response.status)) throw notAvailableError();
        if (!response.ok) throw new Error('Nie udało się pobrać zastępstw.');
        return response.json();
    },

    assignDeputy: async ({ deputyEmail, startDate, endDate }) => {
        const response = await fetch(`${API_URL}/deputy`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({ deputyEmail, startDate, endDate })
        });
        if (NOT_AVAILABLE_STATUSES.includes(response.status)) throw notAvailableError();
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Nie udało się przypisać zastępstwa.');
        }
        return true;
    },

    removeDeputy: async (id) => {
        const response = await fetch(`${API_URL}/deputy/${id}`, {
            method: 'DELETE',
            headers: getHeaders()
        });
        if (NOT_AVAILABLE_STATUSES.includes(response.status)) throw notAvailableError();
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Nie udało się cofnąć zastępstwa.');
        }
        return true;
    },

    setActive: async (email, active) => {
        const response = await fetch(`${API_URL}/set-active`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({
                email: email,
                active: active
            })
        });

        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Nie udało się zmienić statusu aktywności użytkownika.');
        }

        return true;
    }

};

export default userService;
