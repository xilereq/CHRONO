import React, { useState, useEffect } from 'react';
import { PlusCircle } from 'lucide-react';
import { applicationService, userService } from '../../services';
import { Button } from '../../components';
import '../requests/requests.css';
import './manager.css';

const SINGLE_DATE_TYPES = ['EARLY_EXIT', 'OVERTIME_EARN', 'OVERTIME_TAKE'];

const ManagerRequestTab = () => {
    const [subordinates, setSubordinates] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    const [formData, setFormData] = useState({
        targetEmployeeEmail: '',
        title: '',
        type: 'SICK_LEAVE',
        startDate: '',
        endDate: '',
        reason: '',
        durationHours: ''
    });

    useEffect(() => {
        const fetchSubordinates = async () => {
            try {
                const data = await userService.getSubordinates();
                setSubordinates(data || []);
            } catch (err) {
                console.error("Błąd podczas pobierania podwładnych:", err);
            }
        };
        fetchSubordinates();
    }, []);

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
        setMessage('');
        setLoading(true);

        if (!formData.targetEmployeeEmail) {
            setError('Proszę wybrać pracownika.');
            setLoading(false);
            return;
        }

        try {
            const effectiveEndDate = isSingleDateType ? formData.startDate : formData.endDate;

            if (!isSingleDateType && new Date(formData.startDate) > new Date(effectiveEndDate)) {
                throw new Error('Data zakończenia nie może być wcześniejsza niż data rozpoczęcia.');
            }

            const payload = {
                targetEmail: formData.targetEmployeeEmail,
                type: formData.type,
                startDate: formData.startDate,
                endDate: effectiveEndDate,
                reason: formData.reason || formData.title,
                durationSeconds: formData.durationHours ? parseInt(formData.durationHours) * 3600 : null
            };
            await applicationService.createApplicationForEmployee(payload);

            setMessage(`Wniosek dla pracownika został pomyślnie złożony i zaakceptowany.`);

            setFormData(prev => ({ ...prev, title: '', startDate: '', endDate: '', reason: '', durationHours: '' }));

        } catch (err) {
            setError(err.message || 'Wystąpił błąd podczas wysyłania wniosku.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="policy-section">
            <h2 className="section-title">Złóż wniosek za pracownika</h2>
            <p className="section-subtitle mb-3">Wybierz członka swojego zespołu, aby złożyć w jego imieniu wniosek (np. o nagłą nieobecność lub L4). Wniosek ten z reguły zostanie automatycznie zatwierdzony.</p>

            {error && <div className="manager-alert-error">{error}</div>}
            {message && <div className="manager-alert-success"><strong>{message}</strong></div>}

            <form onSubmit={handleSubmit} className="request-form" style={{ marginTop: '1.5rem' }}>
                <div className="request-form-grid">

                    <label className="field field-full-width">
                        <span className="field-label" style={{ color: 'var(--primary-color)' }}>Wybierz pracownika z zespołu *</span>
                        <select
                            className="field-input"
                            name="targetEmployeeEmail"
                            value={formData.targetEmployeeEmail}
                            onChange={handleChange}
                            required
                            style={{ border: '1px solid var(--primary-color)' }}
                        >
                            <option value="">-- Wybierz pracownika --</option>
                            {subordinates.map(sub => (
                                <option key={sub.email} value={sub.email}>
                                    {sub.name} {sub.surname} ({sub.email})
                                </option>
                            ))}
                        </select>
                    </label>

                    <label className="field field-full-width">
                        <span className="field-label">Tytuł wniosku</span>
                        <input
                            className="field-input"
                            name="title"
                            value={formData.title}
                            onChange={handleChange}
                            placeholder="np. Nagłe zachorowanie"
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
                            <option value="SICK_LEAVE">Zwolnienie lekarskie / L4</option>
                            <option value="EARLY_EXIT">Wcześniejsze wyjście</option>
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
                                <input className="field-input" type="date" name="startDate" value={formData.startDate} onChange={handleChange} required />
                            </label>
                            <label className="field field-flex">
                                <span className="field-label">Do</span>
                                <input className="field-input" type="date" name="endDate" value={formData.endDate} onChange={handleChange} required />
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

                <div className="policies-footer">
                    <div style={{ flex: 1 }}></div>
                    <div className="footer-actions">
                        <Button type="submit" variant="primary" disabled={loading} style={{ padding: '0.6rem 1.5rem', borderRadius: '8px' }}>
                            {loading ? 'Przetwarzanie...' : (
                                <>
                                    <PlusCircle size={18} style={{ marginRight: '0.5rem' }} />
                                    <span>Złóż wniosek za pracownika</span>
                                </>
                            )}
                        </Button>
                    </div>
                </div>
            </form>
        </div>
    );
};

export default ManagerRequestTab;
