import React from 'react';
import { User, Clock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Card, Button } from '../../components';

export default function QuickRequests() {
    const navigate = useNavigate();

    return (
        <Card title="SZYBKIE WNIOSKI">
            <div className="quick-requests-list">
                <Button
                    className="btn-large"
                    onClick={() => navigate('/requests', { state: { openForm: true, prefillType: 'HOLIDAY_LEAVE' } })}
                >
                    <User size={20} /> ZŁÓŻ WNIOSEK URLOPOWY
                </Button>
                <Button
                    className="btn-large"
                    onClick={() => navigate('/requests', { state: { openForm: true, prefillType: 'OVERTIME' } })}
                >
                    <Clock size={20} /> ODBIERZ NADGODZINY
                </Button>
            </div>
        </Card>
    );
}
