import React from 'react';
import './requests.css';
import { RequestCard } from '../../components';
import Button from '../ui/Button';

const RequestList = ({ requests }) => {
    return (
        <div className="requests-container">

            <div className="section-panel filters-bar">
                <select className="filter-select">
                    <option value="">Status</option>
                    <option value="Zatwierdzony">Zatwierdzony</option>
                    <option value="Weryfikacja">Weryfikacja</option>
                    <option value="Odrzucony">Odrzucony</option>
                </select>
                <select className="filter-select">
                    <option value="">Typ</option>
                    <option value="Urlop wypoczynkowy">Urlop wypoczynkowy</option>
                    <option value="Nadgodziny">Nadgodziny</option>
                    <option value="Urlop na żądanie">Urlop na żądanie</option>
                </select>
                <input type="date" className="filter-input" placeholder="Data zgłoszenia..." />
            </div>

            <div className="section-panel">
                <div className="section-title">Lista moich wniosków</div>

                {!requests || requests.length === 0 ? (
                    <div className="empty">Brak wniosków w wybranym okresie.</div>
                ) : (
                    <div className="requests-table-container">
                        <table className="requests-table">
                            <thead>
                            <tr>
                                <th>Typ Wniosku</th>
                                <th>Okres / Data</th>
                                <th>Liczba Dni/Godz.</th>
                                <th>Data Złożenia</th>
                                <th>Status</th>
                                <th>Zatwierdzający</th>
                                <th>Akcje</th>
                            </tr>
                            </thead>
                            <tbody>
                            {requests.map(r => (
                                <RequestCard key={r.id} request={r} />
                            ))}
                            </tbody>
                        </table>

                        {/* Paginacja na dole */}
                        <div className="pagination">
                            <span className="page-active">1</span>
                            <span>2</span>
                            <span>3</span>
                            <span>...</span>
                            <span>›</span>
                        </div>
                    </div>
                )}
            </div>

            <div className="section-panel">
                <div className="section-title">Wnioski</div>
                <div className="submit-actions">
                    <Button className="big-yellow-btn" variant="primary">
                        👤 Złóż wniosek urlopowy
                    </Button>
                    <Button className="big-yellow-btn" variant="primary">
                        🕒 Złóż wniosek o nadgodziny
                    </Button>
                </div>
            </div>

        </div>
    );
};

export default RequestList;
