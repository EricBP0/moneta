const DEFAULT_CURRENCY_LOCALE = 'pt-BR';
const DEFAULT_CURRENCY_CODE = 'BRL';

export const formatCents = (value, options = {}) => {
  if (value === null || value === undefined) {
    return '—';
  }
  const {
    locale = DEFAULT_CURRENCY_LOCALE,
    currency = DEFAULT_CURRENCY_CODE,
  } = options;

  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
  }).format(value / 100);
};

export const formatPercent = (value) => {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '—';
  }
  return `${Math.round(value)}%`;
};

export const toIsoDateTime = (value) => {
  if (!value) {
    return null;
  }
  const date = new Date(value);
  return date.toISOString();
};

export const monthToday = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
};
