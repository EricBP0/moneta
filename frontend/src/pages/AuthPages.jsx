import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { useToast } from '../components/Toast.jsx';

export const LoginPage = () => {
  const navigate = useNavigate();
  const { login, loading } = useAuth();
  const { addToast } = useToast();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    try {
      await login(form);
      addToast('Login realizado com sucesso.', 'success');
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="auth-layout">
      <div className="auth-card">
        <h1>Entrar</h1>
        <p className="muted">Acesse sua conta para continuar.</p>
        <form onSubmit={handleSubmit} className="form">
          <label>
            Email
            <input
              type="email"
              value={form.email}
              onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
              required
            />
          </label>
          <label>
            Senha
            <input
              type="password"
              value={form.password}
              onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
              required
            />
          </label>
          <button type="submit" disabled={loading}>Entrar</button>
        </form>
        {error && <p className="error">{error}</p>}
        <p className="muted">
          Não tem conta? <Link to="/register">Criar conta</Link>
        </p>
      </div>
    </div>
  );
};

export const RegisterPage = () => {
  const navigate = useNavigate();
  const { register, loading } = useAuth();
  const { addToast } = useToast();
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [error, setError] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    try {
      await register(form);
      addToast('Conta criada com sucesso.', 'success');
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="auth-layout">
      <div className="auth-card">
        <h1>Criar conta</h1>
        <p className="muted">Preencha seus dados para começar.</p>
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
            Email
            <input
              type="email"
              value={form.email}
              onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
              required
            />
          </label>
          <label>
            Senha
            <input
              type="password"
              value={form.password}
              onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
              required
            />
          </label>
          <button type="submit" disabled={loading}>Registrar</button>
        </form>
        {error && <p className="error">{error}</p>}
        <p className="muted">
          Já possui conta? <Link to="/login">Entrar</Link>
        </p>
      </div>
    </div>
  );
};
