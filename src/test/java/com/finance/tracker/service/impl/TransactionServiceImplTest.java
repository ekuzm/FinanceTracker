package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.Tag;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.request.TransactionUpdateRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.TransactionMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.TagRepository;
import com.finance.tracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TagRepository tagRepository;

    private TransactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransactionServiceImpl(
                transactionRepository,
                accountRepository,
                tagRepository,
                new TransactionMapper());
    }

    @Test
    void getTransactionByIdShouldReturnMappedTransaction() {
        Transaction transaction = transaction(5L, account(1L, "100.00"), List.of(tag(7L, "food")));
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));

        var response = service.getTransactionById(5L);

        assertEquals(5L, response.getId());
        assertEquals(1L, response.getAccountId());
        assertIterableEquals(List.of(7L), response.getTagIds());
    }

    @Test
    void getTransactionByIdShouldThrowWhenMissing() {
        when(transactionRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getTransactionById(5L));
    }

    @Test
    void getAllTransactionsShouldSupportBothRepositoryModes() {
        Transaction transaction = transaction(5L, account(1L, "100.00"), List.of());
        when(transactionRepository.findAllTransactions()).thenReturn(List.of(transaction));
        when(transactionRepository.findAllTransactionsWithEntityGraph()).thenReturn(List.of(transaction));

        var plain = service.getAllTransactions(false);
        var entityGraph = service.getAllTransactions(true);

        assertEquals(1, plain.size());
        assertEquals(1, entityGraph.size());
    }

    @Test
    void getTransactionsByDateRangeShouldReturnTransactionsForWholeDayWindow() {
        Transaction transaction = transaction(5L, account(1L, "100.00"), List.of());
        when(transactionRepository.findByOccurredAtBetween(
                        LocalDate.of(2026, 3, 1).atStartOfDay(),
                        LocalDate.of(2026, 3, 31).atTime(23, 59, 59, 999_999_999)))
                .thenReturn(List.of(transaction));

        var response = service.getTransactionsByDateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertEquals(1, response.size());
    }

    @Test
    void getTransactionsByDateRangeShouldRejectMissingDates() {
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        assertThrows(
                BadRequestException.class,
                () -> service.getTransactionsByDateRange(null, endDate));
    }

    @Test
    void getTransactionsByDateRangeShouldRejectMissingEndDate() {
        LocalDate startDate = LocalDate.of(2026, 3, 1);

        assertThrows(
                BadRequestException.class,
                () -> service.getTransactionsByDateRange(startDate, null));
    }

    @Test
    void getTransactionsByDateRangeShouldRejectReversedDates() {
        LocalDate startDate = LocalDate.of(2026, 3, 31);
        LocalDate endDate = LocalDate.of(2026, 3, 1);

        assertThrows(
                BadRequestException.class,
                () -> service.getTransactionsByDateRange(startDate, endDate));
    }

    @Test
    void createTransactionsBulkTxShouldCreateAllTransactionsAndUpdateBalance() {
        Account account = account(1L, "100.00");
        Tag groceriesTag = tag(1L, "groceries");
        TransactionRequest salary = request("Salary", TransactionType.INCOME, "50.00", 1L, List.of());
        TransactionRequest lunch = request("Lunch", TransactionType.EXPENSE, "20.00", 1L, List.of(1L));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(tagRepository.findAllById(List.of(1L))).thenReturn(List.of(groceriesTag));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicLong ids = new AtomicLong(10L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(ids.getAndIncrement());
            return transaction;
        });

        List<TransactionResponse> responses = service.createTransactionsBulkTx(List.of(salary, lunch));

        assertEquals(2, responses.size());
        assertAmount("130.00", account.getBalance());
        assertEquals(10L, responses.get(0).getId());
        assertEquals(11L, responses.get(1).getId());
        assertIterableEquals(List.of(1L), responses.get(1).getTagIds());
        verify(accountRepository, times(2)).save(account);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void createTransactionsBulkTxShouldRejectEmptyRequest() {
        List<TransactionRequest> requests = List.of();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.createTransactionsBulkTx(requests));

        assertTrue(exception.getMessage().contains("at least one item"));
        verify(accountRepository, never()).findById(any());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransactionShouldFailWhenSomeTagsAreMissing() {
        Account account = account(1L, "100.00");
        TransactionRequest request = request("Coffee", TransactionType.EXPENSE, "5.00", 1L, List.of(1L, 2L));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(tagRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(tag(1L, "coffee")));

        assertThrows(ResourceNotFoundException.class, () -> service.createTransaction(request));

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransactionShouldCreateIncomeWithoutTags() {
        Account account = account(1L, "100.00");
        TransactionRequest request = request("Salary", TransactionType.INCOME, "50.00", 1L, null);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(10L);
            return transaction;
        });

        var response = service.createTransaction(request);

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getAccountId());
        assertIterableEquals(List.of(), response.getTagIds());
        assertEquals(0, new BigDecimal("150.00").compareTo(account.getBalance()));
    }

    @Test
    void createTransactionShouldThrowWhenAccountIsMissing() {
        TransactionRequest request = request("Salary", TransactionType.INCOME, "50.00", 1L, null);
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createTransaction(request));
    }

    @Test
    void createTransactionsBulkNoTxShouldCreateAllTransactions() {
        Account account = account(1L, "100.00");
        TransactionRequest salary = request("Salary", TransactionType.INCOME, "50.00", 1L, null);
        TransactionRequest lunch = request("Lunch", TransactionType.EXPENSE, "20.00", 1L, List.of());

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicLong ids = new AtomicLong(20L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(ids.getAndIncrement());
            return transaction;
        });

        var response = service.createTransactionsBulkNoTx(List.of(salary, lunch));

        assertEquals(2, response.size());
        assertEquals(0, new BigDecimal("130.00").compareTo(account.getBalance()));
    }

    @Test
    void createTransactionsBulkTxShouldRejectNullRequestList() {
        assertThrows(BadRequestException.class, () -> service.createTransactionsBulkTx(null));
    }

    @Test
    void updateTransactionShouldKeepCurrentValuesWhenOptionalFieldsAreMissing() {
        Account account = account(1L, "100.00");
        Tag existingTag = tag(7L, "food");
        Transaction transaction = transaction(5L, account, List.of(existingTag));
        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setAmount(new BigDecimal("10.00"));

        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.updateTransaction(5L, request);

        assertAmount("120.00", account.getBalance());
        assertEquals("Lunch", response.getDescription());
        assertEquals(TransactionType.EXPENSE, response.getType());
        assertEquals(1L, response.getAccountId());
        assertAmount("10.00", response.getAmount());
        assertIterableEquals(List.of(7L), response.getTagIds());
        verify(tagRepository, never()).findAllById(any());
        verify(accountRepository, times(2)).save(account);
    }

    @Test
    void updateTransactionShouldChangeAllProvidedFields() {
        Account oldAccount = account(1L, "100.00");
        Account newAccount = account(2L, "70.00");
        Transaction transaction = transaction(5L, oldAccount, List.of(tag(1L, "food")));
        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setAccountId(2L);
        request.setTagIds(List.of(7L));
        request.setOccurredAt(LocalDateTime.of(2026, 3, 18, 9, 30));
        request.setAmount(new BigDecimal("40.00"));
        request.setDescription("Updated");
        request.setType(TransactionType.INCOME);

        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(newAccount));
        when(tagRepository.findAllById(List.of(7L))).thenReturn(List.of(tag(7L, "travel")));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateTransaction(5L, request);

        assertEquals(2L, response.getAccountId());
        assertEquals(TransactionType.INCOME, response.getType());
        assertEquals("Updated", response.getDescription());
        assertIterableEquals(List.of(7L), response.getTagIds());
        assertEquals(0, new BigDecimal("130.00").compareTo(oldAccount.getBalance()));
        assertEquals(0, new BigDecimal("110.00").compareTo(newAccount.getBalance()));
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void updateTransactionShouldThrowWhenTransactionIsMissing() {
        TransactionUpdateRequest request = new TransactionUpdateRequest();
        when(transactionRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateTransaction(5L, request));
    }

    @Test
    void deleteTransactionShouldRollbackBalanceAndDeleteEntity() {
        Account account = account(1L, "100.00");
        Transaction transaction = transaction(5L, account, List.of());
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteTransaction(5L);

        assertEquals(0, new BigDecimal("130.00").compareTo(account.getBalance()));
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void deleteTransactionShouldThrowWhenTransactionIsMissing() {
        when(transactionRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteTransaction(5L));
    }

    private static Account account(Long id, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    private static Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }

    private static TransactionRequest request(
            String description,
            TransactionType type,
            String amount,
            Long accountId,
            List<Long> tagIds) {
        TransactionRequest request = new TransactionRequest();
        request.setOccurredAt(LocalDateTime.of(2026, 3, 17, 10, 0));
        request.setAmount(new BigDecimal(amount));
        request.setDescription(description);
        request.setType(type);
        request.setAccountId(accountId);
        request.setTagIds(tagIds);
        return request;
    }

    private static Transaction transaction(Long id, Account account, List<Tag> tags) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setOccurredAt(LocalDateTime.of(2026, 3, 17, 12, 0));
        transaction.setAmount(new BigDecimal("30.00"));
        transaction.setDescription("Lunch");
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAccount(account);
        transaction.setTags(tags);
        return transaction;
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
