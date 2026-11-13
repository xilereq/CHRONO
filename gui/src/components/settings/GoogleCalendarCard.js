import React, { useState, useEffect, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { CalendarCheck, CalendarX, RefreshCw } from 'lucide-react';
import { Card, Button } from '..';
import googleCalendarService from '../../services/googleCalendarService';
import { useLanguage } from '../../context/LanguageContext';
import '../../pages/Settings/Settings.css';

const RESULTS = ['connected', 'denied', 'error'];

const GoogleCalendarCard = () => {
    const { t } = useLanguage();
    const location = useLocation();
    const navigate = useNavigate();

    const [connected, setConnected] = useState(null);
    const [statusError, setStatusError] = useState('');
    const [redirecting, setRedirecting] = useState(false);
    const [error, setError] = useState('');
    const [result, setResult] = useState(null);

    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const google = params.get('google');
        if (RESULTS.includes(google)) {
            setResult(google);
            params.delete('google');
            const rest = params.toString();
            navigate({ pathname: location.pathname, search: rest ? `?${rest}` : '' }, { replace: true });
        }
    }, [location.pathname, location.search, navigate]);

    const loadStatus = useCallback(async () => {
        try {
            const status = await googleCalendarService.getStatus();
            setConnected(Boolean(status?.connected));
            setStatusError('');
        } catch (err) {
            setConnected(null);
            setStatusError(err.message);
        }
    }, []);

    useEffect(() => {
        loadStatus();
    }, [loadStatus]);

    const handleConnect = async () => {
        setError('');
        setResult(null);
        setRedirecting(true);
        try {
            const url = await googleCalendarService.getAuthorizationUrl();
            window.location.assign(url);
        } catch (err) {
            setError(err.message);
            setRedirecting(false);
        }
    };

    const resultMessages = {
        connected: { type: 'success', text: t('settings.googleConnectedNotice', 'Połączono z Kalendarzem Google. Zatwierdzone urlopy i dni pracy zdalnej zostały dodane do kalendarza.') },
        denied: { type: 'error', text: t('settings.googleDeniedNotice', 'Nie udzielono dostępu do Kalendarza Google.') },
        error: { type: 'error', text: t('settings.googleErrorNotice', 'Nie udało się połączyć z Kalendarzem Google. Spróbuj ponownie.') },
    };

    return (
        <Card title={t('settings.googleCalendarTitle', 'Kalendarz Google')}>
            {result && (
                <div className={`alert alert-${resultMessages[result].type}`} role="status">
                    {resultMessages[result].text}
                </div>
            )}
            {error && <div className="alert alert-error" role="alert">{error}</div>}

            <p className="gcal-description">
                {t('settings.googleDescription', 'Po połączeniu zatwierdzone urlopy i dni pracy zdalnej są automatycznie dodawane do Twojego Kalendarza Google.')}
            </p>

            <div className="gcal-row">
                {connected === null && !statusError && (
                    <span className="gcal-status">{t('settings.googleChecking', 'Sprawdzanie połączenia...')}</span>
                )}
                {statusError && (
                    <span className="gcal-status gcal-status--error">{statusError}</span>
                )}
                {connected === true && (
                    <span className="gcal-status gcal-status--connected">
                        <CalendarCheck size={16} aria-hidden="true" />
                        {t('settings.googleConnected', 'Połączono')}
                    </span>
                )}
                {connected === false && (
                    <span className="gcal-status gcal-status--disconnected">
                        <CalendarX size={16} aria-hidden="true" />
                        {t('settings.googleNotConnected', 'Nie połączono')}
                    </span>
                )}

                <Button
                    onClick={handleConnect}
                    disabled={redirecting || (connected === null && !statusError)}
                    variant={connected ? 'outline' : 'primary'}
                    style={{ width: 'auto' }}
                >
                    {connected && <RefreshCw size={16} aria-hidden="true" />}
                    {redirecting
                        ? t('settings.googleRedirecting', 'Przekierowanie do Google...')
                        : connected
                            ? t('settings.googleReconnect', 'Połącz ponownie i zsynchronizuj')
                            : t('settings.googleConnect', 'Połącz z Kalendarzem Google')}
                </Button>
            </div>

            {connected && (
                <p className="gcal-hint">
                    {t('settings.googleReconnectHint', 'Ponowne połączenie wyśle do kalendarza wszystkie przyszłe zatwierdzone wnioski.')}
                </p>
            )}
        </Card>
    );
};

export default GoogleCalendarCard;
