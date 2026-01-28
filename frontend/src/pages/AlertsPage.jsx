import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/client.js';
import { monthToday } from '../utils/format.js';
import { useToast } from '../components/Toast.jsx';

const AlertsPage = () => {
  const { addToast } = useToast();
  const [month, setMonth] = useState(monthToday());
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadAlerts = async () => {
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
  };

  useEffect(() => {
    loadAlerts();
  }, [month]);

  const markRead = async (alertId, isRead) => {
    try {
      const response = await apiClient.patch(`/api/alerts/${alertId}`, { isRead });
      setAlerts((prev) => prev.map((alert) => (alert.id === alertId ? response : alert)));
      addToast(isRead ? 'Alerta marcado como lido.' : 'Alerta reaberto.', 'success');
    } catch (err) {
      addToast(err.message, 'error');
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
          <input type="month" value={month} onChange={(event) => setMonth(event.target.value)} />
        </label>
      </header>

      <section className="card">
        {loading && <p className="muted">Carregando alertas...</p>}
        {error && <p className="error">{error}</p>}
        {!loading && alerts.length === 0 && <p className="muted">Nenhum alerta encontrado.</p>}
        <div className="rows">
          {alerts.map((alert) => (
            <div key={alert.id} className={`row ${alert.isRead ? 'row-muted' : ''}`}>
              <div>
                <strong>{alert.type}</strong>
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
