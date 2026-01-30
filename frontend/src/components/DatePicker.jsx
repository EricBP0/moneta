import React from 'react';

const DatePicker = ({
  value,
  onChange,
  type = 'date',
  placeholder = 'Selecione a data',
  ...props
}) => (
  <input
    className="date-picker"
    type={type}
    value={value}
    onChange={onChange}
    placeholder={placeholder}
    lang="pt-BR"
    {...props}
  />
);

export default DatePicker;
