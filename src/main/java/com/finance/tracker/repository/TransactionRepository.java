package com.finance.tracker.repository;

import com.finance.tracker.domain.Transaction;

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

    @Query("SELECT t FROM Transaction t WHERE t.transfer IS NULL")
    List<Transaction> findAllTransactionsWithoutTransfers();

    @EntityGraph(attributePaths = { "account", "transfer", "tags" })
    @Query("SELECT t FROM Transaction t")
    List<Transaction> findAllTransactionsWithEntityGraph();

    @EntityGraph(attributePaths = { "account", "transfer", "tags" })
    @Query("SELECT t FROM Transaction t WHERE t.transfer IS NULL")
    List<Transaction> findAllTransactionsWithoutTransfersWithEntityGraph();

    List<Transaction> findByOccurredAtBetweenAndTransferIsNull(
            LocalDateTime startDateTime, LocalDateTime endDateTime);

    boolean existsByAccountId(Long accountId);
}
