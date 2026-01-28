import React, { useCallback, useEffect, useState } from 'react';
import { apiClient } from '../api/client.js';
import { useToast } from '../components/Toast.jsx';

const defaultForm = { name: '', color: '' };

const CategoriesPage = () => {
  const { addToast } = useToast();
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(defaultForm);
  const [editing, setEditing] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadCategories = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await apiClient.get('/api/categories');
      setCategories(data || []);
    } catch (err) {
      setError(err.message);
      addToast('Erro ao carregar categorias.', 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  const submitForm = async (event) => {
    event.preventDefault();
    try {
      const payload = { name: form.name, color: form.color || null };
      if (editing) {
        await apiClient.patch(`/api/categories/${editing.id}`, payload);
        addToast('Categoria atualizada.', 'success');
      } else {
        await apiClient.post('/api/categories', payload);
        addToast('Categoria criada.', 'success');
      }
      setForm(defaultForm);
      setEditing(null);
      loadCategories();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const editCategory = (category) => {
    setEditing(category);
    setForm({ name: category.name, color: category.color || '' });
  };

  const deleteCategory = async (categoryId) => {
    if (!window.confirm('Deseja desativar esta categoria?')) {
      return;
    }
    try {
      await apiClient.delete(`/api/categories/${categoryId}`);
      addToast('Categoria desativada.', 'success');
      loadCategories();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Categorias</h1>
          <p className="muted">Organize gastos e receitas.</p>
        </div>
      </header>

      <section className="card">
        <h2>{editing ? 'Editar categoria' : 'Nova categoria'}</h2>
        <form onSubmit={submitForm} className="form">
          <label>
            Nome
            <input value={form.name} onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))} required />
          </label>
          <label>
            Cor
            <input value={form.color} onChange={(event) => setForm((prev) => ({ ...prev, color: event.target.value }))} placeholder="#2563eb" />
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
        {loading && <p className="muted">Carregando categorias...</p>}
        {error && <p className="error">{error}</p>}
        {!loading && categories.length === 0 && <p className="muted">Nenhuma categoria cadastrada.</p>}
        <div className="rows">
          {categories.map((category) => (
            <div key={category.id} className="row">
              <div>
                <strong>{category.name}</strong>
                <div className="muted">{category.color || 'Sem cor'}</div>
              </div>
              <div className="button-group">
                <button type="button" className="button secondary" onClick={() => editCategory(category)}>Editar</button>
                <button type="button" className="button danger" onClick={() => deleteCategory(category.id)}>Desativar</button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};

export default CategoriesPage;
