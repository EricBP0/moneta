import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

const navItems = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/txns', label: 'Transações' },
  { to: '/accounts', label: 'Contas' },
  { to: '/institutions', label: 'Instituições' },
  { to: '/categories', label: 'Categorias' },
  { to: '/budgets', label: 'Orçamentos' },
  { to: '/rules', label: 'Regras' },
  { to: '/import', label: 'Import' },
  { to: '/goals', label: 'Metas' },
  { to: '/alerts', label: 'Alertas' }
];

const Layout = ({ children }) => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [isNavOpen, setIsNavOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="app-shell">
      <aside className={`sidebar ${isNavOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <h1>Moneta</h1>
          <p className="muted">MVP Financeiro</p>
        </div>
        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              onClick={() => setIsNavOpen(false)}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      {isNavOpen && <button type="button" className="sidebar-overlay" onClick={() => setIsNavOpen(false)} aria-label="Fechar menu" />}
      <div className="main">
        <header className="topbar">
          <div className="topbar-title">
            <button
              type="button"
              className="icon-button mobile-only"
              onClick={() => setIsNavOpen((prev) => !prev)}
              aria-label="Abrir menu"
            >
              ☰
            </button>
            <strong>Olá, {user?.name || 'Usuário'}</strong>
            <p className="muted">{user?.email}</p>
          </div>
          <button type="button" className="button secondary" onClick={handleLogout}>Logout</button>
        </header>
        <main className="content">{children}</main>
      </div>
    </div>
  );
};

export default Layout;
