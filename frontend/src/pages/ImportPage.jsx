import React, { useCallback, useEffect, useState } from 'react';
import { apiClient } from '../api/client.js';
import { useToast } from '../components/Toast.jsx';

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

const ImportPage = () => {
  const { addToast } = useToast();
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

  const loadAccounts = useCallback(async () => {
    const data = await apiClient.get('/api/accounts');
    setAccounts(data);
    if (!accountId && data.length > 0) {
      setAccountId(String(data[0].id));
    }
  }, [accountId]);

  const loadBatches = useCallback(async () => {
    const data = await apiClient.get('/api/import/batches');
    setBatches(data);
  }, []);

  const loadRows = useCallback(async (batchId, status = statusFilter, currentPage = page) => {
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
  }, [statusFilter, page, size]);

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
      addToast('Importação concluída.', 'success');
    } catch (error) {
      setMessage(error.message);
      addToast(error.message, 'error');
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
      addToast('Commit concluído.', 'success');
      await loadBatches();
      await loadRows(selectedBatch.batchId, statusFilter, page);
    } catch (error) {
      setMessage(error.message);
      addToast(error.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAccounts();
    loadBatches();
  }, [loadAccounts, loadBatches]);

  useEffect(() => {
    if (selectedBatch) {
      loadRows(selectedBatch.batchId, statusFilter, page);
    }
  }, [selectedBatch, statusFilter, page, loadRows]);

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
            <input
              type="file"
              accept=".csv"
              onChange={(event) => {
                const { files } = event.target;
                if (files && files.length > 0) {
                  setFile(files[0]);
                }
              }}
            />
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

export default ImportPage;
