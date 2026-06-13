import React, { useState, useEffect } from 'react';
import { logService } from '../../services';
import '../../components/requests/requests.css';
import '../main.css';
import './Logs.css';

const Logs = () => {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);

    const [filterAction, setFilterAction] = useState("");
    const [filterEmail, setFilterEmail] = useState("");
    const [filterDate, setFilterDate] = useState("");
    const [filterTimeFrom, setFilterTimeFrom] = useState("");
    const [filterTimeTo, setFilterTimeTo] = useState("");

    const [uniqueActions, setUniqueActions] = useState([]);
    const [uniqueEmails, setUniqueEmails] = useState([]);

    useEffect(() => {
        const fetchLogs = async () => {
            try {
                const data = await logService.getAllLogs();

                const sortedData = data.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
                setLogs(sortedData);

                const actions = [...new Set(sortedData.map(log => log.action).filter(Boolean))];
                setUniqueActions(actions);

                const emails = [...new Set(sortedData.map(log => log.userEmail).filter(Boolean))];
                setUniqueEmails(emails);
            } catch (error) {
                console.error(error);
            } finally {
                setLoading(false);
            }
        };

        fetchLogs();
    }, []);

    const handleClearFilters = () => {
        setFilterAction("");
        setFilterEmail("");
        setFilterDate("");
        setFilterTimeFrom("");
        setFilterTimeTo("");
    };

    const filteredLogs = logs.filter(log => {
        const matchAction = filterAction === "" || log.action === filterAction;
        const matchEmail = filterEmail === "" || log.userEmail === filterEmail;

        let matchDate = true;
        let matchTime = true;

        if (log.timestamp) {
            const [logDate, logTimeFull] = log.timestamp.split('T');
            const logTime = logTimeFull ? logTimeFull.substring(0, 5) : "";

            if (filterDate !== "") {
                matchDate = logDate === filterDate;
            }
            if (filterTimeFrom !== "") {
                matchTime = matchTime && logTime >= filterTimeFrom;
            }
            if (filterTimeTo !== "") {
                matchTime = matchTime && logTime <= filterTimeTo;
            }
        }

        return matchAction && matchEmail && matchDate && matchTime;
    });

    const formatDateTime = (isoString) => {
        if (!isoString) return '';
        return isoString.replace('T', ' ').substring(0, 19);
    };

    return (
        <div className="requests-container">
            <h2 className="dashboard-title">Logi systemowe</h2>

            <div className="section-panel filters-bar">
                <select
                    className="filter-select"
                    value={filterAction}
                    onChange={(e) => setFilterAction(e.target.value)}
                >
                    <option value="">Wszystkie akcje</option>
                    {uniqueActions.map(action => (
                        <option key={action} value={action}>{action}</option>
                    ))}
                </select>

                <select
                    className="filter-select"
                    value={filterEmail}
                    onChange={(e) => setFilterEmail(e.target.value)}
                >
                    <option value="">Wszyscy użytkownicy</option>
                    {uniqueEmails.map(email => (
                        <option key={email} value={email}>{email}</option>
                    ))}
                </select>

                <div className="filters-date-row">
                    <input
                        type="date"
                        className="filter-input"
                        value={filterDate}
                        onChange={(e) => setFilterDate(e.target.value)}
                        title="Filtruj po dacie"
                    />
                    <input
                        type="time"
                        className="filter-input"
                        value={filterTimeFrom}
                        max={filterTimeTo || undefined}
                        onChange={(e) => {
                            const val = e.target.value;
                            setFilterTimeFrom(val);
                            if (filterTimeTo && val > filterTimeTo) setFilterTimeTo('');
                        }}
                        title="Godzina od"
                    />
                    <input
                        type="time"
                        className="filter-input"
                        value={filterTimeTo}
                        min={filterTimeFrom || undefined}
                        onChange={(e) => {
                            const val = e.target.value;
                            if (!filterTimeFrom || val >= filterTimeFrom) setFilterTimeTo(val);
                        }}
                        title="Godzina do"
                    />
                </div>

                <button
                    className="action-btn"
                    onClick={handleClearFilters}
                    style={{ height: '40px', padding: '0 1.5rem', fontWeight: '500' }}
                >
                    Wyczyść filtry
                </button>
            </div>

            <div className="section-panel">
                {loading ? (
                    <div className="empty">Pobieranie logów...</div>
                ) : filteredLogs.length === 0 ? (
                    <div className="empty">Brak logów spełniających kryteria.</div>
                ) : (
                    <div className="requests-table-container logs-table-scroll">
                        <table className="requests-table">
                            <thead>
                            <tr>
                                <th className="log-hide-mobile">ID</th>
                                <th>Data i czas</th>
                                <th>Użytkownik</th>
                                <th>Akcja</th>
                                <th className="log-hide-mobile">Szczegóły</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredLogs.map(log => (
                                <tr key={log.logID}>
                                    <td className="log-hide-mobile" style={{ fontWeight: 600 }}>#{log.logID}</td>
                                    <td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(log.timestamp)}</td>
                                    <td className="log-user-email">{log.userEmail || 'Brak danych'}</td>
                                    <td className="log-action">{log.action}</td>
                                    <td className="log-hide-mobile">{log.details}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
};

export default Logs;
