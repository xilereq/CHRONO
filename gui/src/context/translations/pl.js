export const pl = {
    // ============= COMMON / OGÓLNE =============
    common: {
        appName: 'Chrono',
        loading: 'Ładowanie aplikacji...',
        logout: 'Wyloguj się',
        yes: 'Tak',
        no: 'Nie',
        cancel: 'Anuluj',
        save: 'Zapisz',
        delete: 'Usuń',
        edit: 'Edytuj',
        add: 'Dodaj',
        close: 'Zamknij',
        search: 'Szukaj',
        filter: 'Filtruj',
        error: 'Błąd',
        success: 'Sukces',
        warning: 'Ostrzeżenie',
        info: 'Informacja',
        back: 'Wróć',
        next: 'Dalej',
        previous: 'Wstecz',
        submit: 'Wyślij',
        loading_data: 'Ładowanie danych...',
        no_data: 'Brak danych',
        settings: 'Ustawienia',
    },

    // ============= SIDEBAR =============
    sidebar: {
        dashboard: 'Pulpit',
        requests: 'Wnioski',
        reports: 'Raporty',
        schedule: 'Grafik',
        userManagement: 'Zarządzanie Użytkownikami',
        teamSchedule: 'Czas pracy zespołu',
        adminPanel: 'Panel Administratora',
        systemLogs: 'Logi Systemowe',
        settings: 'Ustawienia',
    },

    // ============= TOPBAR =============
    topbar: {
        notifications: 'Powiadomienia',
        noNewNotifications: 'Brak nowych powiadomień',
        markAsRead: 'Odznacz',
        userLoggedIn: 'Zalogowany',
        errorFetchingNotifications: 'Błąd pobierania powiadomień',
        errorParsingNotification: 'Błąd parsowania powiadomienia',
        errorSSEConnection: 'Błąd połączenia SSE',
        markAsReadError: 'Nie udało się oznaczyć jako przeczytane',
        showPassword: 'Pokaż hasło',
        hidePassword: 'Ukryj hasło',
    },

    // ============= LOGIN PAGE =============
    login: {
        title: 'Logowanie do Chrono',
        subtitle: 'Wprowadź swoje dane, aby kontynuować',
        email: 'Adres e-mail',
        password: 'Hasło',
        loginButton: 'Zaloguj się',
        loggingIn: 'Logowanie...',
        forgotPassword: 'Zapomniałem hasła',
        emptyFieldsError: 'Wprowadź email i hasło.',
        loginError: 'Błąd logowania. Sprawdź dane i spróbuj ponownie.',
        emailPlaceholder: 'admin@chrono.pl',
        passwordPlaceholder: '●●●●●●●',
    },

    // ============= FORGOT PASSWORD =============
    forgotPassword: {
        title: 'Odzyskaj hasło',
        subtitle: 'Wprowadź swój adres email',
        sendButton: 'Wyślij',
        emailSent: 'Email wysłany! Sprawdź swoją skrzynkę.',
        invalidEmail: 'Podaj prawidłowy adres email.',
        emailNotFound: 'Adres email nie znaleziony.',
    },

    // ============= RESET PASSWORD =============
    resetPassword: {
        title: 'Resetuj hasło',
        newPassword: 'Nowe hasło',
        confirmPassword: 'Potwierdź hasło',
        resetButton: 'Resetuj hasło',
        passwordMismatch: 'Hasła nie pasują do siebie.',
        passwordTooShort: 'Hasło musi mieć co najmniej 8 znaków.',
        resetSuccess: 'Hasło zostało zmienione!',
    },

    // ============= DASHBOARD =============
    dashboard: {
        title: 'Pulpit',
        welcome: 'Witaj',
        quickRequests: 'Szybkie wnioski',
        recentRequests: 'Ostatnie wnioski',
        timeTracker: 'Tracker czasu',
        statusCharts: 'Wykresy statusu',
        noRecentRequests: 'Brak ostatnich wniosków',
        leaveRequest: 'Wniosek o urlop',
        workFromHome: 'Praca z domu',
        dayOff: 'Dzień wolny',
        scheduleChange: 'Zmiana harmonogramu',
        quickRequestsTitle: 'SZYBKIE AKCJE',
        leaveRequestBtn: 'ZŁÓŻ WNIOSEK URLOPOWY',
        overtimeTakeBtn: 'ODBIERZ NADGODZINY',
        scheduleChangeBtn: 'KOREKCJA CZASU PRACY',
        recentRequestsTitle: 'OSTATNIE WNIOSKI',
        mainTitle: 'Ewidencja Czasu Pracy - Pulpit Pracownika',
    },

    timeTracker: {
        startWork: 'ROZPOCZNIJ PRACĘ',
        endWork: 'ZAKOŃCZ PRACĘ',
        hoStart: 'HO Start',
        onBreak: 'W TRAKCIE PRZERWY',
        break: 'PRZERWA',
        endBreak: 'ZAKOŃCZ PRZERWĘ',
        workEndedAlert: 'Zakończono czas pracy',
        workAlreadyStarted: 'Praca jest już rozpoczęta w systemie.',
        cancelConfirm: 'Czy na pewno chcesz anulować to rozpoczęcie pracy? Ten wpis zostanie usunięty.',
        entryCancelledAlert: 'Anulowano wpis.',
        mistakeCancel: 'Pomyłka? Anuluj wpis ('
    },

    statusCharts: {
        yourStatusTitle: 'TWÓJ STATUS',
        loadingStats: 'Ładowanie statystyk...',
        overtimeTitle: 'NADGODZINY',
        workTimeTitle: 'CZAS PRACY (BIEŻĄCY MIESIĄC)',
        balance: 'Saldo:',
        worked: 'Przepracowano:',
        plan: 'Plan:'
    },


    // ============= REQUESTS PAGE =============
    requests: {
        title: 'Wnioski',
        myRequests: 'Moje wnioski',
        allRequests: 'Wszystkie wnioski',
        dateFrom: 'Data od',
        dateTo: 'Data do',
        description: 'Opis',
        createRequest: 'Utwórz nowy wniosek',
        noRequests: 'Brak wniosków',
        requestDetails: 'Szczegóły wniosku',
        submitRequest: 'Wyślij wniosek',
        cancelRequest: 'Anuluj wniosek',
        viewRequest: 'Pokaż wniosek',
        types: {
            holiday: 'Urlop wypoczynkowy',
            sick: 'Zwolnienie lekarskie',
            earlyExit: 'Wyjście wcześniejsze',
            overtimeEarn: 'Zgłoszenie nadgodzin',
            overtimeTake: 'Odbiór nadgodzin'
        },
        approved: 'Zatwierdzony',
        pending: 'Oczekujący',
        rejected: 'Odrzucony',
        cancelled: 'Anulowany',
        requestType: 'Typ',
        submitDate: 'Data zgłoszenia',
        absenceDate: 'Data nieobecności',
        status: 'Status'
    },

    // ============= APPROVALS PAGE =============
    approvals: {
        title: 'Zatwierdzenia',
        pendingApprovals: 'Wnioski do zatwierdzenia',
        approve: 'Zatwierdź',
        reject: 'Odrzuć',
        approvalComment: 'Komentarz',
        approveSuccess: 'Wniosek zatwierdzony.',
        rejectSuccess: 'Wniosek odrzucony.',
    },

    // ============= SCHEDULE PAGE =============
    schedule: {
        title: 'Grafik',
        mySchedule: 'Mój harmonogram',
        workHours: 'Godziny pracy',
        changeSchedule: 'Zmień harmonogram',
        monday: 'Poniedziałek',
        tuesday: 'Wtorek',
        wednesday: 'Środa',
        thursday: 'Czwartek',
        friday: 'Piątek',
        saturday: 'Sobota',
        sunday: 'Niedziela',
        startTime: 'Godzina początkowa',
        endTime: 'Godzina końcowa',
    },

    // ============= REPORTS PAGE =============
    reports: {
        title: 'Raporty',
        generateReport: 'Generuj raport',
        myReport: 'Mój raport',
        teamReport: 'Raport zespołu',
        leaveReport: 'Raport urlopów',
        worklogReport: 'Raport czasu pracy',
        periodFrom: 'Okres od',
        periodTo: 'Okres do',
        download: 'Pobierz',
        export: 'Eksportuj',
        changeSuccessAlert: 'Prośba o zmianę godzin została pomyślnie wysłana!',
        changeErrorAlert: 'Wystąpił błąd podczas wysyłania prośby.',
        changeWorkHoursTitle: 'Zmiana godzin pracy',
        whenDate: 'Kiedy (Data)',
        newHoursFrom: 'Nowe godziny (Od)',
        newHoursTo: 'Nowe godziny (Do)',
        reason: 'Powód',
        reasonPlaceholder: 'Opisz dlaczego chcesz zmienić godziny...'
    },

    // ============= USER MANAGEMENT =============
    userManagement: {
        title: 'Zarządzanie Użytkownikami',
        addUser: 'Dodaj użytkownika',
        editUser: 'Edytuj użytkownika',
        deleteUser: 'Usuń użytkownika',
        firstName: 'Imię',
        lastName: 'Nazwisko',
        email: 'Email',
        role: 'Rola',
        department: 'Dział',
        active: 'Aktywny',
        inactive: 'Nieaktywny',
        admin: 'Administrator',
        hr: 'HR',
        manager: 'Kierownik',
        employee: 'Pracownik',
        users: 'Użytkownicy',
        deleteConfirm: 'Czy na pewno chcesz usunąć tego użytkownika?',
        deleteSuccess: 'Użytkownik usunięty.',
        userCreated: 'Użytkownik utworzony.',
        userUpdated: 'Użytkownik zaktualizowany.',
    },

    // ============= POLICIES PAGE =============
    policies: {
        title: 'Panel Administratora',
        leavePolicy: 'Polityka urlopów',
        workPolicy: 'Polityka pracy',
        holidays: 'Święta państwowe',
        managers: 'Kierownicy',
        policyName: 'Nazwa polityki',
        description: 'Opis',
        createPolicy: 'Utwórz politykę',
        editPolicy: 'Edytuj politykę',
        deletePolicy: 'Usuń politykę',
        setPolicySuccess: 'Polityka ustawiona.',
        setManagerSuccess: 'Kierownik przypisany.',
        deleteSuccess: 'Polityka usunięta.',
    },

    // ============= SETTINGS PAGE =============
    settings: {
        title: 'Ustawienia',
        myProfile: 'Mój profil',
        changePassword: 'Zmień hasło',
        preferences: 'Preferencje',
        language: 'Język',
        theme: 'Motyw',
        notifications: 'Powiadomienia',
        currentPassword: 'Obecne hasło',
        newPassword: 'Nowe hasło',
        confirmPassword: 'Potwierdź hasło',
        changePasswordButton: 'Zmień hasło',
        passwordChanged: 'Hasło zmienione pomyślnie.',
        invalidPassword: 'Obecne hasło jest nieprawidłowe.',
        passwordMismatch: 'Hasła nie pasują do siebie.',
        light: 'Jasny',
        dark: 'Ciemny',
    },

    // ============= LOGS PAGE =============
    logs: {
        title: 'Logi Systemowe',
        action: 'Akcja',
        user: 'Użytkownik',
        timestamp: 'Czas',
        details: 'Szczegóły',
        filter: 'Filtruj',
        noLogs: 'Brak logów.',
    },

    // ============= MANAGER PAGE =============
    manager: {
        title: 'Czas pracy zespołu',
        teamMembers: 'Członkowie zespołu',
        memberSchedule: 'Harmonogram członka',
        approveRequest: 'Zatwierdź wniosek',
        rejectRequest: 'Odrzuć wniosek',
        noTeamMembers: 'Brak członków zespołu.',
    },

    // ============= ERRORS =============
    errors: {
        roleVerificationFailed: 'Błąd weryfikacji roli',
        roleVerificationFailedAfterLogin: 'Błąd weryfikacji roli po logowaniu',
        failedToFetchRole: 'Nie udało się pobrać roli',
        networkError: 'Błąd sieci. Sprawdź połączenie.',
        serverError: 'Błąd serwera. Spróbuj później.',
        unauthorized: 'Brak uprawnień.',
        notFound: 'Nie znaleziono.',
        genericError: 'Coś poszło nie tak.',
    },

    // ============= CHANGE PASSWORD MODAL =============
    changePasswordModal: {
        title: 'Zmiana hasła',
        subtitle: 'Jesteś zmuszony zmienić hasło przed dalszym korzystaniem z aplikacji.',
        currentPassword: 'Obecne hasło',
        newPassword: 'Nowe hasło',
        confirmPassword: 'Potwierdź nowe hasło',
        changeButton: 'Zmień hasło',
        success: 'Hasło zmienione pomyślnie.',
        error: 'Błąd podczas zmiany hasła.',
        passwordMismatch: 'Hasła nie pasują do siebie.',
        invalidPassword: 'Nieprawidłowe hasło.',
    },
};
