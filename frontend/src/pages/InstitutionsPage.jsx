import React, { useEffect, useState } from 'react';
import { apiClient } from '../api/client.js';
import { useToast } from '../components/Toast.jsx';

const defaultForm = { name: '', type: '' };

const InstitutionsPage = () => {
  const { addToast } = useToast();
  const [institutions, setInstitutions] = useState([]);
  const [form, setForm] = useState(defaultForm);
  const [editing, setEditing] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadInstitutions = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await apiClient.get('/api/institutions');
      setInstitutions(data || []);
    } catch (err) {
      setError(err.message);
      addToast('Erro ao carregar instituições.', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInstitutions();
  }, []);

  const submitForm = async (event) => {
    event.preventDefault();
    try {
      const payload = { name: form.name, type: form.type };
      if (editing) {
        await apiClient.patch(`/api/institutions/${editing.id}`, payload);
        addToast('Instituição atualizada.', 'success');
      } else {
        await apiClient.post('/api/institutions', payload);
        addToast('Instituição criada.', 'success');
      }
      setForm(defaultForm);
      setEditing(null);
      loadInstitutions();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const editInstitution = (institution) => {
    setEditing(institution);
    setForm({ name: institution.name, type: institution.type || '' });
  };

  const deleteInstitution = async (institutionId) => {
    if (!window.confirm('Deseja desativar esta instituição?')) {
      return;
    }
    try {
      await apiClient.delete(`/api/institutions/${institutionId}`);
      addToast('Instituição desativada.', 'success');
      loadInstitutions();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Instituições</h1>
          <p className="muted">Bancos e provedores conectados.</p>
        </div>
      </header>

      <section className="card">
        <h2>{editing ? 'Editar instituição' : 'Nova instituição'}</h2>
        <form onSubmit={submitForm} className="form">
          <label>
            Nome
            <input value={form.name} onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))} required />
          </label>
          <label>
            Tipo
            <input value={form.type} onChange={(event) => setForm((prev) => ({ ...prev, type: event.target.value }))} />
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
        <h2>Lista</h2>
        {loading && <p className="muted">Carregando instituições...</p>}
        {error && <p className="error">{error}</p>}
        {!loading && institutions.length === 0 && <p className="muted">Nenhuma instituição cadastrada.</p>}
        <div className="rows">
          {institutions.map((institution) => (
            <div key={institution.id} className="row">
              <div>
                <strong>{institution.name}</strong>
                <div className="muted">{institution.type || '—'}</div>
              </div>
              <div className="button-group">
                <button type="button" className="button secondary" onClick={() => editInstitution(institution)}>Editar</button>
                <button type="button" className="button danger" onClick={() => deleteInstitution(institution.id)}>Desativar</button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

export default InstitutionsPage;
