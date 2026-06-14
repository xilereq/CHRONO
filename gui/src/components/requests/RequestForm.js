import React, { useState, useEffect } from 'react';
import { PlusCircle } from 'lucide-react';
import { applicationService } from '../../services';
import { Button } from '../../components';
import './requests.css';

const SINGLE_DATE_TYPES = ['EARLY_EXIT', 'OVERTIME_EARN', 'OVERTIME_TAKE'];

const RequestForm = ({ onCancel, onSuccess, initialData, oldApplicationId }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const [formData, setFormData] = useState({
        title: '',
        type: 'HOLIDAY_LEAVE',
        startDate: '',
        endDate: '',
        reason: '',
        durationHours: ''
    });

    useEffect(() => {
        if (initialData) {
            setFormData({
                title: initialData.reason || '',
                type: initialData.type || 'HOLIDAY_LEAVE',
                startDate: initialData.startDate || '',
                endDate: initialData.endDate || '',
                reason: initialData.reason || '',
                durationHours: initialData.durationSeconds
                    ? Math.round(initialData.durationSeconds / 3600).toString()
                    : ''
            });
        }
    }, [initialData]);

    const isSingleDateType = SINGLE_DATE_TYPES.includes(formData.type);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => {
            const updated = { ...prev, [name]: value };
            if (name === 'startDate' && SINGLE_DATE_TYPES.includes(updated.type)) {
                updated.endDate = value;
            }
            if (name === 'type' && SINGLE_DATE_TYPES.includes(value)) {
                updated.endDate = updated.startDate;
            }
            return updated;
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const effectiveEndDate = isSingleDateType ? formData.startDate : formData.endDate;

            if (!isSingleDateType && new Date(formData.startDate) > new Date(effectiveEndDate)) {
                throw new Error('Data zakończenia nie może być wcześniejsza niż data rozpoczęcia.');
            }

            const payload = {
                type: formData.type,
                startDate: formData.startDate,
                endDate: effectiveEndDate,
                reason: formData.reason || formData.title,
                durationSeconds: formData.durationHours ? parseInt(formData.durationHours) * 3600 : null
            };

            if (oldApplicationId) {
                await applicationService.cancelApplication(oldApplicationId);
            }

            await applicationService.createApplication(payload);
            alert(oldApplicationId
                ? 'Wniosek zaktualizowany! Nowy wniosek trafił do kierownika.'
                : 'Wniosek wysłany pomyślnie!');
            if (onSuccess) onSuccess();
        } catch (err) {
            setError(err.message || 'Wystąpił błąd podczas wysyłania wniosku.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="section-panel form-top request-form-container">
            <h3 className="section-title">Nowy Wniosek</h3>
            {error && <div className="login-error" style={{ marginBottom: '1rem' }}>{error}</div>}

            <form onSubmit={handleSubmit} className="request-form">
                <div className="request-form-grid">
                    <label className="field field-full-width">
                        <span className="field-label">Tytuł wniosku</span>
                        <input
                            className="field-input"
                            name="title"
                            value={formData.title}
                            onChange={handleChange}
                            placeholder="np. Wyjazd rodzinny"
                            required
                        />
                    </label>

                    <label className="field">
                        <span className="field-label">Typ wniosku</span>
                        <select
                            className="field-input"
                            name="type"
                            value={formData.type}
                            onChange={handleChange}
                        >
                            <option value="HOLIDAY_LEAVE">Urlop wypoczynkowy</option>
                            <option value="SICK_LEAVE">Zwolnienie lekarskie</option>
                            <option value="HOME_OFFICE">Praca zdalna (Home Office)</option>
                            <option value="EARLY_EXIT">Wcześniejsze wyjście</option>
                            <option value="OVERTIME_EARN">Zgłoszenie nadgodzin</option>
                            <option value="OVERTIME_TAKE">Odbiór nadgodzin</option>
                        </select>
                    </label>

                    {(formData.type === 'EARLY_EXIT' || formData.type === 'OVERTIME_EARN' || formData.type === 'OVERTIME_TAKE') && (
                        <label className="field">
                            <span className="field-label">Liczba godzin</span>
                            <input
                                className="field-input"
                                type="number"
                                name="durationHours"
                                value={formData.durationHours}
                                onChange={handleChange}
                                min="1"
                                required
                            />
                        </label>
                    )}

                    {isSingleDateType ? (
                        <label className="field" style={{ gridColumn: 'span 2' }}>
                            <span className="field-label">Data</span>
                            <input
                                className="field-input"
                                type="date"
                                name="startDate"
                                value={formData.startDate}
                                onChange={handleChange}
                                required
                            />
                        </label>
                    ) : (
                        <div className="field-row" style={{ gridColumn: 'span 2' }}>
                            <label className="field field-flex">
                                <span className="field-label">Od</span>
                                <input
                                    className="field-input"
                                    type="date"
                                    name="startDate"
                                    value={formData.startDate}
                                    onChange={handleChange}
                                    required
                                />
                            </label>
                            <label className="field field-flex">
                                <span className="field-label">Do</span>
                                <input
                                    className="field-input"
                                    type="date"
                                    name="endDate"
                                    value={formData.endDate}
                                    onChange={handleChange}
                                    required
                                />
                            </label>
                        </div>
                    )}

                    <label className="field field-full-width">
                        <span className="field-label">Powód (opcjonalnie)</span>
                        <textarea
                            className="field-input textarea-input"
                            name="reason"
                            value={formData.reason}
                            onChange={handleChange}
                        />
                    </label>
                </div>

                <div className="form-actions-group">
                    <Button
                        type="submit"
                        variant="primary"
                        className="btn-submit-size"
                        disabled={loading}
                    >
                        {loading ? 'Wysyłanie...' : (
                            <>
                                <PlusCircle size={18} />
                                <span>Wyślij</span>
                            </>
                        )}
                    </Button>
                    <button
                        type="button"
                        className="action-btn btn-cancel-custom"
                        onClick={onCancel}
                    >
                        Anuluj
                    </button>
                </div>
            </form>
        </div>
    );
};

export default RequestForm;
