describe('Połączenie z Kalendarzem Google', () => {
    beforeEach(() => {
        cy.mockApi();
    });

    it('łączy konto i pokazuje komunikat po powrocie z Google', () => {
        let connected = false;
        cy.intercept('GET', '**/api/google/calendar/status', (req) => req.reply({ connected })).as('status');
        cy.intercept('GET', '**/api/google/calendar/connect', (req) => {
            connected = true;
            req.reply({ url: '/ustawienia?google=connected' });
        }).as('connect');

        cy.visitAs('/ustawienia');
        cy.contains('.gcal-status', 'Nie połączono').should('be.visible');
        cy.contains('button', 'Połącz z Kalendarzem Google').click();

        cy.wait('@connect');
        cy.contains('Połączono z Kalendarzem Google').should('be.visible');
        cy.contains('.gcal-status', 'Połączono').should('be.visible');
        cy.contains('button', 'Połącz ponownie i zsynchronizuj').should('be.visible');
        cy.location('search').should('eq', '');
    });

    it('pokazuje informację o odmowie dostępu', () => {
        cy.visitAs('/ustawienia?google=denied');
        cy.contains('Nie udzielono dostępu do Kalendarza Google.').should('be.visible');
    });

    it('pokazuje czytelny błąd, gdy serwer nie ma konfiguracji Google', () => {
        cy.intercept('GET', '**/api/google/calendar/connect', {
            statusCode: 500, body: { code: 'GOOGLE_NOT_CONFIGURED', message: 'not configured' },
        }).as('connect');

        cy.visitAs('/ustawienia');
        cy.contains('button', 'Połącz z Kalendarzem Google').click();
        cy.wait('@connect');
        cy.contains('Integracja z Kalendarzem Google nie jest skonfigurowana na serwerze.').should('be.visible');
        cy.location('pathname').should('eq', '/ustawienia');
    });
});
