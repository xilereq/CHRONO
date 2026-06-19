import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

const users = new SharedArray('test users', function () {
    return JSON.parse(open('../data/users.json'));
});

export const options = {
    stages: [
        { duration: '15s', target: 10 },
        { duration: '30s', target: 10 },
        { duration: '15s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'https://localhost:8443/api';

export default function () {
    const randomUser = users[Math.floor(Math.random() * users.length)];

    const loginPayload = JSON.stringify({
        email: randomUser.email,
        password: randomUser.password,
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
    };

    const loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, params);

    check(loginRes, {
        'Login: status is 200': (r) => r.status === 200,
        'Login: has token': (r) => r.json('token') !== undefined,
    });

    if (loginRes.status !== 200) {
        sleep(1);
        return;
    }

    const token = loginRes.json('token');

    sleep(Math.random() * 2 + 1);

    const authHeaders = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
    };

    const workLogRes = http.get(`${BASE_URL}/work-log/monthly-stats`, authHeaders);

    check(workLogRes, {
        'Stats fetched successfully (status 200)': (r) => r.status === 200,
    });

    sleep(Math.random() * 2 + 1);
}
