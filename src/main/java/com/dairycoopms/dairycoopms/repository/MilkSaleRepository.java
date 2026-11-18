package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.MilkSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MilkSale entity.
 *
 * sumRevenueByYearAndMonth() powers cooperative income tracking on the dashboard.
 * getRevenueByCompany() powers the income breakdown chart in ManagerService.
 */
@Repository
public interface MilkSaleRepository extends JpaRepository<MilkSale, UUID> {

    // ── Status queries ───────────────────────────────────────────────────────

    List<MilkSale> findByPaymentStatus(String paymentStatus);

    long countByPaymentStatus(String paymentStatus);

    Optional<MilkSale> findByReferenceNo(String referenceNo);

    // ── Company queries ──────────────────────────────────────────────────────

    List<MilkSale> findByProcessingCompanyId(UUID companyId);

    // ── Revenue aggregate queries ────────────────────────────────────────────

    /**
     * Total revenue from all milk sales in a given month.
     * Used by AccountantService.getMonthlyRevenue() and ManagerService.
     */
    @Query("""
            SELECT SUM(s.totalRevenue) FROM MilkSale s
            WHERE YEAR(s.saleDate)  = :year
              AND MONTH(s.saleDate) = :month
            """)
    Optional<BigDecimal> sumRevenueByYearAndMonth(
            @Param("year")  int year,
            @Param("month") int month);

    /**
     * Revenue grouped by processing company for a given month.
     * Returns [ProcessingCompany, totalRevenue] pairs.
     * Used by ManagerService.getRevenueByCompany().
     */
    @Query("""
            SELECT s.processingCompany, SUM(s.totalRevenue) AS revenue
            FROM MilkSale s
            WHERE YEAR(s.saleDate)  = :year
              AND MONTH(s.saleDate) = :month
            GROUP BY s.processingCompany
            ORDER BY revenue DESC
            """)
    List<Object[]> getRevenueByCompany(
            @Param("year")  int year,
            @Param("month") int month);

    /**
     * Total revenue from all sales (all time).
     */
    @Query("SELECT SUM(s.totalRevenue) FROM MilkSale s WHERE s.paymentStatus = 'RECEIVED'")
    Optional<BigDecimal> sumTotalReceivedRevenue();
}