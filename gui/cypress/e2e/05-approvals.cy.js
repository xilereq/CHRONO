describe('Masowa akceptacja wniosków', () => {
    beforeEach(() => {
        cy.mockApi({ role: 'ROLE_MANAGER' });
        cy.intercept('GET', '**/api/applications/manager/pending', { fixture: 'pendingApplications.json' }).as('pending');
        cy.visitAs('/manager');
        cy.contains('button', 'Wnioski pracownicze').click();
        cy.wait('@pending');
    });

    it('zaznacza wnioski i zatwierdza wybrane jednym przyciskiem', () => {
        cy.intercept('POST', '**/api/applications/bulk-approve', (req) => {
            req.reply({ processed: req.body.applicationIds.map(id => ({ id, status: 'APPROVED' })), failed: [] });
        }).as('bulkApprove');

        cy.get('input[aria-label="Zaznacz wniosek: ola.zielinska@chrono.test"]').check();
        cy.get('input[aria-label="Zaznacz wniosek: marek.wojcik@chrono.test"]').check();
        cy.contains('.bulk-bar-count', 'Zaznaczono: 2').should('be.visible');

        cy.contains('button', 'Akceptuj wybrane (2)').click();
        cy.get('.bulk-confirm input').type('Akceptacja testowa');
        cy.get('.bulk-confirm').contains('button', 'Potwierdź').click();

        cy.wait('@bulkApprove').its('request.body').should('deep.equal', {
            applicationIds: [101, 102], decisionReason: 'Akceptacja testowa',
        });
        cy.contains('.bulk-result', 'Zatwierdzono wnioski: 2.').should('be.visible');
    });

    it('pokazuje, których wniosków nie udało się zatwierdzić', () => {
        cy.intercept('POST', '**/api/applications/bulk-approve', {
            processed: [{ id: 101, status: 'APPROVED' }],
            failed: [{ applicationId: 103, errorCode: 'APPLICATION_ALREADY_PROCESSED', message: 'x' }],
        }).as('bulkApprove');

        cy.get('input[aria-label="Zaznacz wszystkie oczekujące na tej stronie"]').check();
        cy.contains('button', 'Akceptuj wybrane (3)').click();
        cy.get('.bulk-confirm').contains('button', 'Potwierdź').click();

        cy.wait('@bulkApprove');
        cy.contains('.bulk-result', 'Zatwierdzono 1 z 3 wniosków.').should('be.visible');
        cy.get('.bulk-result-list').should('contain', 'Wniosek został już rozpatrzony');
    });

    it('odznacza wszystko przyciskiem Odznacz', () => {
        cy.get('input[aria-label="Zaznacz wszystkie oczekujące na tej stronie"]').check();
        cy.contains('.bulk-bar-count', 'Zaznaczono: 3').should('be.visible');
        cy.contains('.bulk-bar button', 'Odznacz').click();
        cy.get('.bulk-bar').should('not.exist');
    });
});
