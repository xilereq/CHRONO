import React from 'react';
import { User, FileText, Clock, CreditCard, Settings, RotateCcw } from 'lucide-react';
import '../main.css'; // Importujemy główne style aplikacji
import './policies.css';

const Policies = () => {
    return (
        <div className="page-container"> {/* Używamy standardowego kontenera strony */}
            <div className="policies-page-wrapper">

                {/* Lewy panel użytkownika i nawigacji */}
                <div className="policies-sidebar">
                    <div className="policies-user-card">
                        <div className="user-avatar">AN</div>
                        <div className="user-info">
                            <div className="user-name">Adam Nowak</div>
                            <div className="user-role">Manager Operacyjny</div>
                        </div>
                    </div>

                    <nav className="policies-nav">
                        <button className="policies-nav-item active">
                            <User size={18} /> Zatrudnienie
                        </button>
                        <button className="policies-nav-item">
                            <FileText size={18} /> Limity wniosków
                        </button>
                        <button className="policies-nav-item">
                            <Clock size={18} /> System pracy
                        </button>
                        <button className="policies-nav-item">
                            <CreditCard size={18} /> Wynagrodzenia
                        </button>
                        <button className="policies-nav-item">
                            <Settings size={18} /> Zaawansowane
                        </button>
                        <button className="policies-nav-item">
                            <FileText size={18} /> Prywatne notatki
                        </button>
                    </nav>
                </div>

                {/* Prawy panel treści - zamknięty w "pudełko" */}
                <div className="policies-content">
                    <h1 className="policies-header">Edycja Polityki Pracowniczej: Adam Nowak</h1>

                    <div className="policy-section">
                        <h2 className="section-title">Sekcja 1: Wymiar godzinowy</h2>
                        <div className="section-actions">
                            <select className="form-select year-select">
                                <option>Rok: 2024</option>
                            </select>
                            <div className="action-buttons-right">
                                <button className="btn-text">+ Dodaj nowy zbiór</button>
                                <button className="btn-outline-rounded">Edytuj rok</button>
                            </div>
                        </div>

                        <div className="hours-config">
                            <p className="form-label">Miesięczny wymiar godzin</p>
                            <div className="form-row">
                                <div className="inline-input-group">
                                    <span>Styczeń:</span>
                                    <input type="number" className="form-input short-input" defaultValue="168" />
                                </div>
                                <div className="inline-input-group">
                                    <span>Minuty:</span>
                                    <input type="number" className="form-input short-input" defaultValue="0" />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="policy-section">
                        <div className="section-header align-center">
                            <div>
                                <h2 className="section-title">Sekcja 2: System pracy</h2>
                                <p className="section-subtitle">Określ system pracy dla pracownika (np. 8-godzinny, zmianowy).</p>
                            </div>
                            <button className="btn-gray-outline">+ Dodaj system</button>
                        </div>
                    </div>

                    <div className="policy-section">
                        <div className="section-header align-center">
                            <div>
                                <h2 className="section-title">Sekcja 3: Okres rozliczeniowy</h2>
                                <p className="section-subtitle">Wybierz okres rozliczeniowy obowiązujący dla tej polityki.</p>
                            </div>
                            <button className="btn-gray-outline">+ Dodaj okres</button>
                        </div>
                    </div>

                    <div className="policy-section">
                        <h2 className="section-title mb-3">Sekcja 4: Adres</h2>
                        <div className="grid-2-col">
                            <label className="form-group">
                                <span className="form-label">Ulica</span>
                                <input type="text" className="form-input" defaultValue="ul. Marszałkowska, 12" />
                            </label>
                            <label className="form-group">
                                <span className="form-label">Nr budynku</span>
                                <input type="text" className="form-input" defaultValue="[Warszawa]" />
                            </label>
                            <label className="form-group">
                                <span className="form-label">Kod pocztowy</span>
                                <input type="text" className="form-input" defaultValue="00-001" />
                            </label>
                            <label className="form-group">
                                <span className="form-label">Miasto</span>
                                <input type="text" className="form-input" defaultValue="Warszawa" />
                            </label>
                        </div>
                    </div>

                    <div className="policies-footer">
                        <button className="btn-text history-btn">
                            <RotateCcw size={16} /> Przeglądaj historię zmian
                        </button>
                        <div className="footer-actions">
                            <button className="btn-text cancel-btn">Anuluj</button>
                            <button className="btn-yellow">Zapisz zmiany</button>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    );
};

export default Policies;
