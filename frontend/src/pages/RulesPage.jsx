import React, { useCallback, useEffect, useState } from 'react';
import { apiClient } from '../api/client.js';
import { useToast } from '../components/Toast.jsx';
import { monthToday } from '../utils/format.js';

const matchTypes = ['CONTAINS', 'STARTS_WITH', 'REGEX'];

const defaultForm = {
  name: '',
  priority: 0,
  matchType: 'CONTAINS',
  pattern: '',
  categoryId: '',
  accountId: '',
  isActive: true
};

const RulesPage = () => {
  const { addToast } = useToast();
  const [rules, setRules] = useState([]);
  const [categories, setCategories] = useState([]);
  const [accounts, setAccounts] = useState([]);
  const [form, setForm] = useState(defaultForm);
  const [editing, setEditing] = useState(null);
  const [applyMonth, setApplyMonth] = useState(monthToday());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [rulesData, categoriesData, accountsData] = await Promise.all([
        apiClient.get('/api/rules'),
        apiClient.get('/api/categories'),
        apiClient.get('/api/accounts')
      ]);
      setRules(rulesData || []);
      setCategories(categoriesData || []);
      setAccounts(accountsData || []);
    } catch (err) {
      setError(err.message);
      addToast('Erro ao carregar regras.', 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const submitForm = async (event) => {
    event.preventDefault();
    try {
      const payload = {
        name: form.name,
        priority: Number(form.priority),
        matchType: form.matchType,
        pattern: form.pattern,
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        subcategoryId: null,
        accountId: form.accountId ? Number(form.accountId) : null,
        isActive: form.isActive
      };
      if (editing) {
        await apiClient.patch(`/api/rules/${editing.id}`, payload);
        addToast('Regra atualizada.', 'success');
      } else {
        await apiClient.post('/api/rules', payload);
        addToast('Regra criada.', 'success');
      }
      setForm(defaultForm);
      setEditing(null);
      loadData();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const editRule = (rule) => {
    setEditing(rule);
    setForm({
      name: rule.name,
      priority: rule.priority,
      matchType: rule.matchType,
      pattern: rule.pattern,
      categoryId: rule.categoryId ? String(rule.categoryId) : '',
      accountId: rule.accountId ? String(rule.accountId) : '',
      isActive: rule.isActive
    });
  };

  const deleteRule = async (ruleId) => {
    if (!window.confirm('Deseja desativar esta regra?')) {
      return;
    }
    try {
      await apiClient.delete(`/api/rules/${ruleId}`);
      addToast('Regra desativada.', 'success');
      loadData();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const applyRules = async () => {
    try {
      const payload = {
        month: applyMonth,
        accountId: null,
        onlyUncategorized: true,
        dryRun: false,
        overrideManual: false
      };
      const response = await apiClient.post('/api/rules/apply', payload);
      addToast(`Avaliado ${response.evaluated}, atualizadas ${response.updated}.`, 'success');
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Regras</h1>
          <p className="muted">Automatize categorizações.</p>
        </div>
      </header>

      <section className="card">
        <div className="section-header">
          <h2>Aplicar regras</h2>
          <button type="button" className="button secondary" onClick={applyRules}>Aplicar</button>
        </div>
        <label className="inline-field">
          Mês
          <input type="month" value={applyMonth} onChange={(event) => setApplyMonth(event.target.value)} />
        </label>
      </section>

      <section className="card">
        <h2>{editing ? 'Editar regra' : 'Nova regra'}</h2>
        <form onSubmit={submitForm} className="form">
          <label>
            Nome
            <input value={form.name} onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))} required />
          </label>
          <label>
            Prioridade
            <input
              type="number"
              min="0"
              value={form.priority}
              onChange={(event) => setForm((prev) => ({ ...prev, priority: event.target.value }))}
              required
            />
          </label>
          <label>
            Match
            <select value={form.matchType} onChange={(event) => setForm((prev) => ({ ...prev, matchType: event.target.value }))}>
              {matchTypes.map((type) => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
          </label>
          <label>
            Padrão
            <input value={form.pattern} onChange={(event) => setForm((prev) => ({ ...prev, pattern: event.target.value }))} required />
          </label>
          <label>
            Categoria
            <select value={form.categoryId} onChange={(event) => setForm((prev) => ({ ...prev, categoryId: event.target.value }))}>
              <option value="">Sem categoria</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          </label>
          <label>
            Conta
            <select value={form.accountId} onChange={(event) => setForm((prev) => ({ ...prev, accountId: event.target.value }))}>
              <option value="">Todas</option>
              {accounts.map((account) => (
                <option key={account.id} value={account.id}>{account.name}</option>
              ))}
            </select>
          </label>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={form.isActive}
              onChange={(event) => setForm((prev) => ({ ...prev, isActive: event.target.checked }))}
            />
            Ativa
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
        <h2>Lista de regras</h2>
        {loading && <p className="muted">Carregando regras...</p>}
        {error && <p className="error">{error}</p>}
        {!loading && rules.length === 0 && <p className="muted">Nenhuma regra cadastrada.</p>}
        <div className="rows">
          {rules.map((rule) => (
            <div key={rule.id} className="row">
              <div>
                <strong>{rule.name}</strong>
                <div className="muted">{rule.matchType} · {rule.pattern}</div>
                <div className="muted">Prioridade {rule.priority}</div>
              </div>
              <div className="button-group">
                <span className={rule.isActive ? 'chip success' : 'chip muted'}>{rule.isActive ? 'Ativa' : 'Inativa'}</span>
                <button type="button" className="button secondary" onClick={() => editRule(rule)}>Editar</button>
                <button type="button" className="button danger" onClick={() => deleteRule(rule.id)}>Desativar</button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

export default RulesPage;
