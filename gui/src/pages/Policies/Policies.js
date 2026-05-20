import React, { useEffect, useState } from 'react';
import { policiesService } from '../../services';
import './policies.css';

const Policies = () => {
    const [policies, setPolicies] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const load = async () => {
            try {
                const data = await policiesService.getAll();
                setPolicies(data);
            } catch (e) {
                setError(e.message || 'Błąd');
            } finally {
                setLoading(false);
            }
        }
        load();
    }, []);

    const refresh = async () => {
        setLoading(true);
        try {
            const data = await policiesService.getAll();
            setPolicies(data);
            setError(null);
        } catch (e) {
            setError(e.message || 'Błąd');
        } finally {
            setLoading(false);
        }
    };

    const restoreSample = () => {
        policiesService._resetToSample();
        refresh();
    };

    const handleDelete = async (id) => {
        setLoading(true);
        try {
            await policiesService.remove(id);
            await refresh();
        } catch (e) {
            setError(e.message || 'Błąd podczas usuwania');
            setLoading(false);
        }
    };

    if (loading) return <div style={{padding: '2rem'}}>Ładowanie polityk...</div>;
    if (error) return <div style={{padding: '2rem', color: 'red'}}>{error}</div>;

    return (
        <div className="policies-page">
            <h2>Panel polityk</h2>
            <div style={{marginBottom: '1rem'}}>
                <button onClick={refresh} style={{marginRight: '0.5rem'}}>Odśwież</button>
                <button onClick={restoreSample}>Przywróć przykładowe</button>
            </div>
            <div className="policies-list">
                {policies.length === 0 ? (
                    <div>Brak polityk</div>
                ) : (
                    policies.map(p => (
                        <div key={p.id} className="policy-card">
                            <h3>{p.name}</h3>
                            <p>{p.description}</p>
                            <div style={{marginTop: '0.5rem'}}>
                                <button onClick={() => handleDelete(p.id)} style={{color:'red'}}>Usuń</button>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
}

export default Policies;


