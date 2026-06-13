import React, { useState } from 'react';
import { PlusCircle } from 'lucide-react';
import { applicationService } from '../../services';
import { Button } from '../../components';
import './dashboard.css';

const ScheduleChangeForm = ({ onCancel, onSuccess }) => {
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
            const payload = {
                type: 'SCHEDULE_CHANGE',
                startDate: formData.date,
                endDate: formData.date,
                title: 'Wniosek o zmianę godzin pracy',
                reason: formData.reason,
                startTime: formData.newStartTime,
                endTime: formData.newEndTime,
                oldStartTime: formData.oldStartTime,
                oldEndTime: formData.oldEndTime
            };

            await applicationService.createApplication(payload);
            alert('Prośba o zmianę godzin została wysłana do akceptacji!');
            if (onSuccess) onSuccess();
        } catch (err) {
            setError(err.message || 'Wystąpił błąd podczas wysyłania prośby.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="section-panel form-top request-form-container narrow-form-container">
            <h3 className="section-title">Zmiana godzin pracy</h3>
            {error && <div className="login-error" style={{ marginBottom: '1rem' }}>{error}</div>}

            <form onSubmit={handleSubmit} className="request-form">
                <div className="request-form-grid">
                    <label className="field" style={{ gridColumn: 'span 2' }}>
                        <span className="field-label">Kiedy (Data)</span>
                        <input
                            className="field-input"
                            type="date"
                            name="date"
                            value={formData.date}
                            onChange={handleChange}
                            required
                        />
                    </label>

                    <div className="field-row" style={{ gridColumn: 'span 2' }}>
                        <label className="field field-flex">
                            <span className="field-label">Jakie były (Od)</span>
                            <input className="field-input" type="time" name="oldStartTime" value={formData.oldStartTime} onChange={handleChange} required />
                        </label>
                        <label className="field field-flex">
                            <span className="field-label">Jakie były (Do)</span>
                            <input className="field-input" type="time" name="oldEndTime" value={formData.oldEndTime} onChange={handleChange} required />
                        </label>
                    </div>

                    <div className="field-row" style={{ gridColumn: 'span 2', marginTop: '-0.5rem' }}>
                        <label className="field field-flex">
                            <span className="field-label">Nowe godziny (Od)</span>
                            <input className="field-input" type="time" name="newStartTime" value={formData.newStartTime} onChange={handleChange} required />
                        </label>
                        <label className="field field-flex">
                            <span className="field-label">Nowe godziny (Do)</span>
                            <input className="field-input" type="time" name="newEndTime" value={formData.newEndTime} onChange={handleChange} required />
                        </label>
                    </div>

                    <label className="field field-full-width">
                        <span className="field-label">Powód</span>
                        <textarea
                            className="field-input textarea-input"
                            name="reason"
                            value={formData.reason}
                            onChange={handleChange}
                            required
                            placeholder="Opisz dlaczego chcesz zmienić godziny..."
                        />
                    </label>
                </div>

                <div className="form-actions-group">
                    <Button type="submit" variant="primary" className="btn-submit-size" disabled={loading}>
                        {loading ? 'Wysyłanie...' : (
                            <>
                                <PlusCircle size={18} />
                                <span>Wyślij</span>
                            </>
                        )}
                    </Button>
                    <button type="button" className="action-btn btn-cancel-custom" onClick={onCancel}>
                        Anuluj
                    </button>
                </div>
            </form>
        </div>
    );
};

export default ScheduleChangeForm;
