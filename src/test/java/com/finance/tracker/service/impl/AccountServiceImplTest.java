package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.finance.tracker.exception.ConflictException;
import com.finance.tracker.mapper.AccountMapper;
import com.finance.tracker.repository.AccountRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        AccountTransferRequest request = new AccountTransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal(amount));
        request.setOccurredAt(LocalDateTime.of(2026, 3, 17, 14, 0));
        request.setNote("Transfer note");
        return request;
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
