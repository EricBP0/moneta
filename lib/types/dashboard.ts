// Dashboard Widget Configuration Types

export interface WidgetConfig {
  widgetKey: string
  isEnabled: boolean
  displayOrder: number
  settingsJson?: string | null
}

export interface CardLimitSummary {
  cardId: number
  cardName: string
  limitTotal: number
  usedCents: number
  availableCents: number
  percentUsed: number
  cycleStart: string
  cycleEnd: string
}

export const WIDGET_KEYS = {
  SUMMARY: 'SUMMARY',
  BUDGETS: 'BUDGETS',
  GOALS: 'GOALS',
  ALERTS: 'ALERTS',
  CARD_LIMITS: 'CARD_LIMITS',
} as const

export type WidgetKey = typeof WIDGET_KEYS[keyof typeof WIDGET_KEYS]
