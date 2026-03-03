package com.finance.tracker.repository;

import com.finance.tracker.domain.Transaction;
import com.finance.tracker.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByOccurredAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("SELECT t FROM Transaction t")
    List<Transaction> findAllTransactions();

    @Query("SELECT t FROM Transaction t WHERE t.transferId IS NULL")
    List<Transaction> findAllTransactionsWithoutTransfers();

    @EntityGraph(attributePaths = { "budget", "user", "account", "tags" })
    @Query("SELECT t FROM Transaction t")
    List<Transaction> findAllTransactionsWithEntityGraph();

    @EntityGraph(attributePaths = { "budget", "user", "account", "tags" })
    @Query("SELECT t FROM Transaction t WHERE t.transferId IS NULL")
    List<Transaction> findAllTransactionsWithoutTransfersWithEntityGraph();

    List<Transaction> findByOccurredAtBetweenAndTransferIdIsNull(
            LocalDateTime startDateTime, LocalDateTime endDateTime);

    boolean existsByBudgetId(Long budgetId);

    boolean existsByAccountId(Long accountId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.budget.id = :budgetId
              AND t.type = :type
              AND t.transferId IS NULL
              AND t.occurredAt BETWEEN :startDateTime AND :endDateTime
            """)
    BigDecimal sumAmountForBudgetAndTypeInPeriod(
            Long budgetId,
            TransactionType type,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime);

    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
            FROM Transaction t
            WHERE t.budget.id = :budgetId
              AND (t.occurredAt < :startDateTime OR t.occurredAt > :endDateTime)
            """)
    boolean existsOutsideBudgetPeriod(Long budgetId, LocalDateTime startDateTime, LocalDateTime endDateTime);
}
