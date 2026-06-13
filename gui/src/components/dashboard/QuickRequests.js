import React, { useState } from 'react';
import { User, Clock, CalendarClock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Card, Button } from '../../components';
import ScheduleChangeForm from "./ScheduleChangeForm";

export default function QuickRequests() {
    const navigate = useNavigate();

    const [showScheduleForm, setShowScheduleForm] = useState(false);

    return (
        <Card title="SZYBKIE AKCJE">
            <div className="quick-requests-list">

                <Button
                    className="btn-large"
                    onClick={() => navigate('/requests', { state: { openForm: true, prefillType: 'HOLIDAY_LEAVE' } })}
                >
                    <User size={20} /> ZŁÓŻ WNIOSEK URLOPOWY
                </Button>

                <Button
                    className="btn-large"
                    onClick={() => navigate('/requests', { state: { openForm: true, prefillType: 'OVERTIME_TAKE' } })}
                >
                    <Clock size={20} /> ODBIERZ NADGODZINY
                </Button>

                <Button
                    className="btn-large"
                    onClick={() => setShowScheduleForm(!showScheduleForm)}
                >
                    <CalendarClock size={20} /> KOREKCJA CZASU PRACY
                </Button>

            </div>

            {showScheduleForm && (
                <div style={{
                    marginTop: '1.5rem',
                    paddingTop: '1.5rem',
                    borderTop: '1px solid var(--border-color)',
                    animation: 'fadeIn 0.3s ease-in-out'
                }}>
                    <ScheduleChangeForm
                        initialData={{ type: 'SCHEDULE_CHANGE' }}
                        onCancel={() => setShowScheduleForm(false)}
                        onSuccess={() => setShowScheduleForm(false)}
                    />
                </div>
            )}
        </Card>
    );
}
