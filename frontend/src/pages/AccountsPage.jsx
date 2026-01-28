import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/client.js';
import { formatCents } from '../utils/format.js';
import { useToast } from '../components/Toast.jsx';

const defaultForm = {
  name: '',
  type: 'CHECKING',
  currency: 'BRL',
  initialBalanceCents: '',
  institutionId: ''
};

const AccountsPage = () => {
  const { addToast } = useToast();
  const [accounts, setAccounts] = useState([]);
  const [institutions, setInstitutions] = useState([]);
  const [form, setForm] = useState(defaultForm);
  const [editing, setEditing] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [accountsData, institutionsData] = await Promise.all([
        apiClient.get('/api/accounts'),
        apiClient.get('/api/institutions')
      ]);
      setAccounts(accountsData);
      setInstitutions(institutionsData);
    } catch (err) {
      setError(err.message);
      addToast('Erro ao carregar contas.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const submitForm = async (event) => {
    event.preventDefault();
    try {
      const payload = {
        name: form.name,
        type: form.type,
        currency: form.currency,
        initialBalanceCents: Number(form.initialBalanceCents),
        institutionId: form.institutionId ? Number(form.institutionId) : null
      };
      if (editing) {
        await apiClient.patch(`/api/accounts/${editing.id}`, payload);
        addToast('Conta atualizada.', 'success');
      } else {
        await apiClient.post('/api/accounts', payload);
        addToast('Conta criada.', 'success');
      }
      setForm(defaultForm);
      setEditing(null);
      loadData();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const editAccount = (account) => {
    setEditing(account);
    setForm({
      name: account.name,
      type: account.type,
      currency: account.currency,
      initialBalanceCents: String(account.initialBalanceCents),
      institutionId: account.institutionId ? String(account.institutionId) : ''
    });
  };

  const deleteAccount = async (accountId) => {
    if (!window.confirm('Deseja desativar esta conta?')) {
      return;
    }
    try {
      await apiClient.delete(`/api/accounts/${accountId}`);
      addToast('Conta desativada.', 'success');
      loadData();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Contas</h1>
          <p className="muted">Gerencie contas e saldos.</p>
        </div>
      </header>

      <section className="card">
        <h2>{editing ? 'Editar conta' : 'Nova conta'}</h2>
        <form onSubmit={submitForm} className="form">
          <label>
            Nome
            <input value={form.name} onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))} required />
          </label>
          <label>
            Tipo
            <input value={form.type} onChange={(event) => setForm((prev) => ({ ...prev, type: event.target.value }))} required />
          </label>
          <label>
            Moeda
            <input value={form.currency} onChange={(event) => setForm((prev) => ({ ...prev, currency: event.target.value }))} required />
          </label>
          <label>
            Saldo inicial (centavos)
            <input
              type="number"
              min="0"
              value={form.initialBalanceCents}
              onChange={(event) => setForm((prev) => ({ ...prev, initialBalanceCents: event.target.value }))}
              required
            />
          </label>
          <label>
            Instituição
            <select value={form.institutionId} onChange={(event) => setForm((prev) => ({ ...prev, institutionId: event.target.value }))}>
              <option value="">Sem instituição</option>
              {institutions.map((institution) => (
                <option key={institution.id} value={institution.id}>{institution.name}</option>
              ))}
            </select>
          </label>
          <div className="form-actions">
            <button type="submit">Salvar</button>
            {editing && (
              <button type="button" className="button secondary" onClick={() => {
                setEditing(null);
                setForm(defaultForm);
              }}>Cancelar</button>
            )}
          </div>
        </form>
      </section>

      <section className="card">
        <h2>Lista de contas</h2>
        {loading && <p className="muted">Carregando contas...</p>}
        {error && <p className="error">{error}</p>}
        {!loading && accounts.length === 0 && <p className="muted">Nenhuma conta cadastrada.</p>}
        <div className="rows">
          {accounts.map((account) => (
            <div key={account.id} className="row">
              <div>
                <strong>{account.name}</strong>
                <div className="muted">{account.type} · {account.currency}</div>
                <div className="muted">Instituição: {institutions.find((inst) => inst.id === account.institutionId)?.name || '—'}</div>
              </div>
              <div className="row-actions">
                <div>
                  <span className="muted">Saldo</span>
                  <strong>{formatCents(account.balanceCents)}</strong>
                </div>
                <div className="button-group">
                  <button type="button" className="button secondary" onClick={() => editAccount(account)}>Editar</button>
                  <button type="button" className="button danger" onClick={() => deleteAccount(account.id)}>Desativar</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

export default AccountsPage;
