package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.tracker.cache.CacheManager;
import com.finance.tracker.domain.Account;
import com.finance.tracker.domain.AccountType;
import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.TransactionType;
import com.finance.tracker.domain.User;
import com.finance.tracker.dto.request.AccountRequest;
import com.finance.tracker.dto.request.AccountTransferRequest;
import com.finance.tracker.dto.request.AccountUpdateRequest;
import com.finance.tracker.exception.BadRequestException;
import com.finance.tracker.exception.ConflictException;
import com.finance.tracker.exception.ResourceNotFoundException;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private CacheManager cacheManager;
    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        cacheManager = spy(new CacheManager());
        service = new AccountServiceImpl(
                accountRepository,
                userRepository,
                transactionRepository,
                new AccountMapper(),
                cacheManager);
    }

    @Test
    void getAccountByIdShouldReturnMappedAccount() {
        Account account = account(1L, user(1L), "100.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        var response = service.getAccountById(1L);

        assertEquals(1L, response.getId());
        assertEquals(1L, response.getUserId());
    }

    @Test
    void getAccountByIdShouldThrowWhenMissing() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getAccountById(1L));
    }

    @Test
    void getAllAccountsShouldMapAllAccounts() {
        when(accountRepository.findAll()).thenReturn(List.of(account(1L, user(1L), "100.00")));

        var response = service.getAllAccounts();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).getId());
    }

    @Test
    void createAccountShouldSaveAccountAndInvalidateCache() {
        User user = user(1L);
        AccountRequest request = new AccountRequest("Main Card", AccountType.CHECKING, new BigDecimal("150.00"), 1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(7L);
            return account;
        });

        service.createAccount(request);

        verify(accountRepository).save(any(Account.class));
        verify(cacheManager).invalidate(User.class, Account.class, com.finance.tracker.domain.Budget.class);
    }

    @Test
    void createAccountShouldThrowWhenUserIsMissing() {
        AccountRequest request = new AccountRequest("Main", AccountType.CHECKING, new BigDecimal("50.00"), 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createAccount(request));
    }

    @Test
    void updateAccountShouldPatchFieldsAndChangeOwnerWhenAllowed() {
        Account account = account(1L, user(1L), "100.00");
        User newOwner = user(2L);
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setName("Savings");
        request.setType(AccountType.SAVINGS);
        request.setBalance(new BigDecimal("150.00"));
        request.setUserId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newOwner));
        when(transactionRepository.existsByAccountId(1L)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateAccount(1L, request);

        assertEquals("Savings", response.getName());
        assertEquals(AccountType.SAVINGS, response.getType());
        assertEquals(0, new BigDecimal("150.00").compareTo(response.getBalance()));
        assertEquals(2L, response.getUserId());
        verify(cacheManager).invalidate(User.class, Account.class, com.finance.tracker.domain.Budget.class);
    }

    @Test
    void updateAccountShouldAssignOwnerWhenCurrentOwnerIsNull() {
        Account account = account(1L, null, "100.00");
        User newOwner = user(2L);
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setUserId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newOwner));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateAccount(1L, request);

        assertEquals(2L, response.getUserId());
        verify(transactionRepository, never()).existsByAccountId(1L);
    }

    @Test
    void updateAccountShouldKeepOwnerWhenSameUserIsRequested() {
        User owner = user(1L);
        Account account = account(1L, owner, "100.00");
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setUserId(1L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateAccount(1L, request);

        assertEquals(1L, response.getUserId());
        verify(transactionRepository, never()).existsByAccountId(1L);
    }

    @Test
    void updateAccountShouldRejectOwnerChangeWhenTransactionsExist() {
        User currentOwner = user(1L);
        User newOwner = user(2L);
        Account account = account(1L, currentOwner, "100.00");
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setUserId(2L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newOwner));
        when(transactionRepository.existsByAccountId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.updateAccount(1L, request));

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void updateAccountShouldThrowWhenAccountIsMissing() {
        AccountUpdateRequest request = new AccountUpdateRequest();
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateAccount(1L, request));
    }

    @Test
    void updateAccountShouldSaveScalarChangesWhenOwnerIsOmitted() {
        Account account = account(1L, user(1L), "100.00");
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setName("Cash");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateAccount(1L, request);

        assertEquals("Cash", response.getName());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void deleteAccountShouldDeleteExistingAccount() {
        Account account = account(1L, user(1L), "100.00");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        service.deleteAccount(1L);

        verify(accountRepository).delete(account);
    }

    @Test
    void deleteAccountShouldThrowWhenMissing() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteAccount(1L));
    }

    @Test
    void createTransferTxShouldMoveMoneyAndPersistMirrorTransactions() {
        User owner = user(1L);
        Account fromAccount = account(1L, owner, "200.00");
        Account toAccount = account(2L, owner, "50.00");
        AccountTransferRequest request = transferRequest("70.00");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createTransferTx(request, false);

        assertAmount("130.00", fromAccount.getBalance());
        assertAmount("120.00", toAccount.getBalance());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        assertEquals(TransactionType.EXPENSE, captor.getAllValues().get(0).getType());
        assertEquals(TransactionType.INCOME, captor.getAllValues().get(1).getType());
        assertEquals("Transfer note to account 2", captor.getAllValues().get(0).getDescription());
        assertEquals("Transfer note from account 1", captor.getAllValues().get(1).getDescription());
    }

    @Test
    void createTransferNoTxShouldUseDefaultNoteAndCurrentTime() {
        User owner = user(1L);
        Account fromAccount = account(1L, owner, "200.00");
        Account toAccount = account(2L, owner, "50.00");
        AccountTransferRequest request = transferRequest(1L, 2L, "70.00", null, "   ");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createTransferNoTx(request, false);

        assertAmount("130.00", fromAccount.getBalance());
        assertAmount("120.00", toAccount.getBalance());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        assertEquals("Transfer to account 2", captor.getAllValues().get(0).getDescription());
        assertEquals("Transfer from account 1", captor.getAllValues().get(1).getDescription());
        assertNotNull(captor.getAllValues().get(0).getOccurredAt());
    }

    @Test
    void createTransferTxShouldThrowWhenFundsAreInsufficient() {
        User owner = user(1L);
        Account fromAccount = account(1L, owner, "20.00");
        Account toAccount = account(2L, owner, "50.00");
        AccountTransferRequest request = transferRequest("70.00");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        assertThrows(ConflictException.class, () -> service.createTransferTx(request, false));

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransferTxShouldThrowWhenAmountIsNull() {
        AccountTransferRequest request = transferRequest(1L, 2L, null, LocalDateTime.now(), "note");

        assertThrows(BadRequestException.class, () -> service.createTransferTx(request, false));
    }

    @Test
    void createTransferTxShouldThrowWhenAmountIsZero() {
        AccountTransferRequest request =
                transferRequest(1L, 2L, "0.00", LocalDateTime.of(2026, 3, 17, 9, 0), "note");

        assertThrows(BadRequestException.class, () -> service.createTransferTx(request, false));
    }

    @Test
    void createTransferTxShouldThrowWhenAccountsAreTheSame() {
        User owner = user(1L);
        Account account = account(1L, owner, "200.00");
        AccountTransferRequest request =
                transferRequest(1L, 1L, "10.00", LocalDateTime.of(2026, 3, 17, 9, 0), "note");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThrows(BadRequestException.class, () -> service.createTransferTx(request, false));
    }

    @Test
    void createTransferTxShouldThrowWhenAccountOwnersDiffer() {
        Account fromAccount = account(1L, user(1L), "200.00");
        Account toAccount = account(2L, user(2L), "50.00");
        AccountTransferRequest request =
                transferRequest(1L, 2L, "10.00", LocalDateTime.of(2026, 3, 17, 9, 0), "note");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        assertThrows(ConflictException.class, () -> service.createTransferTx(request, false));
    }

    @Test
    void createTransferTxShouldThrowWhenSourceOwnerIsMissing() {
        Account fromAccount = account(1L, null, "200.00");
        Account toAccount = account(2L, user(2L), "50.00");
        AccountTransferRequest request =
                transferRequest(1L, 2L, "10.00", LocalDateTime.of(2026, 3, 17, 9, 0), "note");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        assertThrows(ConflictException.class, () -> service.createTransferTx(request, false));
    }

    @Test
    void createTransferTxShouldThrowWhenTargetOwnerIsMissing() {
        Account fromAccount = account(1L, user(1L), "200.00");
        Account toAccount = account(2L, null, "50.00");
        AccountTransferRequest request =
                transferRequest(1L, 2L, "10.00", LocalDateTime.of(2026, 3, 17, 9, 0), "note");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        assertThrows(ConflictException.class, () -> service.createTransferTx(request, false));
    }

    @Test
    void createTransferTxShouldThrowWhenNoteIsTooLong() {
        User owner = user(1L);
        Account fromAccount = account(1L, owner, "200.00");
        Account toAccount = account(2L, owner, "50.00");
        AccountTransferRequest request =
                transferRequest(1L, 2L, "10.00", LocalDateTime.of(2026, 3, 17, 9, 0), "x".repeat(256));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        assertThrows(BadRequestException.class, () -> service.createTransferTx(request, false));
    }

    @Test
    void createTransferTxShouldThrowWhenSourceAccountIsMissing() {
        AccountTransferRequest request =
                transferRequest(1L, 2L, "10.00", LocalDateTime.of(2026, 3, 17, 9, 0), "note");

        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createTransferTx(request, false));
    }

    @Test
    void createTransferTxShouldFailAfterDebitWhenRequested() {
        User owner = user(1L);
        Account fromAccount = account(1L, owner, "200.00");
        Account toAccount = account(2L, owner, "50.00");
        AccountTransferRequest request = transferRequest(1L, 2L, "70.00", null, null);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalStateException.class, () -> service.createTransferTx(request, true));

        assertAmount("130.00", fromAccount.getBalance());
        assertAmount("50.00", toAccount.getBalance());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static Account account(Long id, User user, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUser(user);
        account.setBalance(new BigDecimal(balance));
        account.setType(AccountType.CHECKING);
        account.setName("Account " + id);
        return account;
    }

    private static AccountTransferRequest transferRequest(String amount) {
        return transferRequest(1L, 2L, amount, LocalDateTime.of(2026, 3, 17, 14, 0), "Transfer note");
    }

    private static AccountTransferRequest transferRequest(
            Long fromAccountId, Long toAccountId, String amount, LocalDateTime occurredAt, String note) {
        AccountTransferRequest request = new AccountTransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(amount == null ? null : new BigDecimal(amount));
        request.setOccurredAt(occurredAt);
        request.setNote(note);
        return request;
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
