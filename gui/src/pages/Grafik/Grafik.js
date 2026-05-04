import React, { useState } from 'react';
import '../main.css';
import './Grafik.css';

function generateMonth(year, month) {
    const first = new Date(year, month, 1);
    const last = new Date(year, month + 1, 0);
    const days = [];

    const startDay = first.getDay();
    for (let i = 0; i < startDay; i++) {
        days.push(null);
    }

    for (let d = 1; d <= last.getDate(); d++) {
        days.push(new Date(year, month, d));
    }

    while (days.length % 7 !== 0) days.push(null);
    return days;
}

const sampleEvents = {
    '2026-04-05': [{ id: 1, title: 'Spotkanie z zespołem', time: '10:00' }],
    '2026-04-12': [{ id: 2, title: 'Wizyta u klienta', time: '09:00' }],
};

const Grafik = () => {
    const today = new Date();
    const [view, setView] = useState({ year: today.getFullYear(), month: today.getMonth() });
    const [selectedDate, setSelectedDate] = useState(null);

    const days = generateMonth(view.year, view.month);

    const monthName = new Date(view.year, view.month).toLocaleString('default', { month: 'long' });

    const prevMonth = () => {
        const m = view.month - 1;
        if (m < 0) setView({ year: view.year - 1, month: 11 });
        else setView({ ...view, month: m });
    }
    const nextMonth = () => {
        const m = view.month + 1;
        if (m > 11) setView({ year: view.year + 1, month: 0 });
        else setView({ ...view, month: m });
    }

    const formatKey = (d) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;

    return (
        <div className="grafik-root">
            <div className="grafik-header">
                <div className="grafik-header-left">
                    <button className="btn" onClick={prevMonth}>◀</button>
                    <div className="grafik-title">{monthName} {view.year}</div>
                    <button className="btn" onClick={nextMonth}>▶</button>
                </div>
                <div>
                    <button className="btn btn-outline" onClick={() => { setView({ year: today.getFullYear(), month: today.getMonth() }); setSelectedDate(null); }}>Dziś</button>
                </div>
            </div>

            <div className="grafik-scroll">
                <div className="grafik-grid">
                    {['Ndz', 'Pon', 'Wt', 'Śr', 'Czw', 'Pt', 'Sob'].map(d => (
                        <div key={d} className="grafik-weekday">{d}</div>
                    ))}

                    {days.map((d, idx) => {
                        const key = d ? formatKey(d) : `empty-${idx}`;
                        const events = d ? (sampleEvents[key] || []) : [];
                        const isToday = d && d.toDateString() === today.toDateString();
                        const cellClass = d ? `grafik-cell` : `grafik-cell grafik-cell--empty`;
                        return (
                            <div
                                key={key}
                                className={cellClass}
                                onClick={() => d && setSelectedDate(d)}
                            >
                                {d && (
                                    <div className="grafik-cell-inner">
                                        <div className="grafik-cell-top">
                                            <div className={`day-number ${isToday ? 'day-number--today' : ''}`}>{d.getDate()}</div>
                                            <div className="day-events">
                                                {events.length > 0 && (
                                                    <span className="event-badge">{events.length}</span>
                                                )}
                                            </div>
                                        </div>

                                        <div className="event-line">
                                            {events.length > 0 ? `${events[0].time} — ${events[0].title}` : ''}
                                        </div>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>

            <div className="grafik-panel-wrap">
                {selectedDate ? (
                    <div className="grafik-panel">
                        <h3 className="grafik-panel-title">Wydarzenia — {selectedDate.toDateString()}</h3>
                        { (sampleEvents[formatKey(selectedDate)] || []).length === 0 ? (
                            <div>Brak wydarzeń tego dnia.</div>
                        ) : (
                            <ul>
                                {(sampleEvents[formatKey(selectedDate)] || []).map(ev => (
                                    <li key={ev.id}>{ev.time} — {ev.title}</li>
                                ))}
                            </ul>
                        )}
                    </div>
                ) : (
                    <div className="grafik-panel grafik-panel--empty">Wybierz dzień by zobaczyć wydarzenia</div>
                )}
            </div>
        </div>
    );
}

export default Grafik;
