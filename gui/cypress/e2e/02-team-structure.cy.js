describe('Struktura zespołu i lista kontaktów', () => {
    beforeEach(() => {
        cy.mockApi({ role: 'ROLE_MANAGER' });
        cy.intercept('GET', '**/api/users/team-tree*', { fixture: 'teamTree.json' }).as('teamTree');
        cy.visitAs('/');
        cy.get('.sidebar').contains('Struktura zespołu').click();
        cy.wait('@teamTree');
    });

    it('wyświetla diagram i pozwala zwijać oraz rozwijać gałęzie', () => {
        cy.location('pathname').should('eq', '/zespol');
        cy.get('.org-card').contains('Anna Kowalska').should('be.visible');
        cy.get('.org-card').contains('Ola Zielinska').should('be.visible');

        cy.get('button[aria-label="Zwiń: Kasia Nowak"]').click();
        cy.get('.org-card').contains('Ola Zielinska').should('not.exist');

        cy.get('button[aria-label="Rozwiń: Kasia Nowak"]').click();
        cy.get('.org-card').contains('Ola Zielinska').should('be.visible');
    });

    it('wyszukiwarka podświetla znalezioną osobę', () => {
        cy.get('button[aria-label="Zwiń: Kasia Nowak"]').click();
        cy.get('input[aria-label="Szukaj osoby lub e-maila…"]').type('wojcik');

        cy.contains('Znaleziono: 1').should('be.visible');
        cy.get('.org-card--match').should('have.length', 1).and('contain', 'Marek Wojcik');
    });

    it('karta kontaktu pokazuje e-mail i przełożonego', () => {
        cy.get('.org-card').contains('Ola Zielinska').click();

        cy.get('.contact-card').within(() => {
            cy.contains('h3', 'Ola Zielinska');
            cy.get('a[href="mailto:ola.zielinska@chrono.test"]').should('be.visible');
            cy.contains('button', 'Kasia Nowak').click();
            cy.contains('h3', 'Kasia Nowak');
        });

        cy.get('.contact-card-close').click();
        cy.get('.contact-card').should('not.exist');
    });

    it('lista kontaktów zawiera wszystkie osoby i sortuje się po kolumnie', () => {
        cy.contains('[role="tab"]', 'Lista kontaktów').click();
        cy.get('.contact-list tbody tr').should('have.length', 6);
        cy.get('.contact-list tbody tr').first().should('contain', 'Anna Kowalska');

        cy.contains('.report-sort-btn', 'Imię i nazwisko').click();
        cy.get('.contact-list tbody tr').first().should('contain', 'Piotr Adamski');

        cy.get('input[aria-label="Szukaj osoby lub e-maila…"]').type('kaminska');
        cy.get('.contact-list tbody tr').should('have.length', 1).and('contain', 'Ewa Kaminska');
    });
});

describe('Struktura zespołu: dostęp', () => {
    it('pracownik bez uprawnień nie widzi pozycji w menu', () => {
        cy.mockApi({ role: 'ROLE_EMPLOYEE' });
        cy.visitAs('/', { email: 'pracownik@chrono.test' });
        cy.get('.sidebar').contains('Pulpit').should('be.visible');
        cy.get('.sidebar').contains('Struktura zespołu').should('not.exist');
    });
});
