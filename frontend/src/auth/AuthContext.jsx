import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { apiClient, clearSession, storeSession } from '../api/client.js';

const AuthContext = createContext(null);

const storageUserKey = 'user';

const readStoredUser = () => {
  const raw = localStorage.getItem(storageUserKey);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch (error) {
    localStorage.removeItem(storageUserKey);
    return null;
  }
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(readStoredUser());
  const [loading, setLoading] = useState(false);

  const persistUser = useCallback((nextUser) => {
    if (nextUser) {
      localStorage.setItem(storageUserKey, JSON.stringify(nextUser));
    } else {
      localStorage.removeItem(storageUserKey);
    }
    setUser(nextUser);
  }, []);

  const login = useCallback(async ({ email, password }) => {
    setLoading(true);
    try {
      const response = await apiClient.post('/api/auth/login', { email, password }, { skipAuth: true, skipRefresh: true });
      storeSession(response);
      persistUser(response.user);
      return response.user;
    } finally {
      setLoading(false);
    }
  }, [persistUser]);

  const register = useCallback(async ({ name, email, password }) => {
    setLoading(true);
    try {
      const response = await apiClient.post('/api/auth/register', { name, email, password }, { skipAuth: true, skipRefresh: true });
      storeSession(response);
      persistUser(response.user);
      return response.user;
    } finally {
      setLoading(false);
    }
  }, [persistUser]);

  const logout = useCallback(() => {
    clearSession();
    persistUser(null);
  }, [persistUser]);

  const refreshProfile = useCallback(async () => {
    if (!localStorage.getItem('accessToken')) {
      return null;
    }
    const response = await apiClient.get('/api/me');
    persistUser(response);
    return response;
  }, [persistUser]);

  useEffect(() => {
    if (!user && localStorage.getItem('accessToken')) {
      refreshProfile().catch(() => {
        clearSession();
        persistUser(null);
      });
    }
  }, [refreshProfile, user, persistUser]);

  const value = useMemo(() => ({
    user,
    loading,
    login,
    register,
    logout,
    refreshProfile
  }), [user, loading, login, register, logout, refreshProfile]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
