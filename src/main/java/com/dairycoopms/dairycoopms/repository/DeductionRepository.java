package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.Deduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Deduction entity (maps to payment_deductions table).
 *
 * findByMemberIdAndYearAndMonth() is critical for two operations:
 *   1. Idempotency check in AccountantService.applyDeductions()
 *      → if rows already exist, return them without re-calculating
 *   2. Pre-payment check in AccountantService.processPayment()
 *      → payment is blocked if this returns empty
 *
 * sumAmountByMemberIdAndYearAndMonth() computes totalDeductions
 * used in Payment.totalDeductions field.
 */
@Repository
public interface DeductionRepository extends JpaRepository<Deduction, UUID> {

    /**
     * All deduction rows for a member in a given month.
     * Used for idempotency check AND pre-payment validation.
     */
    @Query("""
            SELECT d FROM Deduction d
            WHERE d.member.id = :memberId
              AND d.year  = :year
              AND d.month = :month
            """)
    List<Deduction> findByMemberIdAndYearAndMonth(
            @Param("memberId") UUID memberId,
            @Param("year")     int year,
            @Param("month")    int month);

    /**
     * Total deduction amount for a member in a given month.
     * Used by AccountantService to populate Payment.totalDeductions.
     */
    @Query("""
            SELECT SUM(d.amount) FROM Deduction d
            WHERE d.member.id = :memberId
              AND d.year  = :year
              AND d.month = :month
            """)
    Optional<BigDecimal> sumAmountByMemberIdAndYearAndMonth(
            @Param("memberId") UUID memberId,
            @Param("year")     int year,
            @Param("month")    int month);

    List<Deduction> findByMemberId(UUID memberId);
}