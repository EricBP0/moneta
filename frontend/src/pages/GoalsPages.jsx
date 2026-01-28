import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { apiClient } from '../api/client.js';
import { formatCents } from '../utils/format.js';
import { useToast } from '../components/Toast.jsx';

export const GoalsPage = () => {
  const { addToast } = useToast();
  const [goals, setGoals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadGoals = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await apiClient.get('/api/goals');
      setGoals(data || []);
    } catch (err) {
      setError(err.message);
      addToast('Erro ao carregar metas.', 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    loadGoals();
  }, [loadGoals]);

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Metas</h1>
          <p>Gerencie metas, aportes e acompanhe a projeção.</p>
        </div>
        <Link className="button" to="/goals/new">Nova meta</Link>
      </header>

      <section className="card">
        {loading && <p className="muted">Carregando metas...</p>}
        {error && <p className="error">{error}</p>}
        {!loading && goals.length === 0 && <p className="muted">Nenhuma meta cadastrada.</p>}
        <div className="goal-list">
          {goals.map((goal) => (
            <Link key={goal.id} to={`/goals/${goal.id}`} className="goal-item">
              <div>
                <strong>{goal.name}</strong>
                <div className="muted">Alvo {formatCents(goal.targetAmountCents)} · até {goal.targetDate}</div>
              </div>
              <div className="goal-progress">
                <span>{formatCents(goal.savedSoFarCents)}</span>
                <span className="muted">{goal.targetAmountCents ? Math.round((goal.savedSoFarCents / goal.targetAmountCents) * 100) : 0}%</span>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
};

export const GoalCreatePage = () => {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [form, setForm] = useState({
    name: '',
    targetAmountCents: '',
    targetDate: '',
    startDate: '',
    monthlyRateBps: ''
  });
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setMessage('');
    try {
      const payload = {
        name: form.name,
        targetAmountCents: Number(form.targetAmountCents),
        targetDate: form.targetDate,
        startDate: form.startDate || null,
        monthlyRateBps: form.monthlyRateBps ? Number(form.monthlyRateBps) : 0
      };
      const response = await apiClient.post('/api/goals', payload);
      addToast('Meta criada.', 'success');
      navigate(`/goals/${response.id}`);
    } catch (err) {
      setMessage(err.message);
      addToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <header>
        <h1>Nova meta</h1>
        <p>Defina o valor alvo, data e taxa mensal (opcional).</p>
      </header>
      <section className="card">
        <form onSubmit={handleSubmit} className="form">
          <label>
            Nome
            <input
              value={form.name}
              onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
              required
            />
          </label>
          <label>
            Valor alvo (centavos)
            <input
              type="number"
              min="1"
              value={form.targetAmountCents}
              onChange={(event) => setForm((prev) => ({ ...prev, targetAmountCents: event.target.value }))}
              required
            />
          </label>
          <label>
            Data alvo
            <input
              type="month"
              value={form.targetDate}
              onChange={(event) => setForm((prev) => ({ ...prev, targetDate: event.target.value }))}
              required
            />
          </label>
          <label>
            Data inicial (opcional)
            <input
              type="date"
              value={form.startDate}
              onChange={(event) => setForm((prev) => ({ ...prev, startDate: event.target.value }))}
            />
          </label>
          <label>
            Taxa mensal (bps)
            <input
              type="number"
              min="0"
              value={form.monthlyRateBps}
              onChange={(event) => setForm((prev) => ({ ...prev, monthlyRateBps: event.target.value }))}
            />
          </label>
          <div className="form-actions">
            <button type="submit" disabled={loading}>Salvar meta</button>
            <Link className="button secondary" to="/goals">Cancelar</Link>
          </div>
        </form>
        {message && <p className="error">{message}</p>}
      </section>
    </div>
  );
};

export const GoalDetailPage = () => {
  const { addToast } = useToast();
  const { id } = useParams();
  const [goal, setGoal] = useState(null);
  const [projection, setProjection] = useState(null);
  const [contributions, setContributions] = useState([]);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [contributionForm, setContributionForm] = useState({
    contributedAt: '',
    amountCents: '',
    note: ''
  });

  const loadGoal = useCallback(async () => {
    setLoading(true);
    setMessage('');
    try {
      const data = await apiClient.get(`/api/goals/${id}`);
      setGoal(data);
      const projectionData = await apiClient.get(`/api/goals/${id}/projection`);
      setProjection(projectionData);
      const contribData = await apiClient.get(`/api/goals/${id}/contributions?page=0&size=20`);
      setContributions(contribData.contributions || []);
    } catch (err) {
      setMessage(err.message);
      addToast('Erro ao carregar meta.', 'error');
    } finally {
      setLoading(false);
    }
  }, [id, addToast]);

  useEffect(() => {
    loadGoal();
  }, [loadGoal]);

  const handleContribution = async (event) => {
    event.preventDefault();
    setMessage('');
    try {
      const payload = {
        contributedAt: contributionForm.contributedAt,
        amountCents: Number(contributionForm.amountCents),
        note: contributionForm.note
      };
      const response = await apiClient.post(`/api/goals/${id}/contributions`, payload);
      setContributions((prev) => [response.contribution, ...prev]);
      setProjection(response.projection);
      setGoal((prev) => ({
        ...prev,
        savedSoFarCents: (prev.savedSoFarCents || 0) + payload.amountCents
      }));
      setContributionForm({ contributedAt: '', amountCents: '', note: '' });
      addToast('Aporte registrado.', 'success');
    } catch (err) {
      setMessage(err.message);
      addToast(err.message, 'error');
    }
  };

  if (loading) {
    return (
      <div className="page">
        <p className="muted">Carregando meta...</p>
      </div>
    );
  }

  if (!goal) {
    return (
      <div className="page">
        <p className="error">Meta não encontrada.</p>
      </div>
    );
  }

  const percent = goal.targetAmountCents
    ? Math.round((goal.savedSoFarCents / goal.targetAmountCents) * 100)
    : 0;

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>{goal.name}</h1>
          <p className="muted">Meta até {goal.targetDate} · Status {goal.status}</p>
        </div>
        <Link className="button secondary" to="/goals">Voltar</Link>
      </header>

      {message && <p className="error">{message}</p>}

      <section className="card">
        <h2>Progresso</h2>
        <div className="goal-summary">
          <div>
            <span className="muted">Salvo</span>
            <strong>{formatCents(goal.savedSoFarCents)}</strong>
          </div>
          <div>
            <span className="muted">Alvo</span>
            <strong>{formatCents(goal.targetAmountCents)}</strong>
          </div>
          <div>
            <span className="muted">Percentual</span>
            <strong>{percent}%</strong>
          </div>
        </div>
        {projection && (
          <div className="projection">
            <p>
              Aporte necessário por mês: <strong>{formatCents(projection.neededMonthlyCents)}</strong>
            </p>
            <p className="muted">Conclusão estimada: {projection.estimatedCompletionMonth}</p>
          </div>
        )}
      </section>

      <section className="card">
        <h2>Aportes</h2>
        <form onSubmit={handleContribution} className="form">
          <label>
            Data
            <input
              type="date"
              value={contributionForm.contributedAt}
              onChange={(event) => setContributionForm((prev) => ({ ...prev, contributedAt: event.target.value }))}
              required
            />
          </label>
          <label>
            Valor (centavos)
            <input
              type="number"
              min="1"
              value={contributionForm.amountCents}
              onChange={(event) => setContributionForm((prev) => ({ ...prev, amountCents: event.target.value }))}
              required
            />
          </label>
          <label>
            Nota
            <input
              value={contributionForm.note}
              onChange={(event) => setContributionForm((prev) => ({ ...prev, note: event.target.value }))}
            />
          </label>
          <button type="submit">Registrar aporte</button>
        </form>
        <div className="rows">
          {contributions.map((contribution) => (
            <div key={contribution.id} className="row">
              <div>
                <strong>{contribution.contributedAt}</strong>
                {contribution.note && <div className="muted">{contribution.note}</div>}
              </div>
              <div className="muted">{formatCents(contribution.amountCents)}</div>
            </div>
          ))}
          {contributions.length === 0 && <p className="muted">Nenhum aporte registrado.</p>}
        </div>
      </section>
    </div>
  );
};
