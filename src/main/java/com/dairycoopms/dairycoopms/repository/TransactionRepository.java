package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Transaction entity (financial ledger).
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByTransactionType(String transactionType);

    List<Transaction> findByMemberId(UUID memberId);

    List<Transaction> findByMemberIdOrderByCreatedAtDesc(UUID memberId);

    List<Transaction> findByReferenceId(UUID referenceId);
}