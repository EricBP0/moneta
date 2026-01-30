import React, { useCallback, useEffect, useState } from 'react';
import { apiClient } from '../api/client.js';
import { monthToday } from '../utils/format.js';
import { useToast } from '../components/Toast.jsx';
import DatePicker from '../components/DatePicker.jsx';
import { getAlertLabel } from '../constants/labels.js';

const AlertsPage = () => {
  const { addToast } = useToast();
  const [month, setMonth] = useState(monthToday());
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadAlerts = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await apiClient.get(`/api/alerts?month=${month}`);
      setAlerts(data || []);
    } catch (err) {
      setError(err.message);
      addToast('Erro ao carregar alertas.', 'error');
    } finally {
      setLoading(false);
    }
  }, [month, addToast]);

  useEffect(() => {
    loadAlerts();
  }, [loadAlerts]);

  const markRead = async (alertId, isRead) => {
    try {
      const response = await apiClient.patch(`/api/alerts/${alertId}`, { isRead });
      setAlerts((prev) => prev.map((alert) => (alert.id === alertId ? response : alert)));
      addToast(isRead ? 'Alerta marcado como lido.' : 'Alerta reaberto.', 'success');
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const markAllRead = async () => {
    const unread = alerts.filter((alert) => !alert.isRead);
    if (unread.length === 0) {
      addToast('Nenhum alerta pendente.', 'success');
      return;
    }
    setAlerts((prev) => prev.map((alert) => ({ ...alert, isRead: true })));
    try {
      await Promise.all(unread.map((alert) => apiClient.patch(`/api/alerts/${alert.id}`, { isRead: true })));
      addToast('Alertas marcados como lidos.', 'success');
    } catch (err) {
      addToast('Não foi possível marcar todos os alertas.', 'error');
      loadAlerts();
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Alertas</h1>
          <p className="muted">Acompanhe alertas de orçamento e metas.</p>
        </div>
        <label className="inline-field">
          Mês
          <DatePicker type="month" value={month} onChange={(event) => setMonth(event.target.value)} />
        </label>
      </header>

      <section className="card">
        <div className="section-header">
          <h2>Lista</h2>
          <button type="button" className="button secondary" onClick={markAllRead}>Marcar todos</button>
        </div>
        {loading && <p className="muted">Carregando alertas...</p>}
        {error && <p className="error">{error}</p>}
        {!loading && alerts.length === 0 && <p className="muted">Nenhum alerta encontrado.</p>}
        <div className="rows">
          {alerts.map((alert) => (
            <div key={alert.id} className={`row ${alert.isRead ? 'row-muted' : ''}`}>
              <div>
                <strong>{getAlertLabel(alert.type)}</strong>
                <div className="muted">{alert.message}</div>
                <div className="muted">{new Date(alert.triggeredAt).toLocaleDateString('pt-BR')}</div>
              </div>
              <div className="button-group">
                <button
                  type="button"
                  className="button secondary"
                  onClick={() => markRead(alert.id, !alert.isRead)}
                >
                  {alert.isRead ? 'Marcar como não lido' : 'Marcar como lido'}
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

export default AlertsPage;
