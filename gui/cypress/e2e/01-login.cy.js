describe('Logowanie, język i motyw', () => {
    beforeEach(() => {
        cy.mockApi();
        cy.visit('/login');
    });

    it('zmienia język strony logowania na niemiecki i angielski', () => {
        cy.contains('h1', 'Logowanie do Chrono').should('be.visible');

        cy.get('select[aria-label="Język"]').select('Deutsch');
        cy.contains('h1', 'Anmeldung bei Chrono').should('be.visible');
        cy.contains('button', 'Anmelden').should('be.visible');

        cy.get('select[aria-label="Sprache"]').select('English');
        cy.contains('h1', 'Login to Chrono').should('be.visible');
    });

    it('przełącza motyw na ciemny i z powrotem', () => {
        cy.get('html').should('have.attr', 'data-theme', 'light');
        cy.get('button[aria-label="Przełącz na ciemny motyw"]').click();
        cy.get('html').should('have.attr', 'data-theme', 'dark');
        cy.get('button[aria-label="Przełącz na jasny motyw"]').click();
        cy.get('html').should('have.attr', 'data-theme', 'light');
    });

    it('pokazuje błąd przy niepoprawnych danych', () => {
        cy.intercept('POST', '**/api/auth/login', { statusCode: 401, body: {} }).as('login');

        cy.get('input[type="email"]').type('kierownik@chrono.test');
        cy.get('input[autocomplete="current-password"]').type('zle-haslo');
        cy.contains('button', 'Zaloguj się').click();

        cy.wait('@login');
        cy.get('.login-error').should('contain', 'Błędny email lub hasło');
        cy.location('pathname').should('eq', '/login');
    });

    it('loguje się i przechodzi na pulpit', () => {
        cy.intercept('POST', '**/api/auth/login', {
            token: 'cypress-token', refreshToken: 'cypress-refresh', roleName: 'ROLE_MANAGER',
        }).as('login');

        cy.get('input[type="email"]').type('kierownik@chrono.test');
        cy.get('input[autocomplete="current-password"]').type('haslo-testowe');
        cy.contains('button', 'Zaloguj się').click();

        cy.wait('@login').its('request.body').should('deep.equal', {
            email: 'kierownik@chrono.test', password: 'haslo-testowe',
        });
        cy.location('pathname').should('eq', '/');
        cy.get('.sidebar').contains('Pulpit').should('be.visible');
    });
});
