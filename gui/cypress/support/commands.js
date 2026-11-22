Cypress.Commands.add('mockApi', ({ role = 'ROLE_MANAGER' } = {}) => {
    cy.intercept({ url: '**/api/**' }, (req) => {
        req.reply({ statusCode: 200, body: [] });
    }).as('anyApi');

    cy.intercept('GET', '**/api/users/my-role', { role }).as('myRole');
    cy.intercept('GET', '**/api/delegations/received', []).as('receivedDelegations');
    cy.intercept('GET', '**/api/google/calendar/status', { connected: false }).as('googleStatus');
});

Cypress.Commands.add('visitAs', (path, { email = 'kierownik@chrono.pl', lang = 'pl', theme = 'light' } = {}) => {
    cy.visit(path, {
        onBeforeLoad(win) {
            win.localStorage.setItem('accessToken', 'cypress-token');
            win.localStorage.setItem('userEmail', email);
            win.localStorage.setItem('app-language', lang);
            win.localStorage.setItem('app-theme', theme);
            win.EventSource = class {
                addEventListener() {}
                close() {}
            };
        },
    });
});
