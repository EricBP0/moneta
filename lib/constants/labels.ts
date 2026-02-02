/**
 * Centralized label mappers for consistent UI display
 */

// Alert types - map enums to friendly Brazilian Portuguese labels
export const ALERT_TYPE_LABELS: Record<string, string> = {
  'GOAL_BEHIND': 'Objetivo em atraso',
  'GOAL_REACHED': 'Objetivo alcançado',
  'GOAL_ACHIEVED': 'Objetivo alcançado',
  'BUDGET_EXCEEDED': 'Orçamento excedido',
  'BUDGET_80': 'Orçamento atingiu 80%',
  'BUDGET_100': 'Orçamento atingiu 100%',
  'BUDGET_80_PERCENT': 'Orçamento atingiu 80%',
  'BUDGET_100_PERCENT': 'Orçamento atingiu 100%',
  'LOW_BALANCE': 'Saldo baixo',
  'NEGATIVE_BALANCE': 'Saldo negativo',
  'UNUSUAL_SPENDING': 'Gasto incomum',
}

/**
 * Get friendly label for alert type
 * @param type - Alert type enum
 * @returns Friendly label or fallback
 */
export function getAlertTypeLabel(type: string): string {
  return ALERT_TYPE_LABELS[type] || 'Alerta'
}

// Transaction status labels
export const TRANSACTION_STATUS_LABELS: Record<string, string> = {
  'CLEARED': 'Compensada',
  'PENDING': 'Pendente',
  'CANCELED': 'Cancelada',
  'VOID': 'Cancelada',
}

/**
 * Get friendly label for transaction status
 * @param status - Transaction status enum
 * @returns Friendly label
 */
export function getTransactionStatusLabel(status: string): string {
  return TRANSACTION_STATUS_LABELS[status] || status
}

// Goal status labels
export const GOAL_STATUS_LABELS: Record<string, string> = {
  'ACTIVE': 'Ativa',
  'PAUSED': 'Pausada',
  'COMPLETED': 'Concluída',
  'ACHIEVED': 'Concluída',
  'CANCELED': 'Cancelada',
}

/**
 * Get friendly label for goal status
 * @param status - Goal status enum
 * @returns Friendly label
 */
export function getGoalStatusLabel(status: string): string {
  return GOAL_STATUS_LABELS[status] || status
}

// Account type labels
export const ACCOUNT_TYPE_LABELS: Record<string, string> = {
  'CHECKING': 'Conta corrente',
  'SAVINGS': 'Poupança',
  'SALARY': 'Salário',
  'INVESTMENT': 'Investimentos',
}

/**
 * Get friendly label for account type
 * @param type - Account type enum
 * @returns Friendly label
 */
export function getAccountTypeLabel(type: string): string {
  return ACCOUNT_TYPE_LABELS[type] || type
}

// Account type options for select
export const ACCOUNT_TYPE_OPTIONS = [
  { value: 'CHECKING', label: 'Conta corrente' },
  { value: 'SAVINGS', label: 'Poupança' },
  { value: 'SALARY', label: 'Salário' },
  { value: 'INVESTMENT', label: 'Investimentos' },
]

// Transaction status options for select
export const TRANSACTION_STATUS_OPTIONS = [
  { value: 'CLEARED', label: 'Compensada' },
  { value: 'PENDING', label: 'Pendente' },
]

// Currency options for select
export const CURRENCY_OPTIONS = [
  { value: 'BRL', label: 'Real (BRL)' },
  { value: 'USD', label: 'Dólar (USD)' },
  { value: 'EUR', label: 'Euro (EUR)' },
  { value: 'GBP', label: 'Libra (GBP)' },
  { value: 'JPY', label: 'Iene (JPY)' },
  { value: 'CAD', label: 'Dólar Canadense (CAD)' },
  { value: 'AUD', label: 'Dólar Australiano (AUD)' },
  { value: 'CHF', label: 'Franco Suíço (CHF)' },
]

/**
 * Get currency label from code
 * @param code - Currency code
 * @returns Currency label
 */
export function getCurrencyLabel(code: string): string {
  const option = CURRENCY_OPTIONS.find(opt => opt.value === code)
  return option?.label || code
}
