package com.moneta.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.moneta.account.Account;
import com.moneta.account.AccountRepository;
import com.moneta.auth.User;
import com.moneta.auth.UserRepository;
import com.moneta.category.Category;
import com.moneta.category.CategoryRepository;
import com.moneta.importer.ImportDtos.ImportCommitRequest;
import com.moneta.importer.ImportDtos.ImportCommitResponse;
import com.moneta.rule.RuleService;
import com.moneta.txn.Txn;
import com.moneta.txn.TxnCategorizationMode;
import com.moneta.txn.TxnDirection;
import com.moneta.txn.TxnRepository;
import com.moneta.txn.TxnStatus;
import com.moneta.txn.TxnType;
import com.moneta.support.PostgresContainerTest;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@Tag("integration")
@Transactional
class ImportServiceTest extends PostgresContainerTest {
  @Autowired
  private ImportService importService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private ImportRowRepository importRowRepository;

  @Autowired
  private ImportBatchRepository importBatchRepository;

  @Autowired
  private TxnRepository txnRepository;

  @MockBean
  private RuleService ruleService;

  @Test
  void createsBatchWithTotalsAndDedupe() {
    User user = createUser();
    Account account = createAccount(user);

    String csv = "date,description,amount,category\n" +
      "2024-01-10,Almoço,25.00,Food\n" +
      "2024-01-11,Erro,0,Food\n" +
      "2024-01-10,Almoço,25.00,Food";

    Category category = new Category();
    category.setUser(user);
    category.setName("Food");
    categoryRepository.save(category);

    MockMultipartFile file = new MockMultipartFile(
      "file",
      "import.csv",
      "text/csv",
      csv.getBytes(StandardCharsets.UTF_8)
    );

    var response = importService.uploadCsv(user.getId(), account.getId(), file);

    assertThat(response.totals().totalRows()).isEqualTo(3);
    assertThat(response.totals().errorRows()).isEqualTo(1);
    assertThat(response.totals().duplicateRows()).isEqualTo(1);
    assertThat(response.totals().readyRows()).isEqualTo(1);
  }

  @Test
  void commitsReadyRowsAndIsIdempotent() {
    User user = createUser();
    Account account = createAccount(user);

    String csv = "date,description,amount\n" +
      "2024-02-01,Salário,100.00";

    MockMultipartFile file = new MockMultipartFile(
      "file",
      "import.csv",
      "text/csv",
      csv.getBytes(StandardCharsets.UTF_8)
    );

    var batchResponse = importService.uploadCsv(user.getId(), account.getId(), file);

    ImportCommitResponse firstCommit = importService.commitBatch(
      user.getId(),
      batchResponse.batchId(),
      new ImportCommitRequest(true, true, true)
    );
    ImportCommitResponse secondCommit = importService.commitBatch(
      user.getId(),
      batchResponse.batchId(),
      new ImportCommitRequest(true, true, true)
    );

    assertThat(firstCommit.createdTxns()).isEqualTo(1);
    assertThat(secondCommit.createdTxns()).isZero();
    assertThat(txnRepository.findByUserIdAndAccountIdAndIsActiveTrue(user.getId(), account.getId()))
      .hasSize(1);
  }

  @Test
  void appliesRulesAfterCommit() {
    User user = createUser();
    Account account = createAccount(user);

    Category category = new Category();
    category.setUser(user);
    category.setName("Groceries");
    category = categoryRepository.save(category);
    final Long categoryId = category.getId();

    String csv = "date,description,amount\n" +
      "2024-03-01,Supermercado,150.00";

    MockMultipartFile file = new MockMultipartFile(
      "file",
      "import.csv",
      "text/csv",
      csv.getBytes(StandardCharsets.UTF_8)
    );

    var batchResponse = importService.uploadCsv(user.getId(), account.getId(), file);

    when(ruleService.applyRules(eq(user.getId()), any(List.class)))
      .thenAnswer(invocation -> {
        List<Txn> txns = invocation.getArgument(1);
        txns.forEach(txn -> txn.setCategoryId(categoryId));
        return txns;
      });

    importService.commitBatch(user.getId(), batchResponse.batchId(), new ImportCommitRequest(true, true, true));

    Txn saved = txnRepository.findByUserIdAndAccountIdAndIsActiveTrue(user.getId(), account.getId())
      .get(0);
    assertThat(saved.getCategorizationMode()).isEqualTo(TxnCategorizationMode.RULE);
    assertThat(saved.getCategoryId()).isEqualTo(categoryId);
  }

  @Test
  void detectsDuplicatesAgainstExistingTxns() {
    User user = createUser();
    Account account = createAccount(user);

    Txn existing = new Txn();
    existing.setUser(user);
    existing.setAccount(account);
    existing.setAmountCents(5000L);
    existing.setDirection(TxnDirection.OUT);
    existing.setDescription("Mercado");
    existing.setOccurredAt(OffsetDateTime.of(2024, 4, 10, 0, 0, 0, 0, ZoneOffset.UTC));
    existing.setMonthRef("2024-04");
    existing.setStatus(TxnStatus.POSTED);
    existing.setTxnType(TxnType.NORMAL);
    existing.setCategorizationMode(TxnCategorizationMode.MANUAL);
    txnRepository.save(existing);

    String csv = "date,description,amount\n" +
      "2024-04-10,Mercado,-50.00";

    MockMultipartFile file = new MockMultipartFile(
      "file",
      "import.csv",
      "text/csv",
      csv.getBytes(StandardCharsets.UTF_8)
    );

    var batchResponse = importService.uploadCsv(user.getId(), account.getId(), file);

    assertThat(batchResponse.totals().duplicateRows()).isEqualTo(1);
    assertThat(importRowRepository.findByBatchIdAndUserId(batchResponse.batchId(), user.getId()))
      .allMatch(row -> row.getStatus() == ImportRowStatus.DUPLICATE);
  }

  private User createUser() {
    User user = new User();
    user.setEmail("user" + System.nanoTime() + "@moneta.test");
    user.setName("Teste");
    user.setPasswordHash("hash");
    return userRepository.save(user);
  }

  private Account createAccount(User user) {
    Account account = new Account();
    account.setUser(user);
    account.setName("Carteira");
    account.setType("CHECKING");
    account.setCurrency("BRL");
    account.setInitialBalanceCents(0L);
    return accountRepository.save(account);
  }
}
