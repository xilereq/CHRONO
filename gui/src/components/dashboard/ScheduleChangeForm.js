import React, { useState } from 'react';
import { PlusCircle } from 'lucide-react';
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
        oldStartTime: '',
        oldEndTime: '',
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
        <div className="section-panel form-top request-form-container narrow-form-container">
            <h3 className="section-title">{t('schedule.changeWorkHoursTitle', 'Zmiana godzin pracy')}</h3>
            {error && <div className="login-error" style={{ marginBottom: '1rem' }}>{error}</div>}

            <form onSubmit={handleSubmit} className="request-form">
                <div className="request-form-grid">
                    <label className="field" style={{ gridColumn: 'span 2' }}>
                        <span className="field-label">{t('schedule.whenDate', 'Kiedy (Data)')}</span>
                        <input
                            className="field-input"
                            type="date"
                            name="date"
                            value={formData.date}
                            onChange={handleChange}
                            required
                        />
                    </label>

                    <div className="field-row" style={{ gridColumn: 'span 2', marginTop: '-0.5rem' }}>
                        <label className="field field-flex">
                            <span className="field-label">{t('schedule.newHoursFrom', 'Nowe godziny (Od)')}</span>
                            <input className="field-input" type="time" name="newStartTime" value={formData.newStartTime} onChange={handleChange} required />
                        </label>
                        <label className="field field-flex">
                            <span className="field-label">{t('schedule.newHoursTo', 'Nowe godziny (Do)')}</span>
                            <input className="field-input" type="time" name="newEndTime" value={formData.newEndTime} onChange={handleChange} required />
                        </label>
                    </div>

                    <label className="field field-full-width">
                        <span className="field-label">{t('schedule.reason', 'Powód')}</span>
                        <textarea
                            className="field-input textarea-input"
                            name="reason"
                            value={formData.reason}
                            onChange={handleChange}
                            required
                            placeholder={t('schedule.reasonPlaceholder', 'Opisz dlaczego chcesz zmienić godziny...')}
                        />
                    </label>
                </div>

                <div className="form-actions-group">
                    <Button type="submit" variant="primary" className="btn-submit-size" disabled={loading}>
                        {loading ? t('common.sending', 'Wysyłanie...') : (
                            <>
                                <PlusCircle size={18} />
                                <span>{t('common.submit', 'Wyślij')}</span>
                            </>
                        )}
                    </Button>
                    <button type="button" className="action-btn btn-cancel-custom" onClick={onCancel}>
                        {t('common.cancel', 'Anuluj')}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default ScheduleChangeForm;
