const API_URL = '../api/applications';

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
});

const ERROR_MESSAGES = {
    APPLICATION_NOT_FOUND:              'Nie znaleziono wniosku.',
    APPLICATION_ALREADY_PROCESSED:      'Wniosek został już rozpatrzony — nie można zmienić jego statusu.',
    APPLICATION_NOT_PENDING:            'Anulowanie jest możliwe tylko dla wniosków oczekujących na rozpatrzenie.',
    UNAUTHORIZED_MANAGER:               'Brak uprawnień. Tylko bezpośredni przełożony może rozpatrywać wnioski tego pracownika.',
    UNAUTHORIZED_ACCESS:                'Brak uprawnień dostępu do tego zasobu.',
    UNAUTHORIZED_CANCELLATION:          'Możesz anulować tylko własne wnioski.',
    USER_NOT_FOUND:                     'Nie znaleziono użytkownika.',
    INSUFFICIENT_LEAVE_BALANCE:         'Niewystarczające saldo urlopu. Sprawdź liczbę dostępnych dni urlopowych.',
    APPLICATION_DAILY_LIMIT_EXCEEDED:   'Wniosek odrzucony — suma godzin pracy i nadgodzin przekroczyłaby dzienny limit.',
    REQUESTED_TIME_EXCEEDS_SCHEDULED:   'Żądana liczba godzin przekracza zaplanowany czas pracy na ten dzień.',
};

const readError = async (response, fallback) => {
    const body = await response.json().catch(() => null);
    const translated = ERROR_MESSAGES[body?.code];
    return new Error(translated || body?.message || fallback);
};

const applicationService = {
    getUserApplications: async () => {
        const response = await fetch(`${API_URL}/my`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać wniosków');
        return response.json();
    },

    createApplication: async (applicationData) => {
        const response = await fetch(`${API_URL}/employee`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(applicationData)
        });
        if (!response.ok) {
            const errorBody = await response.json().catch(() => null);
            if (errorBody?.code === 'INSUFFICIENT_BALANCE') {
                const timeMatch = errorBody.message?.match(/(\d+h:\d+m)/);
                const time = timeMatch ? timeMatch[1] : '0h:00m';
                throw new Error(`Niewystarczające saldo nadgodzin. Aktualnie masz ${time} do wykorzystania.`);
            }
            throw new Error(ERROR_MESSAGES[errorBody?.code] || errorBody?.message || 'Błąd podczas składania wniosku');
        }
        return response.json();
    },

    cancelApplication: async (applicationId) => {
        const response = await fetch(`${API_URL}/${applicationId}/cancel`, {
            method: 'PATCH',
            headers: getHeaders()
        });
        if (!response.ok) throw await readError(response, 'Błąd podczas anulowania wniosku');
        return response.json();
    },

    changeStatus: async (applicationId, newStatus, decisionReason) => {
        const payload = {
            newStatus: newStatus,
            decisionReason: decisionReason || ""
        };
        const response = await fetch(`${API_URL}/${applicationId}/status`, {
            method: 'PATCH',
            headers: getHeaders(),
            body: JSON.stringify(payload)
        });
        if (!response.ok) throw await readError(response, 'Błąd zmiany statusu wniosku');
        return response.json();
    },

    approveMany: async (applicationIds, decisionReason) => {
        const response = await fetch(`${API_URL}/bulk-approve`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify({ applicationIds, decisionReason: decisionReason || '' })
        });

        if (response.ok) {
            const body = await response.json().catch(() => ({}));
            const approved = Array.isArray(body.approved) ? body.approved : [];
            const failed = Array.isArray(body.failed)
                ? body.failed.map(f => ({
                    id: f.id,
                    message: ERROR_MESSAGES[f.code] || f.message || 'Nie udało się zatwierdzić wniosku.'
                }))
                : [];
            return { approved, failed };
        }

        if (![404, 405, 501].includes(response.status)) {
            throw await readError(response, 'Błąd masowej akceptacji wniosków');
        }

        const approved = [];
        const failed = [];
        for (const id of applicationIds) {
            try {
                await applicationService.changeStatus(id, 'APPROVED', decisionReason);
                approved.push(id);
            } catch (error) {
                failed.push({ id, message: error.message });
            }
        }
        return { approved, failed };
    },

    getAllApplications: async () => {
        const response = await fetch(`${API_URL}/all`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać wszystkich wniosków');
        return response.json();
    },

    getPendingForManager: async () => {
        const response = await fetch(`${API_URL}/manager/pending`, {
            headers: getHeaders()
        });
        if (!response.ok) throw new Error('Nie udało się pobrać wniosków do zatwierdzenia');
        return response.json();
    },

    createApplicationForEmployee: async (targetEmployeeId, payload) => {
        const response = await fetch(`${API_URL}/employee/${targetEmployeeId}`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(payload)
        });
        if (!response.ok) throw await readError(response, 'Błąd podczas składania wniosku za pracownika');
        return response.json();
    },
};

export default applicationService;
