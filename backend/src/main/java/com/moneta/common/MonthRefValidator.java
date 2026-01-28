package com.moneta.common;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class MonthRefValidator {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private MonthRefValidator() {}

  public static void validate(String monthRef) {
    if (monthRef == null || monthRef.isBlank()) {
      throw new IllegalArgumentException("mês é obrigatório");
    }
    try {
      YearMonth.parse(monthRef, FORMATTER);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("mês inválido");
    }
  }
}
