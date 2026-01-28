import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext.jsx';
import PrivateRoute from './components/PrivateRoute.jsx';
import Layout from './components/Layout.jsx';
import { ToastProvider } from './components/Toast.jsx';
import { LoginPage, RegisterPage } from './pages/AuthPages.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import TransactionsPage from './pages/TransactionsPage.jsx';
import AccountsPage from './pages/AccountsPage.jsx';
import InstitutionsPage from './pages/InstitutionsPage.jsx';
import CategoriesPage from './pages/CategoriesPage.jsx';
import BudgetsPage from './pages/BudgetsPage.jsx';
import RulesPage from './pages/RulesPage.jsx';
import AlertsPage from './pages/AlertsPage.jsx';
import ImportPage from './pages/ImportPage.jsx';
import { GoalCreatePage, GoalDetailPage, GoalsPage } from './pages/GoalsPages.jsx';

const AppRoutes = () => (
  <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />
    <Route
      path="/*"
      element={(
        <PrivateRoute>
          <Layout>
            <Routes>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/txns" element={<TransactionsPage />} />
              <Route path="/accounts" element={<AccountsPage />} />
              <Route path="/institutions" element={<InstitutionsPage />} />
              <Route path="/categories" element={<CategoriesPage />} />
              <Route path="/budgets" element={<BudgetsPage />} />
              <Route path="/rules" element={<RulesPage />} />
              <Route path="/alerts" element={<AlertsPage />} />
              <Route path="/import" element={<ImportPage />} />
              <Route path="/goals" element={<GoalsPage />} />
              <Route path="/goals/new" element={<GoalCreatePage />} />
              <Route path="/goals/:id" element={<GoalDetailPage />} />
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </Layout>
        </PrivateRoute>
      )}
    />
    <Route path="*" element={<Navigate to="/login" replace />} />
  </Routes>
);

const App = () => (
  <AuthProvider>
    <ToastProvider>
      <AppRoutes />
    </ToastProvider>
  </AuthProvider>
);

export default App;
