import React, { useEffect, useState } from 'react';
import { Link, Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom';
import { apiClient } from './api/client.js';

const statusTabs = [
  { label: 'Ready', value: 'READY' },
  { label: 'Duplicadas', value: 'DUPLICATE' },
  { label: 'Erros', value: 'ERROR' }
];

const formatTotals = (totals) => {
  if (!totals) {
    return '—';
  }
  return `${totals.readyRows} prontas / ${totals.duplicateRows} dup / ${totals.errorRows} erro`;
};

const formatCents = (value) => {
  if (value === null || value === undefined) {
    return '—';
  }
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(value / 100);
};

const ImportPage = () => {
  const [accounts, setAccounts] = useState([]);
  const [batches, setBatches] = useState([]);
  const [selectedBatch, setSelectedBatch] = useState(null);
  const [rows, setRows] = useState([]);
  const [rowTotals, setRowTotals] = useState(null);
  const [statusFilter, setStatusFilter] = useState(statusTabs[0].value);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [file, setFile] = useState(null);
  const [accountId, setAccountId] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const loadAccounts = async () => {
    const data = await apiClient.get('/api/accounts');
    setAccounts(data);
    if (!accountId && data.length > 0) {
      setAccountId(String(data[0].id));
    }
  };

  const loadBatches = async () => {
    const data = await apiClient.get('/api/import/batches');
    setBatches(data);
  };

  const loadRows = async (batchId, status = statusFilter, currentPage = page) => {
    if (!batchId) {
      return;
    }
    const query = new URLSearchParams({
      status,
      page: String(currentPage),
      size: String(size)
    });
    const data = await apiClient.get(`/api/import/batches/${batchId}/rows?${query}`);
    setRows(data.rows || []);
    setRowTotals(data.totals || null);
  };

  const selectBatch = async (batch) => {
    setSelectedBatch(batch);
    setPage(0);
    await loadRows(batch.batchId, statusFilter, 0);
  };

  const handleUpload = async (event) => {
    event.preventDefault();
    if (!file || !accountId) {
      setMessage('Selecione um arquivo e uma conta.');
      return;
    }
    setLoading(true);
    setMessage('');
    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('accountId', accountId);
      const response = await apiClient.post('/api/import/csv', formData, { isForm: true });
      await loadBatches();
      setSelectedBatch(response);
      setStatusFilter(statusTabs[0].value);
      await loadRows(response.batchId, statusTabs[0].value, 0);
      setMessage(`Upload concluído: batch ${response.batchId}`);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleCommit = async () => {
    if (!selectedBatch) {
      return;
    }
    setLoading(true);
    setMessage('');
    try {
      const response = await apiClient.post(`/api/import/batches/${selectedBatch.batchId}/commit`, {
        applyRulesAfterCommit: true,
        skipDuplicates: true,
        commitOnlyReady: true
      });
      setMessage(`Commit: ${response.createdTxns} transações criadas.`);
      await loadBatches();
      await loadRows(selectedBatch.batchId, statusFilter, page);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAccounts();
    loadBatches();
  }, []);

  useEffect(() => {
    if (selectedBatch) {
      loadRows(selectedBatch.batchId, statusFilter, page);
    }
  }, [statusFilter, page]);

  return (
    <div className="page">
      <header>
        <h1>Importação CSV</h1>
        <p>Envie um CSV, revise as linhas e finalize o commit.</p>
      </header>

      <section className="card">
        <h2>Upload</h2>
        <form onSubmit={handleUpload} className="form">
          <label>
            Conta
            <select value={accountId} onChange={(event) => setAccountId(event.target.value)}>
              {accounts.map((account) => (
                <option key={account.id} value={account.id}>
                  {account.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Arquivo CSV
            <input type="file" accept=".csv" onChange={(event) => setFile(event.target.files[0])} />
          </label>
          <button type="submit" disabled={loading}>Enviar</button>
        </form>
        {message && <p className="message">{message}</p>}
      </section>

      <section className="card">
        <h2>Batches</h2>
        <div className="batch-list">
          {batches.map((batch) => (
            <button
              key={batch.batchId}
              className={selectedBatch?.batchId === batch.batchId ? 'batch selected' : 'batch'}
              type="button"
              onClick={() => selectBatch(batch)}
            >
              <div>
                <strong>#{batch.batchId}</strong> · {batch.filename}
              </div>
              <div className="muted">
                {batch.status} · {formatTotals(batch.totals)}
              </div>
            </button>
          ))}
          {batches.length === 0 && <p className="muted">Nenhum batch importado ainda.</p>}
        </div>
      </section>

      {selectedBatch && (
        <section className="card">
          <div className="batch-header">
            <div>
              <h2>Batch #{selectedBatch.batchId}</h2>
              <p className="muted">{selectedBatch.filename}</p>
            </div>
            <button type="button" onClick={handleCommit} disabled={loading}>
              Commit
            </button>
          </div>
          <div className="tabs">
            {statusTabs.map((tab) => (
              <button
                key={tab.value}
                type="button"
                className={statusFilter === tab.value ? 'tab active' : 'tab'}
                onClick={() => {
                  setStatusFilter(tab.value);
                  setPage(0);
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>
          <p className="muted">Totais: {formatTotals(rowTotals)}</p>
          <div className="rows">
            {rows.map((row) => (
              <div key={row.id} className="row">
                <div>
                  <strong>{row.parsedDate}</strong> · {row.description}
                </div>
                <div className="muted">
                  {row.direction} · {row.amountCents} cents
                </div>
                {row.errorMessage && <div className="error">{row.errorMessage}</div>}
              </div>
            ))}
            {rows.length === 0 && <p className="muted">Nenhuma linha para este filtro.</p>}
          </div>
          <div className="pagination">
            <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Anterior
            </button>
            <span>Página {page + 1}</span>
            <button type="button" disabled={rows.length < size} onClick={() => setPage(page + 1)}>
              Próxima
            </button>
          </div>
        </section>
      )}
    </div>
  );
};

const GoalsPage = () => {
  const [goals, setGoals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadGoals = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await apiClient.get('/api/goals');
      setGoals(data || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadGoals();
  }, []);

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
                <span className="muted">{Math.round((goal.savedSoFarCents / goal.targetAmountCents) * 100)}%</span>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
};

const GoalCreatePage = () => {
  const navigate = useNavigate();
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
      navigate(`/goals/${response.id}`);
    } catch (err) {
      setMessage(err.message);
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

const GoalDetailPage = () => {
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

  const loadGoal = async () => {
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
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadGoal();
  }, [id]);

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
    } catch (err) {
      setMessage(err.message);
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

const AppLayout = ({ children }) => (
  <div>
    <nav className="top-nav">
      <Link to="/import">Importação</Link>
      <Link to="/goals">Metas</Link>
    </nav>
    {children}
  </div>
);

const App = () => (
  <AppLayout>
    <Routes>
      <Route path="/import" element={<ImportPage />} />
      <Route path="/goals" element={<GoalsPage />} />
      <Route path="/goals/new" element={<GoalCreatePage />} />
      <Route path="/goals/:id" element={<GoalDetailPage />} />
      <Route path="*" element={<Navigate to="/goals" replace />} />
    </Routes>
  </AppLayout>
);

export default App;
