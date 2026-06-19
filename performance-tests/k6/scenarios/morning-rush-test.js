import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { vu } from 'k6/execution';

const users = new SharedArray('test users', function () {
    return JSON.parse(open('../data/users.json'));
});

export const options = {
    stages: [
        { duration: '10s', target: 200 },
        { duration: '1m', target: 200 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(90)<2000'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'https://localhost:8443/api';

export default function () {
    const uniqueUser = users[(vu.idInTest - 1) % users.length];

    const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
        email: uniqueUser.email,
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

    sleep(Math.random() * 2);

    const authHeaders = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`,
        },
    };

    const cancelWorkRes = http.post(`${BASE_URL}/work-log/cancel`, null, authHeaders);

    check(cancelWorkRes, {
        'Work cancelled successfully (200)': (r) => r.status === 200,
    });

    sleep(1);
}
