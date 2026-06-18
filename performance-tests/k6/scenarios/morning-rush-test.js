import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { vu } from 'k6/execution';

// Wczytanie testowych użytkowników (przygotowanych wcześniej)
const users = new SharedArray('test users', function () {
    return JSON.parse(open('../data/users.json'));
});

// Konfiguracja obciążenia typu "Spike Test" (Test piku)
// Symuluje nagły przyrost użytkowników w krótkim czasie (np. o godzinie 8:00)
export const options = {
    stages: [
        { duration: '10s', target: 200 }, // Błyskawiczny skok do 50 pracujących jednocześnie
        { duration: '1m', target: 200 },  // Wszyscy "klikają" przez minutę
        { duration: '10s', target: 0 },  // Szybkie wygaszenie
    ],
    thresholds: {
        http_req_duration: ['p(90)<2000'], // 90% zapytań powinno odpowiedzieć poniżej 800ms
        http_req_failed: ['rate<0.01'],   // Maksymalnie 1% błędów
    },
};

const BASE_URL = 'https://localhost:8443/api';

export default function () {
    // ZMIENIONE: Zamiast losowego użytkownika, bierzemy unikalnego dla danego wątku (VU).
    // vu.idInTest to numer od 1 do 50.
    // Odejmujemy 1, żeby pasowało do indeksów tablicy (0 do 49).
    const uniqueUser = users[(vu.idInTest - 1) % users.length];

    // --- ETAP 1: Logowanie ---
    const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
        email: uniqueUser.email,         // Używamy uniqueUser zamiast randomUser
        password: uniqueUser.password,
    }), { headers: { 'Content-Type': 'application/json' } });

    check(loginRes, {
        'Login success': (r) => r.status === 200,
    });

    if (loginRes.status !== 200) {
        sleep(1);
        return;
    }

    const token = loginRes.json('token');

    // Symulacja chwili zanim pracownik kliknie "Rozpocznij pracę"
    sleep(Math.random() * 2);

    // --- ETAP 2: ANULOWANIE PRACY (Czyszczenie bazy pod kolejną pętlę) ---
    const authHeaders = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
    };

    // Wywołujemy /work-log/cancel (według WorkLogController.java jest to zwykły POST bez body)
    const cancelWorkRes = http.post(`${BASE_URL}/work-log/cancel`, null, authHeaders);

    check(cancelWorkRes, {
        'Work cancelled successfully (200)': (r) => r.status === 200,
    });

    // Chwila przerwy przed kolejną iteracją
    sleep(1);
}
