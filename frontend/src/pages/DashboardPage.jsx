import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../api/client.js';
import { formatCents, formatPercent, monthToday } from '../utils/format.js';
import { useToast } from '../components/Toast.jsx';

const DashboardPage = () => {
  const [month, setMonth] = useState(monthToday());
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { addToast } = useToast();

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get(`/api/dashboard/monthly?month=${month}`);
      setData(response);
    } catch (err) {
      setError(err.message);
      addToast('Erro ao carregar dashboard.', 'error');
    } finally {
      setLoading(false);
    }
  }, [month, addToast]);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p className="muted">Resumo mensal e alertas.</p>
        </div>
        <label className="inline-field">
          Mês
          <input type="month" value={month} onChange={(event) => setMonth(event.target.value)} />
        </label>
      </header>

      {loading && <p className="muted">Carregando dashboard...</p>}
      {error && <p className="error">{error}</p>}

      {data && (
        <>
          <section className="card grid-3">
            <div>
              <span className="muted">Receitas</span>
              <strong>{formatCents(data.incomeCents)}</strong>
            </div>
            <div>
              <span className="muted">Despesas</span>
              <strong>{formatCents(data.expenseCents)}</strong>
            </div>
            <div>
              <span className="muted">Saldo</span>
              <strong>{formatCents(data.netCents)}</strong>
            </div>
          </section>

          <section className="card">
            <h2>Gastos por categoria</h2>
            <div className="rows">
              {data.byCategory.map((item) => (
                <div key={item.categoryId} className="row">
                  <span>{item.categoryName}</span>
                  <strong>{formatCents(item.expenseCents)}</strong>
                </div>
              ))}
              {data.byCategory.length === 0 && <p className="muted">Nenhum gasto categorizado.</p>}
            </div>
          </section>

          <section className="card">
            <div className="section-header">
              <h2>Status de orçamentos</h2>
              <Link className="button secondary" to="/budgets">Ver orçamentos</Link>
            </div>
            <div className="rows">
              {data.budgetStatus.map((budget) => (
                <div key={budget.budgetId} className="row">
                  <div>
                    <strong>Categoria #{budget.categoryId || '—'}</strong>
                    <div className="muted">Consumo {formatCents(budget.consumptionCents)} / {formatCents(budget.limitCents)}</div>
                  </div>
                  <div className="status-chip">
                    {formatPercent(budget.percent)}
                    {(budget.triggered80 || budget.triggered100) && (
                      <span className="alert-dot" />
                    )}
                  </div>
                </div>
              ))}
              {data.budgetStatus.length === 0 && <p className="muted">Nenhum orçamento configurado.</p>}
            </div>
          </section>

          <section className="card">
            <div className="section-header">
              <h2>Alertas do mês</h2>
              <Link className="button secondary" to="/alerts">Ver alertas</Link>
            </div>
            <div className="rows">
              {data.alerts.map((alert) => (
                <div key={alert.id} className={`row ${alert.isRead ? 'row-muted' : ''}`}>
                  <div>
                    <strong>{alert.type}</strong>
                    <div className="muted">{alert.message}</div>
                  </div>
                  <span className="muted">{new Date(alert.triggeredAt).toLocaleDateString('pt-BR')}</span>
                </div>
              ))}
              {data.alerts.length === 0 && <p className="muted">Nenhum alerta disparado.</p>}
            </div>
          </section>

          <section className="card">
            <div className="section-header">
              <h2>Metas</h2>
              <Link className="button secondary" to="/goals">Ver metas</Link>
            </div>
            <div className="rows">
              {data.goalsSummary.map((goal) => (
                <div key={goal.goalId} className="row">
                  <div>
                    <strong>{goal.name}</strong>
                    <div className="muted">{formatPercent(goal.percent)} · {goal.status}</div>
                  </div>
                  <div className="muted">Necessário {formatCents(goal.neededMonthlyCents)}</div>
                </div>
              ))}
              {data.goalsSummary.length === 0 && <p className="muted">Nenhuma meta ativa.</p>}
            </div>
          </section>
        </>
      )}
    </div>
  );
};

export default DashboardPage;
