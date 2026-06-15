import React, { useState } from 'react';
import { PlusCircle, CalendarClock, Calendar, Clock, AlignLeft } from 'lucide-react'; // Dodano nowe ikony
import { workLogRequestService } from '../../services';
import { Button } from '../../components';
import { useLanguage } from '../../context/LanguageContext';
import '../requests/requests.css';

const ScheduleChangeForm = ({ onCancel, onSuccess }) => {
    const { t } = useLanguage();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const [formData, setFormData] = useState({
        date: '',
        newStartTime: '',
        newEndTime: '',
        reason: ''
    });

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const formatDateTime = (dateStr, timeStr) => {
                const formattedTime = timeStr.length === 5 ? `${timeStr}:00` : timeStr;
                return `${dateStr}T${formattedTime}`;
            };

            const payload = {
                proposedStartTime: formatDateTime(formData.date, formData.newStartTime),
                proposedEndTime: formatDateTime(formData.date, formData.newEndTime),
                date: formData.date,
                reason: formData.reason
            };
            await workLogRequestService.submitRequest(payload);

            alert(t('schedule.changeSuccessAlert', 'Prośba o zmianę godzin została pomyślnie wysłana!'));
            if (onSuccess) onSuccess();
        } catch (err) {
            setError(err.message || t('schedule.changeErrorAlert', 'Wystąpił błąd podczas wysyłania prośby.'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="section-panel form-top request-form-container narrow-form-container" style={{ padding: '1.5rem', borderRadius: '12px', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05)' }}>

            <h3 className="section-title" style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '1.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '1rem' }}>
                <CalendarClock size={24} style={{ color: 'var(--primary-color, #E6B800)' }} />
                {t('schedule.changeWorkHoursTitle', 'Zmiana godzin pracy')}
            </h3>

            {error && <div className="login-error" style={{ marginBottom: '1rem' }}>{error}</div>}

            <form onSubmit={handleSubmit} className="request-form">
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>

                    <label className="field" style={{ margin: 0 }}>
                        <span className="field-label" style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: '500', marginBottom: '8px' }}>
                            <Calendar size={16} style={{ color: 'var(--text-muted)' }} />
                            {t('schedule.whenDate', 'Kiedy (Data)')}
                        </span>
                        <input
                            className="field-input"
                            type="date"
                            name="date"
                            value={formData.date}
                            onChange={handleChange}
                            required
                        />
                    </label>

                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: '1fr 1fr',
                        gap: '1rem',
                        background: 'var(--bg-secondary, #f8fafc)',
                        padding: '1rem',
                        borderRadius: '8px',
                        border: '1px solid var(--border-color, #e2e8f0)'
                    }}>
                        <label className="field" style={{ margin: 0 }}>
                            <span className="field-label" style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: '500', marginBottom: '8px' }}>
                                <Clock size={16} style={{ color: 'var(--text-muted)' }} />
                                {t('schedule.newHoursFrom', 'Nowe godziny (Od)')}
                            </span>
                            <input
                                className="field-input"
                                type="time"
                                name="newStartTime"
                                value={formData.newStartTime}
                                onChange={handleChange}
                                required
                            />
                        </label>
                        <label className="field" style={{ margin: 0 }}>
                            <span className="field-label" style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: '500', marginBottom: '8px' }}>
                                <Clock size={16} style={{ color: 'var(--text-muted)' }} />
                                {t('schedule.newHoursTo', 'Nowe godziny (Do)')}
                            </span>
                            <input
                                className="field-input"
                                type="time"
                                name="newEndTime"
                                value={formData.newEndTime}
                                onChange={handleChange}
                                required
                            />
                        </label>
                    </div>

                    <label className="field" style={{ margin: 0 }}>
                        <span className="field-label" style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: '500', marginBottom: '8px' }}>
                            <AlignLeft size={16} style={{ color: 'var(--text-muted)' }} />
                            {t('schedule.reason', 'Powód')}
                        </span>
                        <textarea
                            className="field-input textarea-input"
                            name="reason"
                            value={formData.reason}
                            onChange={handleChange}
                            required
                            placeholder={t('schedule.reasonPlaceholder', 'Opisz dlaczego chcesz zmienić godziny...')}
                            style={{ minHeight: '100px', resize: 'vertical' }}
                        />
                    </label>
                </div>

                <div className="form-actions-group" style={{ marginTop: '2rem', display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                    <button
                        type="button"
                        className="action-btn btn-cancel-custom"
                        onClick={onCancel}
                        style={{ padding: '0.6rem 1.2rem', fontWeight: '500' }}
                    >
                        {t('common.cancel', 'Anuluj')}
                    </button>
                    <Button
                        type="submit"
                        variant="primary"
                        className="btn-submit-size"
                        disabled={loading}
                        style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '0.6rem 1.2rem' }}
                    >
                        {loading ? (
                            <span>{t('common.sending', 'Wysyłanie...')}</span>
                        ) : (
                            <>
                                <PlusCircle size={18} />
                                <span>{t('common.submit', 'Wyślij')}</span>
                            </>
                        )}
                    </Button>
                </div>
            </form>
        </div>
    );
};

export default ScheduleChangeForm;
