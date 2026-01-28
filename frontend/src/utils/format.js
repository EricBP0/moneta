export const formatCents = (value) => {
  if (value === null || value === undefined) {
    return '—';
  }
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
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
