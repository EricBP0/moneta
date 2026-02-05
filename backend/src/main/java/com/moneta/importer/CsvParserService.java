package com.moneta.importer;

import com.moneta.card.PaymentType;
import com.moneta.txn.TxnDirection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

@Service
public class CsvParserService {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public CsvParseResult parse(InputStream inputStream) throws IOException {
    try (CSVParser parser = CSVFormat.DEFAULT.builder()
      .setHeader()
      .setSkipHeaderRecord(true)
      .setIgnoreEmptyLines(true)
      .setTrim(true)
      .build()
      .parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      Map<String, Integer> headerMap = parser.getHeaderMap();
      if (headerMap == null) {
        throw new IllegalArgumentException("CSV sem cabeçalho");
      }

      ensureRequiredHeader(headerMap, "date");
      ensureRequiredHeader(headerMap, "description");
      ensureRequiredHeader(headerMap, "amount");

      List<CsvParsedRow> rows = new ArrayList<>();
      for (CSVRecord record : parser) {
        int rowIndex = (int) record.getRecordNumber();
        String rawLine = String.join(",", record.toList());
        rows.add(parseRecord(rowIndex, rawLine, record, headerMap));
      }
      return new CsvParseResult(rows);
    }
  }

  private CsvParsedRow parseRecord(
    int rowIndex,
    String rawLine,
    CSVRecord record,
    Map<String, Integer> headerMap
  ) {
    String dateValue = getValue(record, headerMap, "date");
    String description = getValue(record, headerMap, "description");
    String amountValue = getValue(record, headerMap, "amount");
    String category = getOptionalValue(record, headerMap, "category").orElse(null);
    String subcategory = getOptionalValue(record, headerMap, "subcategory").orElse(null);
    
    // Check if payment_method column exists in CSV
    boolean hasPaymentMethodColumn = headerMap.keySet().stream()
      .anyMatch(header -> header.equalsIgnoreCase("payment_method"));
    
    String paymentMethodValue = getOptionalValue(record, headerMap, "payment_method").orElse("PIX");
    String accountName = getOptionalValue(record, headerMap, "account").orElse(null);
    String cardName = getOptionalValue(record, headerMap, "card").orElse(null);

    if (dateValue == null || dateValue.isBlank()) {
      return CsvParsedRow.error(rowIndex, rawLine, "data inválida");
    }
    if (amountValue == null || amountValue.isBlank()) {
      return CsvParsedRow.error(rowIndex, rawLine, "valor inválido");
    }

    LocalDate parsedDate;
    try {
      parsedDate = LocalDate.parse(dateValue.trim(), DATE_FORMATTER);
    } catch (DateTimeParseException ex) {
      return CsvParsedRow.error(rowIndex, rawLine, "data inválida");
    }

    BigDecimal amount;
    try {
      amount = new BigDecimal(amountValue.trim());
    } catch (NumberFormatException ex) {
      return CsvParsedRow.error(rowIndex, rawLine, "valor inválido");
    }

    if (amount.compareTo(BigDecimal.ZERO) == 0) {
      return CsvParsedRow.error(rowIndex, rawLine, "valor não pode ser zero");
    }

    TxnDirection direction = amount.signum() < 0 ? TxnDirection.OUT : TxnDirection.IN;
    long amountCents = amount.abs().movePointRight(2).longValue();

    // Parse payment method
    PaymentType paymentType;
    try {
      paymentType = PaymentType.valueOf(paymentMethodValue.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return CsvParsedRow.error(rowIndex, rawLine, "payment_method inválido: deve ser PIX ou CARD");
    }

    // Only validate account/card requirements if payment_method column is explicitly provided
    // For backward compatibility, CSVs without payment_method column don't require account/card columns
    if (hasPaymentMethodColumn) {
      // Validate PIX requires account
      if (paymentType == PaymentType.PIX && (accountName == null || accountName.isBlank())) {
        return CsvParsedRow.error(rowIndex, rawLine, "transação PIX requer coluna 'account'");
      }

      // Validate CARD requires card
      if (paymentType == PaymentType.CARD && (cardName == null || cardName.isBlank())) {
        return CsvParsedRow.error(rowIndex, rawLine, "transação CARD requer coluna 'card'");
      }
    }

    return CsvParsedRow.parsed(
      rowIndex,
      rawLine,
      parsedDate,
      description,
      amountCents,
      direction,
      paymentType,
      accountName,
      cardName,
      category,
      subcategory
    );
  }

  private void ensureRequiredHeader(Map<String, Integer> headerMap, String expected) {
    boolean present = headerMap.keySet().stream()
      .anyMatch(header -> header.equalsIgnoreCase(expected));
    if (!present) {
      throw new IllegalArgumentException("coluna obrigatória ausente: " + expected);
    }
  }

  private String getValue(CSVRecord record, Map<String, Integer> headerMap, String expected) {
    return headerMap.keySet().stream()
      .filter(header -> header.equalsIgnoreCase(expected))
      .findFirst()
      .map(record::get)
      .orElse(null);
  }

  private Optional<String> getOptionalValue(CSVRecord record, Map<String, Integer> headerMap, String expected) {
    return headerMap.keySet().stream()
      .filter(header -> header.equalsIgnoreCase(expected))
      .findFirst()
      .map(record::get)
      .map(value -> value.isBlank() ? null : value);
  }

  public record CsvParsedRow(
    int rowIndex,
    String rawLine,
    LocalDate parsedDate,
    String description,
    Long amountCents,
    TxnDirection direction,
    PaymentType paymentType,
    String accountName,
    String cardName,
    String categoryName,
    String subcategoryName,
    ImportRowStatus status,
    String errorMessage
  ) {
    public static CsvParsedRow parsed(
      int rowIndex,
      String rawLine,
      LocalDate parsedDate,
      String description,
      Long amountCents,
      TxnDirection direction,
      PaymentType paymentType,
      String accountName,
      String cardName,
      String categoryName,
      String subcategoryName
    ) {
      return new CsvParsedRow(
        rowIndex,
        rawLine,
        parsedDate,
        description,
        amountCents,
        direction,
        paymentType,
        accountName,
        cardName,
        categoryName,
        subcategoryName,
        ImportRowStatus.PARSED,
        null
      );
    }

    public static CsvParsedRow error(int rowIndex, String rawLine, String errorMessage) {
      return new CsvParsedRow(
        rowIndex,
        rawLine,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        ImportRowStatus.ERROR,
        errorMessage
      );
    }
  }

  public record CsvParseResult(List<CsvParsedRow> rows) {}
}
