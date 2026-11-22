describe('Tabele raportów', () => {
    beforeEach(() => {
        cy.mockApi({ role: 'ROLE_MANAGER' });
        cy.intercept('GET', '**/api/reports/workload/team/data*', { fixture: 'workloadReport.json' }).as('workload');
        cy.intercept('GET', '**/api/reports/absences/team/data*', { fixture: 'absencesReport.json' }).as('absences');
        cy.visitAs('/raporty');
        cy.contains('button', 'Raporty zespołu').click();
    });

    it('pokazuje raport w tabeli z paginacją', () => {
        cy.contains('button', 'Obciążenie').click();
        cy.contains('button', 'Pokaż w tabeli').click();
        cy.wait('@workload');

        cy.contains('.report-table-title', 'Raport Obciążenia Zespołu').should('be.visible');
        cy.get('.report-data-table tbody tr').should('have.length', 10);
        cy.get('.report-page-info').scrollIntoView().should('be.visible').and('contain', '1–10 z 12');

        cy.get('button[aria-label="Następna strona"]').click();
        cy.get('.report-data-table tbody tr').should('have.length', 2);

        cy.get('.report-page-size-select').select('25');
        cy.get('.report-data-table tbody tr').should('have.length', 12);
    });

    it('sortuje i przeszukuje tabelę', () => {
        cy.contains('button', 'Obciążenie').click();
        cy.contains('button', 'Pokaż w tabeli').click();
        cy.wait('@workload');

        cy.contains('.report-sort-btn', 'Imię i Nazwisko').click();
        cy.get('.report-data-table tbody tr').first().should('contain', 'Adam Kot');
        cy.contains('.report-sort-btn', 'Imię i Nazwisko').click();
        cy.get('.report-data-table tbody tr').first().should('contain', 'Zofia Mak');

        cy.get('input[aria-label="Szukaj w raporcie…"]').type('sowa');
        cy.get('.report-data-table tbody tr').should('have.length', 1).and('contain', 'Iga Sowa');
        cy.contains('button', 'Wyczyść filtry').click();
        cy.contains('.report-count', '12 wierszy').should('be.visible');
    });

    it('filtruje po wartości kolumny i tłumaczy kody', () => {
        cy.contains('button', 'Absencje').click();
        cy.contains('button', 'Pokaż w tabeli').click();
        cy.wait('@absences');

        cy.get('.report-data-table').should('contain', 'Urlop wypoczynkowy');
        cy.get('select[aria-label="Status"]').select('Zatwierdzony');
        cy.get('.report-data-table tbody tr').should('have.length', 2);
        cy.contains('.report-count', '2 z 4 wierszy').should('be.visible');
    });

    it('pobiera raport w wybranym formacie', () => {
        cy.intercept('GET', '**/api/reports/night-hours/team?*', {
            statusCode: 200, body: 'plik', headers: { 'content-type': 'text/csv' },
        }).as('download');

        cy.contains('button', 'Godziny nocne').click();
        cy.contains('button', 'XLSX').click();
        cy.contains('button', 'Pobierz raport').click();

        cy.wait('@download').its('request.url').should('include', 'format=XLSX');
    });
});
