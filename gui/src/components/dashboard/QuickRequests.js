import React, { useState } from 'react';
import { User, Clock, CalendarClock, DoorOpen } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Card, Button } from '../../components';
import ScheduleChangeForm from "./ScheduleChangeForm";
import { useLanguage } from '../../context/LanguageContext';

export default function QuickRequests() {
    const navigate = useNavigate();
    const { t } = useLanguage();
    const [showScheduleForm, setShowScheduleForm] = useState(false);

    const actionBtnStyle = {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1rem 0.5rem',
        gap: '8px',
        height: '100%',
        textAlign: 'center',
        lineHeight: '1.2'
    };

    return (
        <Card title={t('dashboard.quickRequestsTitle', 'SZYBKIE AKCJE')}>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '12px' }}>

                <Button
                    className="btn-outline-rounded"
                    style={actionBtnStyle}
                    onClick={() => navigate('/requests', { state: { openForm: true, prefillType: 'HOLIDAY_LEAVE' } })}
                >
                    <User size={24} />
                    <span style={{ fontSize: '0.85rem' }}>{t('dashboard.quick.leave', 'Urlop')}</span>
                </Button>

                <Button
                    className="btn-outline-rounded"
                    style={actionBtnStyle}
                    onClick={() => navigate('/requests', { state: { openForm: true, prefillType: 'OVERTIME_TAKE' } })}
                >
                    <Clock size={24} />
                    <span style={{ fontSize: '0.85rem' }}>{t('dashboard.quick.overtime', 'Odbiór godzin')}</span>
                </Button>

                <Button
                    className="btn-outline-rounded"
                    style={actionBtnStyle}
                    onClick={() => navigate('/requests', { state: { openForm: true, prefillType: 'EARLY_EXIT' } })}
                >
                    <DoorOpen size={24} />
                    <span style={{ fontSize: '0.85rem' }}>{t('dashboard.quick.earlyExit', 'Wyjście prywatne')}</span>
                </Button>

                <Button
                    className="btn-outline-rounded"
                    style={actionBtnStyle}
                    onClick={() => setShowScheduleForm(!showScheduleForm)}
                >
                    <CalendarClock size={24} />
                    <span style={{ fontSize: '0.85rem' }}>{t('dashboard.quick.schedule', 'Korekta czasu')}</span>
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
