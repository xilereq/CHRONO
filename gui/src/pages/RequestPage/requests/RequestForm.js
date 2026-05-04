import React, { useState } from 'react';
import { PlusCircle } from 'lucide-react';
import { applicationService } from '../../../services/applicationService';
import Button from '../../../components/ui/Button';

import './requests.css';

const RequestForm = ({ onCancel, onSuccess }) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const [formData, setFormData] = useState({
        title: '',
        type: 'Urlop wypoczynkowy',
        startDate: '',
        endDate: '',
        reason: ''
    });

    const userId = 1;

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            if (new Date(formData.startDate) > new Date(formData.endDate)) {
                throw new Error('Data zakończenia nie może być wcześniejsza niż data rozpoczęcia.');
            }
            await applicationService.createApplication(userId, formData);
            alert('Wniosek został wysłany pomyślnie!');
            if (onSuccess) onSuccess();
        } catch (err) {
            setError(err.message || 'Wystąpił błąd.');
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
                            <option value="Urlop wypoczynkowy">Urlop wypoczynkowy</option>
                            <option value="Urlop na żądanie">Urlop na żądanie</option>
                            <option value="Nadgodziny">Odbiór nadgodzin</option>
                        </select>
                    </label>

                    <div className="field-row">
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
                                Wyślij
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
