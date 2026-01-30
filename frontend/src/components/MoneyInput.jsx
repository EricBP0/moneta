import React from 'react';
import { formatCentsToMoney, parseMoneyToCents } from '../utils/format.js';

const MoneyInput = ({ value, onChange, onBlur, ...props }) => {
  const handleBlur = () => {
    if (value === '') {
      return;
    }
    const cents = parseMoneyToCents(value);
    const formatted = formatCentsToMoney(cents);
    onChange(formatted);
    if (onBlur) {
      onBlur(cents);
    }
  };

  return (
    <input
      className="money-input"
      type="text"
      inputMode="decimal"
      value={value}
      onChange={(event) => onChange(event.target.value)}
      onBlur={handleBlur}
      placeholder="0,00"
      {...props}
    />
  );
};

export default MoneyInput;
