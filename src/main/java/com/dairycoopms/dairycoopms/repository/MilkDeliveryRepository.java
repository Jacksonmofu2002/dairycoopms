package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.MilkDelivery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MilkDelivery entity.
 *
 * All queries use deliveryDate (DATE), not collected_at (TIMESTAMP), because:
 *   a) deliveryDate drives the UNIQUE constraint (member_id, delivery_date)
 *   b) Period grouping (year/month) is done on the DATE column efficiently
 *
 * FIX – getTopProducers():
 *   JPQL does not support LIMIT as a keyword or as a named parameter.
 *   Spring Data JPA's Pageable is the correct way to limit results.
 *   Callers pass PageRequest.of(0, limit) as the Pageable argument.
 *   The @Query has only :year and :month – both present in the JPQL.
 */
@Repository
public interface MilkDeliveryRepository extends JpaRepository<MilkDelivery, UUID> {

    // ── Delivery count per day (max 2 allowed) ───────────────────────────────────

    @Query("""
            SELECT COUNT(d) FROM MilkDelivery d
            WHERE d.member.id  = :memberId
              AND d.deliveryDate = :date
            """)
    long countByMemberIdAndDeliveryDate(
            @Param("memberId") UUID memberId,
            @Param("date") LocalDate date);

    // ── Fetch by date / member ────────────────────────────────────────────────

    List<MilkDelivery> findByDeliveryDate(LocalDate deliveryDate);

    List<MilkDelivery> findByMemberId(UUID memberId);

    List<MilkDelivery> findByDeliveryDateBetween(LocalDate from, LocalDate to);

    // ── Earnings calculation ──────────────────────────────────────────────────

    @Query("""
            SELECT d FROM MilkDelivery d
            WHERE d.member.id          = :memberId
              AND YEAR(d.deliveryDate)  = :year
              AND MONTH(d.deliveryDate) = :month
            """)
    List<MilkDelivery> findByMemberIdAndYearAndMonth(
            @Param("memberId") UUID memberId,
            @Param("year") int year,
            @Param("month") int month);

    // ── Aggregate queries ─────────────────────────────────────────────────────

    @Query("""
            SELECT SUM(d.quantityLitres) FROM MilkDelivery d
            WHERE d.deliveryDate = :date
            """)
    Optional<BigDecimal> sumQuantityByDeliveryDate(@Param("date") LocalDate date);

    @Query("""
            SELECT SUM(d.quantityLitres) FROM MilkDelivery d
            WHERE d.member.id          = :memberId
              AND YEAR(d.deliveryDate)  = :year
              AND MONTH(d.deliveryDate) = :month
            """)
    Optional<BigDecimal> sumQuantityByMemberIdAndYearAndMonth(
            @Param("memberId") UUID memberId,
            @Param("year") int year,
            @Param("month") int month);

    // ── Manager dashboard / chart queries ─────────────────────────────────────

    @Query("""
            SELECT d.deliveryDate, SUM(d.quantityLitres)
            FROM MilkDelivery d
            WHERE d.deliveryDate BETWEEN :from AND :to
            GROUP BY d.deliveryDate
            ORDER BY d.deliveryDate ASC
            """)
    List<Object[]> getDailyTotals(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT MONTH(d.deliveryDate), SUM(d.quantityLitres)
            FROM MilkDelivery d
            WHERE YEAR(d.deliveryDate) = :year
            GROUP BY MONTH(d.deliveryDate)
            ORDER BY MONTH(d.deliveryDate) ASC
            """)
    List<Object[]> getMonthlyTotals(@Param("year") int year);

    /**
     * Top milk-producing members for a given month.
     * Returns [Member, totalLitres] pairs ordered by litres descending.
     * <p>
     * The 'limit' is applied via Pageable – NOT as a JPQL parameter.
     * JPQL has no LIMIT keyword; Spring Data applies the page size as LIMIT.
     * <p>
     * Caller usage (e.g. ManagerService):
     * milkDeliveryRepository.getTopProducers(year, month, PageRequest.of(0, limit))
     */
    @Query("""
            SELECT d.member, SUM(d.quantityLitres) AS totalLitres
            FROM MilkDelivery d
            WHERE YEAR(d.deliveryDate)  = :year
              AND MONTH(d.deliveryDate) = :month
            GROUP BY d.member
            ORDER BY totalLitres DESC
            """)
    List<Object[]> getTopProducers(
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable);

    @Query("SELECT d.deliveryDate, SUM(d.quantityLitres) FROM MilkDelivery d GROUP BY d.deliveryDate ORDER BY d.deliveryDate DESC")
    List<Object[]> getAllDailyTotals();
}