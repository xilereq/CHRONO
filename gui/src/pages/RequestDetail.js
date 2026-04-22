import React from 'react';
import { useParams, Link } from 'react-router-dom';

const fakeData = {
    '1': { id: '1', title: 'Vacation request', status: 'Approved', from: '2026-05-01', to: '2026-05-10', reason: 'Rest' },
    '2': { id: '2', title: 'Private leave request', status: 'Pending', from: '2026-04-20', to: '2026-04-20', reason: 'Personal matters' },
    '3': { id: '3', title: 'Business trip request', status: 'Rejected', from: '2026-03-10', to: '2026-03-12', reason: 'Client meeting' },
};

const RequestDetail = () => {
    const { id } = useParams();
    const r = fakeData[id] || null;

    if (!r) return (
        <div style={{padding: '2rem'}}>Request not found. <Link to="/my-requests">Back to list</Link></div>
    );

    return (
        <div style={{padding: '2rem'}}>
            <h2 style={{marginTop: 0}}>{r.title}</h2>
            <p><strong>Status:</strong> {r.status}</p>
            <p><strong>Period:</strong> {r.from} — {r.to}</p>
            <p><strong>Reason:</strong> {r.reason}</p>
            <p><Link to="/my-requests">← Back to my requests</Link></p>
        </div>
    );
}

export default RequestDetail;
