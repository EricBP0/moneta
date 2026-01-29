import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

const PrivateRoute = ({ children }) => {
  const { user } = useAuth();
  const token = localStorage.getItem('accessToken');
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  if (!user) {
    return <div className="page"><p className="muted">Carregando sessão...</p></div>;
  }
  return children;
};

export default PrivateRoute;
