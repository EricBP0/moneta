package com.moneta.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moneta.importer.CsvParserService.CsvParseResult;
import com.moneta.txn.TxnDirection;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CsvParserServiceTest {
  private final CsvParserService csvParserService = new CsvParserService();

  @Test
  void parsesMinimalCsv() throws Exception {
    String csv = "date,description,amount\n2024-01-05,Padaria,12.50";

    CsvParseResult result = csvParserService.parse(
      new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
    );

    assertThat(result.rows()).hasSize(1);
    var row = result.rows().get(0);
    assertThat(row.parsedDate()).hasToString("2024-01-05");
    assertThat(row.description()).isEqualTo("Padaria");
    assertThat(row.amountCents()).isEqualTo(1250L);
    assertThat(row.direction()).isEqualTo(TxnDirection.IN);
    assertThat(row.paymentType()).isNotNull();
    assertThat(row.status()).isEqualTo(ImportRowStatus.PARSED);
  }

  @Test
  void parsesNegativeAmountAsOut() throws Exception {
    String csv = "date,description,amount\n2024-01-05,Taxi,-42.10";

    CsvParseResult result = csvParserService.parse(
      new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
    );

    var row = result.rows().get(0);
    assertThat(row.amountCents()).isEqualTo(4210L);
    assertThat(row.direction()).isEqualTo(TxnDirection.OUT);
    assertThat(row.paymentType()).isNotNull();
  }

  @Test
  void rejectsZeroAmountAndInvalidDate() throws Exception {
    String csv = "date,description,amount\n2024-99-01,Teste,0";

    CsvParseResult result = csvParserService.parse(
      new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
    );

    var row = result.rows().get(0);
    assertThat(row.status()).isEqualTo(ImportRowStatus.ERROR);
    assertThat(row.errorMessage()).isNotBlank();
  }

  @Test
  void rejectsMissingRequiredColumns() {
    String csv = "description,amount\nPadaria,10";

    assertThatThrownBy(() -> csvParserService.parse(
      new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
    )).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("coluna obrigatória");
  }
}
