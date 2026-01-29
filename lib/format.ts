const DEFAULT_CURRENCY_LOCALE = 'pt-BR'
const DEFAULT_CURRENCY_CODE = 'BRL'

interface FormatOptions {
  locale?: string
  currency?: string
}

export const formatCents = (value: number | null | undefined, options: FormatOptions = {}): string => {
  if (value === null || value === undefined) {
    return '—'
  }
  const { locale = DEFAULT_CURRENCY_LOCALE, currency = DEFAULT_CURRENCY_CODE } = options

  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
  }).format(value / 100)
}

export const formatPercent = (value: number | null | undefined): string => {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '—'
  }
  return `${Math.round(value)}%`
}

export const toIsoDateTime = (value: string | null | undefined): string | null => {
  if (!value) {
    return null
  }
  const date = new Date(value)
  return date.toISOString()
}

export const monthToday = (): string => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

export const formatDate = (date: string | Date, options?: Intl.DateTimeFormatOptions): string => {
  const d = typeof date === 'string' ? new Date(date) : date
  return d.toLocaleDateString('pt-BR', options)
}
