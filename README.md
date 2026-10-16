# CHRONO (Czas, Harmonogram, Raporty, Obecności, Nadgodziny, Odbiory) 

CHRONO Webowy rejestrator czasu pracy z rozliczaniem nadgodzin i obsługą absencji 

Cel projektu: Celem projektu jest stworzenie aplikacji webowej do ewidencjonowania czasu pracy, która automatycznie rozlicza nadgodziny oraz umożliwia ich odbiór, a także rejestruje wyjścia prywatne, badania lekarskie i delegacje. System zapewni workflow wniosków i akceptacji (pracownik -> przełożony), generowanie raportów dziennych/miesięcznych oraz eksport danych (CSV/XLSX) do działu kadr. Aplikacja będzie wyposażona w panel administracyjny do konfiguracji polityk czasu pracy (limity, słowniki absencji) oraz mechanizmy zgodności z RODO (rola użytkownika, retencja danych, rejestr operacji). 

Osoby: Edyta Szymańska 251647, Bogdan Banaszczyk 252925, Wiktor Pankanin 251606, Arkadiusz Grabarczyk 251523, Jakub Dłubak 251502 

Konfiguracja projektu z wykorzystaniem Dockera:

- wymagane: Docker i Docker Compose na systemie lokalnym
- z katalogu projektu uruchomić polecenie: `docker-compose up -d --build`
- po uruchomieniu kontenerów, aplikacja będzie dostępna pod adresem: `http://localhost:3000` (frontend) oraz `http://localhost:8443` (backend).
