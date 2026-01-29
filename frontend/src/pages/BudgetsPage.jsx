import React, { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiClient } from '../api/client.js';
import { formatCents, formatPercent, monthToday } from '../utils/format.js';
import { useToast } from '../components/Toast.jsx';

const defaultForm = { monthRef: monthToday(), categoryId: '', limitCents: '' };

const BudgetsPage = () => {
  const { addToast } = useToast();
  const [month, setMonth] = useState(monthToday());
  const [budgets, setBudgets] = useState([]);
  const [budgetStatus, setBudgetStatus] = useState([]);
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(defaultForm);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [budgetsData, categoriesData, dashboardData] = await Promise.all([
        apiClient.get(`/api/budgets?month=${month}`),
        apiClient.get('/api/categories'),
        apiClient.get(`/api/dashboard/monthly?month=${month}`)
      ]);
      setBudgets(budgetsData || []);
      setCategories(categoriesData || []);
      setBudgetStatus(dashboardData?.budgetStatus || []);
    } catch (err) {
      setError(err.message);
      addToast('Erro ao carregar orçamentos.', 'error');
    } finally {
      setLoading(false);
    }
  }, [month, addToast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const submitForm = async (event) => {
    event.preventDefault();
    try {
      const payload = {
        monthRef: form.monthRef,
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        subcategoryId: null,
        limitCents: Number(form.limitCents)
      };
      await apiClient.post('/api/budgets', payload);
      addToast('Orçamento criado.', 'success');
      setForm((prev) => ({ ...prev, limitCents: '' }));
      loadData();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const deleteBudget = async (budgetId) => {
    if (!window.confirm('Deseja excluir este orçamento?')) {
      return;
    }
    try {
      await apiClient.delete(`/api/budgets/${budgetId}`);
      addToast('Orçamento removido.', 'success');
      loadData();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const findStatus = (budgetId) => budgetStatus.find((status) => status.budgetId === budgetId);

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Orçamentos</h1>
          <p className="muted">Controle limites e alertas.</p>
        </div>
        <label className="inline-field">
          Mês
          <input type="month" value={month} onChange={(event) => setMonth(event.target.value)} />
        </label>
      </header>

      <section className="card">
        <h2>Novo orçamento</h2>
        <form onSubmit={submitForm} className="form">
          <label>
            Mês
            <input type="month" value={form.monthRef} onChange={(event) => setForm((prev) => ({ ...prev, monthRef: event.target.value }))} required />
          </label>
          <label>
            Categoria
            <select value={form.categoryId} onChange={(event) => setForm((prev) => ({ ...prev, categoryId: event.target.value }))} required>
              <option value="">Selecione</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          </label>
          <label>
            Limite (centavos)
            <input
              type="number"
              min="1"
              value={form.limitCents}
              onChange={(event) => setForm((prev) => ({ ...prev, limitCents: event.target.value }))}
              required
            />
          </label>
          <div className="form-actions">
            <button type="submit">Salvar</button>
            <Link className="button secondary" to="/alerts">Alertas</Link>
          </div>
        </form>
      </section>

      <section className="card">
        <h2>Lista de orçamentos</h2>
        {loading && <p className="muted">Carregando orçamentos...</p>}
        {error && <p className="error">{error}</p>}
        {!loading && budgets.length === 0 && <p className="muted">Nenhum orçamento cadastrado.</p>}
        <div className="rows">
          {budgets.map((budget) => {
            const status = findStatus(budget.id);
            return (
              <div key={budget.id} className="row">
                <div>
                  <strong>{categories.find((category) => category.id === budget.categoryId)?.name || budget.categoryId}</strong>
                  <div className="muted">Limite {formatCents(budget.limitCents)}</div>
                  {status && (
                    <div className="muted">
                      Consumo {formatCents(status.consumptionCents)} · {formatPercent(status.percent)}
                    </div>
                  )}
                </div>
                <div className="button-group">
                  {status?.triggered80 && <span className="chip warning">80%</span>}
                  {status?.triggered100 && <span className="chip danger">100%</span>}
                  <button type="button" className="button danger" onClick={() => deleteBudget(budget.id)}>Excluir</button>
                </div>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
};

export default BudgetsPage;
