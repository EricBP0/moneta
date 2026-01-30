export const ALERT_LABELS = {
  BUDGET_80: 'Orçamento em 80%',
  BUDGET_100: 'Orçamento estourado',
  GOAL_BEHIND: 'Objetivo em atraso',
  GOAL_REACHED: 'Objetivo atingido'
};

export const TRANSACTION_STATUS_LABELS = {
  CLEARED: 'Confirmada',
  PENDING: 'Pendente'
};

export const TRANSACTION_DIRECTION_LABELS = {
  IN: 'Entrada',
  OUT: 'Saída'
};

export const GOAL_STATUS_LABELS = {
  ACTIVE: 'Ativa',
  PAUSED: 'Pausada',
  COMPLETED: 'Concluída',
  CANCELED: 'Cancelada'
};

export const ACCOUNT_TYPE_LABELS = {
  CHECKING: 'Corrente',
  SAVINGS: 'Poupança',
  SALARY: 'Salário',
  INVESTMENT: 'Investimentos',
  CREDIT: 'Crédito',
  CASH: 'Dinheiro'
};

export const CURRENCY_LABELS = {
  BRL: 'Real (BRL)',
  USD: 'Dólar (USD)',
  EUR: 'Euro (EUR)',
  GBP: 'Libra (GBP)'
};

export const getAlertLabel = (code) => ALERT_LABELS[code] || 'Desconhecido';

export const getTransactionStatusLabel = (status) => TRANSACTION_STATUS_LABELS[status] || 'Desconhecido';

export const getTransactionDirectionLabel = (direction) => TRANSACTION_DIRECTION_LABELS[direction] || 'Outro';

export const getAccountTypeLabel = (type) => ACCOUNT_TYPE_LABELS[type] || 'Outro';

export const getCurrencyLabel = (currency) => CURRENCY_LABELS[currency] || currency || 'Outro';

export const getGoalStatusLabel = (status) => GOAL_STATUS_LABELS[status] || 'Outro';

export const ACCOUNT_TYPE_OPTIONS = Object.entries(ACCOUNT_TYPE_LABELS).map(([value, label]) => ({
  value,
  label
}));

export const CURRENCY_OPTIONS = Object.entries(CURRENCY_LABELS).map(([value, label]) => ({
  value,
  label
}));

export const TRANSACTION_STATUS_OPTIONS = Object.entries(TRANSACTION_STATUS_LABELS).map(([value, label]) => ({
  value,
  label
}));

export const TRANSACTION_DIRECTION_OPTIONS = Object.entries(TRANSACTION_DIRECTION_LABELS).map(([value, label]) => ({
  value,
  label
}));
