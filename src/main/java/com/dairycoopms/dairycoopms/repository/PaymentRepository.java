package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payment entity.
 *
 * findByMemberIdAndYearAndMonth() is the idempotency guard in
 * AccountantService.processPayment() – if a payment already exists
 * for this member/period it is returned without creating a duplicate.
 *
 * The UNIQUE constraint on (member_id, period_year, period_month)
 * in the DB schema backs this up at the database level.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Idempotency check – returns existing payment for a member/period if present.
     * Used by AccountantService.processPayment().
     */
    @Query("""
            SELECT p FROM Payment p
            WHERE p.member.id = :memberId
              AND p.year  = :year
              AND p.month = :month
            """)
    Optional<Payment> findByMemberIdAndYearAndMonth(
            @Param("memberId") UUID memberId,
            @Param("year")     int year,
            @Param("month")    int month);

    List<Payment> findByMemberId(UUID memberId);

    /** All payments for a period – monthly payment report. */
    @Query("""
            SELECT p FROM Payment p
            WHERE p.year  = :year
              AND p.month = :month
            ORDER BY p.member.name ASC
            """)
    List<Payment> findByYearAndMonth(
            @Param("year")  int year,
            @Param("month") int month);

    List<Payment> findByStatus(String status);

    long countByStatus(String status);

    List<Payment> findByMemberIdAndStatus(UUID memberId, String status);
}