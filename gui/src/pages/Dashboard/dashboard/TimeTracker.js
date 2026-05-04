import React, { useState, useEffect } from 'react';
import { Clock, Coffee, ArrowRightSquare, Briefcase, Stethoscope } from 'lucide-react';
import { Card, Button } from '../../../components';

import '../../main.css';

const TimeTracker = () => {
    const [now, setNow] = useState(new Date());

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 1000);
        return () => clearInterval(timer);
    }, []);

    const timeString = now.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

    return (
        <Card>
            <div className="time-display">
                <div className="time-header">
                    <Clock size={20} />
                    <span className="time-header-text">ROZPOCZNIJ PRACĘ</span>
                </div>
                <div className="time-clock">{timeString}</div>
            </div>

            <div className="time-actions-grid">
                <Button variant="outline"><Coffee size={18} /> PRZERWA</Button>
            </div>
        </Card>
    );
}

export default TimeTracker;
