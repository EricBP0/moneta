import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { apiClient } from '../api/client.js';
import { formatCents, monthToday, parseMoneyToCents, toIsoDateTime } from '../utils/format.js';
import { useToast } from '../components/Toast.jsx';
import DatePicker from '../components/DatePicker.jsx';
import MoneyInput from '../components/MoneyInput.jsx';
import {
  getTransactionDirectionLabel,
  getTransactionStatusLabel,
  TRANSACTION_DIRECTION_OPTIONS,
  TRANSACTION_STATUS_OPTIONS
} from '../constants/labels.js';

const defaultTxnForm = {
  accountId: '',
  amount: '',
  direction: 'OUT',
  description: '',
  occurredAt: '',
  status: 'CLEARED',
  categoryId: ''
};

const defaultTransferForm = {
  fromAccountId: '',
  toAccountId: '',
  amount: '',
  occurredAt: '',
  description: ''
};

const TransactionsPage = () => {
  const { addToast } = useToast();
  const [filters, setFilters] = useState({
    month: monthToday(),
    accountId: '',
    categoryId: '',
    query: '',
    direction: '',
    status: ''
  });
  const [accounts, setAccounts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [txns, setTxns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(defaultTxnForm);
  const [transferOpen, setTransferOpen] = useState(false);
  const [transferForm, setTransferForm] = useState(defaultTransferForm);

  const filterQuery = useMemo(() => {
    const params = new URLSearchParams();
    if (filters.month) params.append('month', filters.month);
    if (filters.accountId) params.append('accountId', filters.accountId);
    if (filters.categoryId) params.append('categoryId', filters.categoryId);
    if (filters.query) params.append('q', filters.query);
    if (filters.direction) params.append('direction', filters.direction);
    if (filters.status) params.append('status', filters.status);
    return params.toString();
  }, [filters]);

  const loadSupporting = useCallback(async () => {
    const [accountsData, categoriesData] = await Promise.all([
      apiClient.get('/api/accounts'),
      apiClient.get('/api/categories')
    ]);
    setAccounts(accountsData);
    setCategories(categoriesData);
  }, []);

  const loadTxns = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get(`/api/txns?${filterQuery}`);
      setTxns(response || []);
    } catch (err) {
      setError(err.message);
      addToast('Erro ao carregar transações.', 'error');
    } finally {
      setLoading(false);
    }
  }, [filterQuery, addToast]);

  useEffect(() => {
    loadSupporting();
  }, [loadSupporting]);

  useEffect(() => {
    loadTxns();
  }, [loadTxns]);

  const openCreate = () => {
    setEditing(null);
    setForm(defaultTxnForm);
    setIsFormOpen(true);
  };

  const openEdit = (txn) => {
    setEditing(txn);
    setForm({
      accountId: String(txn.accountId || ''),
      amount: formatCents(txn.amountCents || 0).replace('R$', '').trim(),
      direction: txn.direction,
      description: txn.description || '',
      occurredAt: txn.occurredAt ? new Date(txn.occurredAt).toISOString().slice(0, 16) : '',
      status: txn.status || 'CLEARED',
      categoryId: txn.categoryId ? String(txn.categoryId) : ''
    });
    setIsFormOpen(true);
  };

  const submitTxn = async (event) => {
    event.preventDefault();
    try {
      const amountCents = parseMoneyToCents(form.amount);
      if (amountCents <= 0) {
        addToast('Informe um valor válido.', 'error');
        return;
      }
      const payload = {
        accountId: Number(form.accountId),
        amountCents,
        direction: form.direction,
        description: form.description,
        occurredAt: toIsoDateTime(form.occurredAt),
        status: form.status,
        categoryId: form.categoryId ? Number(form.categoryId) : null
      };
      if (editing) {
        await apiClient.patch(`/api/txns/${editing.id}`, payload);
        addToast('Transação atualizada.', 'success');
      } else {
        await apiClient.post('/api/txns', payload);
        addToast('Transação criada.', 'success');
      }
      setIsFormOpen(false);
      loadTxns();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const deleteTxn = async (txnId) => {
    if (!window.confirm('Deseja remover esta transação?')) {
      return;
    }
    try {
      await apiClient.delete(`/api/txns/${txnId}`);
      addToast('Transação removida.', 'success');
      loadTxns();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const submitTransfer = async (event) => {
    event.preventDefault();
    // Validate that source and destination accounts are different
    if (transferForm.fromAccountId && transferForm.toAccountId && 
        transferForm.fromAccountId === transferForm.toAccountId) {
      addToast('As contas de origem e destino devem ser diferentes.', 'error');
      return;
    }
    try {
      const amountCents = parseMoneyToCents(transferForm.amount);
      if (amountCents <= 0) {
        addToast('Informe um valor válido.', 'error');
        return;
      }
      const payload = {
        fromAccountId: Number(transferForm.fromAccountId),
        toAccountId: Number(transferForm.toAccountId),
        amountCents,
        occurredAt: toIsoDateTime(transferForm.occurredAt),
        description: transferForm.description
      };
      await apiClient.post('/api/txns/transfer', payload);
      addToast('Transferência registrada.', 'success');
      setTransferOpen(false);
      setTransferForm(defaultTransferForm);
      loadTxns();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const applyRules = async () => {
    try {
      const payload = {
        month: filters.month,
        accountId: filters.accountId ? Number(filters.accountId) : null,
        onlyUncategorized: true,
        dryRun: false,
        overrideManual: false
      };
      const response = await apiClient.post('/api/rules/apply', payload);
      addToast(`Regras aplicadas: ${response.updated} atualizadas.`, 'success');
      loadTxns();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Transações</h1>
          <p className="muted">Filtre e gerencie lançamentos.</p>
        </div>
        <div className="actions">
          <button type="button" className="button secondary" onClick={() => setTransferOpen(true)}>Transferência</button>
          <button type="button" onClick={openCreate}>Nova transação</button>
        </div>
      </header>

      <section className="card">
        <h2>Filtros</h2>
        <div className="filters-grid">
          <label>
            Mês
            <DatePicker
              type="month"
              value={filters.month}
              onChange={(event) => setFilters((prev) => ({ ...prev, month: event.target.value }))}
            />
          </label>
          <label>
            Conta
            <select value={filters.accountId} onChange={(event) => setFilters((prev) => ({ ...prev, accountId: event.target.value }))}>
              <option value="">Todas</option>
              {accounts.map((account) => (
                <option key={account.id} value={account.id}>{account.name}</option>
              ))}
            </select>
          </label>
          <label>
            Categoria
            <select value={filters.categoryId} onChange={(event) => setFilters((prev) => ({ ...prev, categoryId: event.target.value }))}>
              <option value="">Todas</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          </label>
          <label>
            Texto
            <input value={filters.query} onChange={(event) => setFilters((prev) => ({ ...prev, query: event.target.value }))} />
          </label>
          <label>
            Direção
            <select value={filters.direction} onChange={(event) => setFilters((prev) => ({ ...prev, direction: event.target.value }))}>
              <option value="">Todas</option>
              {TRANSACTION_DIRECTION_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
          <label>
            Status
            <select value={filters.status} onChange={(event) => setFilters((prev) => ({ ...prev, status: event.target.value }))}>
              <option value="">Todos</option>
              {TRANSACTION_STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
        </div>
        <div className="form-actions">
          <button type="button" className="button secondary" onClick={applyRules}>Aplicar regras</button>
        </div>
      </section>

      <section className="card">
        <h2>Lista</h2>
        {loading && <p className="muted">Carregando transações...</p>}
        {error && <p className="error">{error}</p>}
        {!loading && txns.length === 0 && <p className="muted">Nenhuma transação encontrada.</p>}
        {txns.length > 0 && (
          <>
            <div className="table-wrapper desktop-only">
              <table>
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Conta</th>
                    <th>Descrição</th>
                    <th>Categoria</th>
                    <th>Direção</th>
                    <th>Status</th>
                    <th>Valor</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {txns.map((txn) => (
                    <tr key={txn.id}>
                      <td>{new Date(txn.occurredAt).toLocaleDateString('pt-BR')}</td>
                      <td>{accounts.find((account) => account.id === txn.accountId)?.name || txn.accountId}</td>
                      <td>{txn.description || '—'}</td>
                      <td>{categories.find((category) => category.id === txn.categoryId)?.name || '—'}</td>
                      <td>{getTransactionDirectionLabel(txn.direction)}</td>
                      <td>{getTransactionStatusLabel(txn.status)}</td>
                      <td>{formatCents(txn.amountCents)}</td>
                      <td className="table-actions">
                        <button type="button" className="button secondary" onClick={() => openEdit(txn)}>Editar</button>
                        <button type="button" className="button danger" onClick={() => deleteTxn(txn.id)}>Excluir</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="mobile-only mobile-list">
              {txns.map((txn) => (
                <div key={txn.id} className="card mobile-card">
                  <div className="mobile-card-header">
                    <div>
                      <strong>{txn.description || 'Sem descrição'}</strong>
                      <div className="muted">{new Date(txn.occurredAt).toLocaleDateString('pt-BR')}</div>
                    </div>
                    <div className="mobile-card-amount">{formatCents(txn.amountCents)}</div>
                  </div>
                  <div className="mobile-card-body">
                    <div>
                      <span className="muted">Conta</span>
                      <div>{accounts.find((account) => account.id === txn.accountId)?.name || txn.accountId}</div>
                    </div>
                    <div>
                      <span className="muted">Categoria</span>
                      <div>{categories.find((category) => category.id === txn.categoryId)?.name || '—'}</div>
                    </div>
                    <div>
                      <span className="muted">Direção</span>
                      <div>{getTransactionDirectionLabel(txn.direction)}</div>
                    </div>
                    <div>
                      <span className="muted">Status</span>
                      <div>{getTransactionStatusLabel(txn.status)}</div>
                    </div>
                  </div>
                  <div className="mobile-card-actions">
                    <button type="button" className="button secondary" onClick={() => openEdit(txn)}>Editar</button>
                    <button type="button" className="button danger" onClick={() => deleteTxn(txn.id)}>Excluir</button>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </section>

      {isFormOpen && (
        <div className="modal-overlay" onClick={() => setIsFormOpen(false)}>
          <div className="modal" role="dialog" aria-modal="true" aria-labelledby="txn-modal-title" onClick={(e) => e.stopPropagation()}>
            <h2 id="txn-modal-title">{editing ? 'Editar transação' : 'Nova transação'}</h2>
            <form onSubmit={submitTxn} className="form">
              <label>
                Conta
                <select value={form.accountId} onChange={(event) => setForm((prev) => ({ ...prev, accountId: event.target.value }))} required>
                  <option value="">Selecione</option>
                  {accounts.map((account) => (
                    <option key={account.id} value={account.id}>{account.name}</option>
                  ))}
                </select>
              </label>
              <label>
                Valor
                <MoneyInput
                  value={form.amount}
                  onChange={(value) => setForm((prev) => ({ ...prev, amount: value }))}
                  required
                />
              </label>
              <label>
                Direção
                <select value={form.direction} onChange={(event) => setForm((prev) => ({ ...prev, direction: event.target.value }))}>
                  {TRANSACTION_DIRECTION_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </label>
              <label>
                Data/Hora
                <DatePicker
                  type="datetime-local"
                  value={form.occurredAt}
                  onChange={(event) => setForm((prev) => ({ ...prev, occurredAt: event.target.value }))}
                  required
                />
              </label>
              <label>
                Descrição
                <input value={form.description} onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))} />
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
                Status
                <select value={form.status} onChange={(event) => setForm((prev) => ({ ...prev, status: event.target.value }))}>
                  {TRANSACTION_STATUS_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </label>
              <div className="form-actions">
                <button type="submit">Salvar</button>
                <button type="button" className="button secondary" onClick={() => setIsFormOpen(false)}>Cancelar</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {transferOpen && (
        <div className="modal-overlay" onClick={() => setTransferOpen(false)}>
          <div className="modal" role="dialog" aria-modal="true" aria-labelledby="transfer-modal-title" onClick={(e) => e.stopPropagation()}>
            <h2 id="transfer-modal-title">Transferência</h2>
            <form onSubmit={submitTransfer} className="form">
              <label>
                Conta origem
                <select value={transferForm.fromAccountId} onChange={(event) => setTransferForm((prev) => ({ ...prev, fromAccountId: event.target.value }))} required>
                  <option value="">Selecione</option>
                  {accounts.map((account) => (
                    <option key={account.id} value={account.id}>{account.name}</option>
                  ))}
                </select>
              </label>
              <label>
                Conta destino
                <select value={transferForm.toAccountId} onChange={(event) => setTransferForm((prev) => ({ ...prev, toAccountId: event.target.value }))} required>
                  <option value="">Selecione</option>
                  {accounts.map((account) => (
                    <option key={account.id} value={account.id}>{account.name}</option>
                  ))}
                </select>
              </label>
              <label>
                Valor
                <MoneyInput
                  value={transferForm.amount}
                  onChange={(value) => setTransferForm((prev) => ({ ...prev, amount: value }))}
                  required
                />
              </label>
              <label>
                Data/Hora
                <DatePicker
                  type="datetime-local"
                  value={transferForm.occurredAt}
                  onChange={(event) => setTransferForm((prev) => ({ ...prev, occurredAt: event.target.value }))}
                  required
                />
              </label>
              <label>
                Descrição
                <input value={transferForm.description} onChange={(event) => setTransferForm((prev) => ({ ...prev, description: event.target.value }))} />
              </label>
              <div className="form-actions">
                <button type="submit">Confirmar</button>
                <button type="button" className="button secondary" onClick={() => setTransferOpen(false)}>Cancelar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default TransactionsPage;
