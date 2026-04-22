import React from 'react';
import { User, Clock } from 'lucide-react';
import { Card, Button } from '../../../components';

import '../../dashboard.css';

export default function QuickRequests() {
    return (
        <Card title="SZYBKIE WNIOSKI">
            <div className="quick-requests-list">
                <Button className="btn-large">
                    <User size={20} /> ZŁÓŻ WNIOSEK URLOPOWY
                </Button>
                <Button className="btn-large">
                    <Clock size={20} /> ODBIERZ NADGODZINY
                </Button>
            </div>
        </Card>
    );
}
