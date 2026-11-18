package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.CooperativeFundTransaction;
import com.dairycoopms.dairycoopms.model.CooperativeFundTransaction.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CooperativeFundTransactionRepository extends JpaRepository<CooperativeFundTransaction, UUID> {

    /**
     * Get recent transactions (latest first).
     */
    List<CooperativeFundTransaction> findTop20ByOrderByCreatedAtDesc();

    /**
     * Find transactions by type.
     */
    List<CooperativeFundTransaction> findByTransactionTypeOrderByCreatedAtDesc(TransactionType type);

    /**
     * Find transactions in date range.
     */
    List<CooperativeFundTransaction> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    /**
     * Sum income by type for a period.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CooperativeFundTransaction t " +
            "WHERE t.transactionType = :type AND t.createdAt BETWEEN :from AND :to")
    BigDecimal sumByTypeAndPeriod(@Param("type") TransactionType type,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    /**
     * Total milk sales income.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CooperativeFundTransaction t " +
            "WHERE t.transactionType = 'MILK_SALE'")
    BigDecimal sumMilkSalesIncome();

    /**
     * Total milk sales income for current month.
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CooperativeFundTransaction t " +
            "WHERE t.transactionType = 'MILK_SALE' AND MONTH(t.createdAt) = :month AND YEAR(t.createdAt) = :year")
    BigDecimal sumMilkSalesIncomeByMonth(@Param("month") int month, @Param("year") int year);
}