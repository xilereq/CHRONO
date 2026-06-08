
export const getErrorMessage = (errorData) => {
    const errorCode = errorData?.code;

    switch (errorCode) {
        case 'INSUFFICIENT_BALANCE':
            if (errorData.message) {
                return `Nie posiadasz wystarczającego salda nadgodzin. ${tłumaczKomunikatNadgodzin(errorData.message)}`;
            }
            return 'Nie posiadasz wystarczającego salda nadgodzin, aby złożyć ten wniosek.';

        case 'POLICY_NOT_FOUND':
            return 'Wskazana polityka czasu pracy nie istnieje w systemie.';

        case 'PAST_START_DATE':
            return 'Data początkowa obowiązywania polityki nie może być datą z przeszłości.';

        case 'START_DATE_CONFLICT':
            return 'Nowa polityka nie może zaczynać się wcześniej lub pokrywać z obecną polityką użytkownika.';

        case 'POLICY_ALREADY_ASSIGNED':
            return 'Ta polityka czasu pracy została już przypisana do tego pracownika.';

        case 'HOLIDAY_NOT_FOUND':
            return 'Wybrane święto nie zostało znalezione w bazie danych.';

        case 'HOLIDAY_ALREADY_ASSIGNED':
            return 'To święto zostało już wcześniej przypisane do wybranej polityki.';

        case 'HOLIDAY_NOT_ASSIGNED':
            return 'Nie można usunąć powiązania, ponieważ to święto nie jest przypisane do tej polityki.';

        case 'WORK_ALREADY_STARTED':
            return 'Praca została już rozpoczęta. Nie możesz otworzyć kolejnej aktywnej sesji.';

        case 'NO_ACTIVE_WORK':
            return 'Brak aktywnej sesji pracy. Nie możesz zarejestrować tego zdarzenia.';

        case 'INSUFFICIENT_WORK_TIME':
            return errorData.message ? tłumaczKomunikatCzasu(errorData.message) : 'Twój dzisiejszy czas pracy jest zbyt krótki, aby zakończyć dzień.';

        case 'EXCESSIVE_WORK_TIME':
            return 'Przekroczono maksymalny dozwolony limit pracy dla Twojej polityki. Czas sesji został automatycznie skorygowany.';

        case 'LEAVE_ALREADY_STARTED':
            return 'Przerwa lub urlop zostały już rozpoczęte.';

        case 'NO_ACTIVE_LEAVE':
            return 'Nie jesteś obecnie na żadnej przerwie ani urlopie.';

        default:
            return errorData?.message || 'Wystąpił nieoczekiwany błąd aplikacji. Spróbuj ponownie później.';
    }
};

const tłumaczKomunikatCzasu = (message) => {
    return message
        .replace('You still need to work for:', 'Musisz popracować jeszcze przez:')
        .replace('today.', 'dzisiaj.');
};

const tłumaczKomunikatNadgodzin = (message) => {
    return message
        .replace('Not enough overtime balance to reserve (you have currently have', 'Dostępne saldo nadgodzin to tylko:')
        .replace('to use.', '');
};

export const handleApiResponse = async (response) => {
    if (!response.ok) {
        let errorData = {};
        try {
            errorData = await response.json();
        } catch (e) {
            errorData = { message: 'Błąd połączenia lub krytyczny błąd serwera.' };
        }

        throw new Error(getErrorMessage(errorData));
    }

    if (response.status === 204) return null;

    const text = await response.text();
    return text ? JSON.parse(text) : {};
};
