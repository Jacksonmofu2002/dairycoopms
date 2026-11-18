package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.MilkInquiry.Status;
import com.dairycoopms.dairycoopms.model.MilkInquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MilkInquiryRepository extends JpaRepository<MilkInquiry, UUID> {

    // ═══════════════════════════════════════════════════════════════════════════
    // FIND BY REFERENCE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find inquiry by reference number (for status tracking).
     */
    Optional<MilkInquiry> findByReferenceNo(String referenceNo);

    /**
     * Find inquiry by reference number (case-insensitive).
     */
    Optional<MilkInquiry> findByReferenceNoIgnoreCase(String referenceNo);

    /**
     * Check if reference number exists.
     */
    boolean existsByReferenceNo(String referenceNo);

    // ═══════════════════════════════════════════════════════════════════════════
    // FIND BY STATUS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find all inquiries with a specific status.
     */
    List<MilkInquiry> findByStatusOrderBySubmittedAtDesc(MilkInquiry.Status status);

    /**
     * Find all inquiries with a specific status (sorted by submitted date).
     */
    List<MilkInquiry> findByStatusInOrderBySubmittedAtDesc(List<MilkInquiry.Status> statuses);

    /**
     * Count inquiries by status.
     */
    long countByStatus(MilkInquiry.Status status);

    // ═══════════════════════════════════════════════════════════════════════════
    // FIND BY COMPANY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find inquiries by company name (for returning customers).
     */
    List<MilkInquiry> findByCompanyNameIgnoreCaseOrderBySubmittedAtDesc(String companyName);

    /**
     * Find inquiries by company name with pagination (for returning customers).
     */
    Page<MilkInquiry> findByCompanyNameIgnoreCaseOrderBySubmittedAtDesc(String companyName, Pageable pageable);

    /**
     * Find inquiries by email.
     */
    List<MilkInquiry> findByEmailIgnoreCaseOrderBySubmittedAtDesc(String email);

    // ═══════════════════════════════════════════════════════════════════════════
    // RECENT INQUIRIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get recent inquiries (all statuses).
     */
    @Query("SELECT i FROM MilkInquiry i ORDER BY i.submittedAt DESC")
    List<MilkInquiry> findAllOrderBySubmittedAtDesc();

    /**
     * Get recent pending inquiries (limited).
     */
    @Query("SELECT i FROM MilkInquiry i WHERE i.status = :status ORDER BY i.submittedAt DESC")
    List<MilkInquiry> findRecentByStatus(@Param("status") MilkInquiry.Status status);

    /**
     * Find inquiries submitted after a certain date.
     */
    List<MilkInquiry> findBySubmittedAtAfterOrderBySubmittedAtDesc(LocalDateTime after);

    // ═══════════════════════════════════════════════════════════════════════════
    // REFERENCE NUMBER GENERATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the latest reference number for sequence generation.
     */
    @Query("SELECT i.referenceNo FROM MilkInquiry i WHERE i.referenceNo LIKE :prefix% ORDER BY i.referenceNo DESC")
    List<String> findLatestReferenceNoByPrefix(@Param("prefix") String prefix);

    /**
     * Count inquiries for a specific year (for reference number generation).
     */
    @Query("SELECT COUNT(i) FROM MilkInquiry i WHERE YEAR(i.submittedAt) = :year")
    long countByYear(@Param("year") int year);

    // ═══════════════════════════════════════════════════════════════════════════
    // DELIVERED ORDERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find delivered orders awaiting invoice generation.
     * Orders that have been delivered but not yet invoiced.
     */
    @Query("SELECT i FROM MilkInquiry i WHERE i.status = 'PO_APPROVED' AND i.deliveredAt IS NOT NULL AND i.invoiceNo IS NULL ORDER BY i.deliveredAt DESC")
    List<MilkInquiry> findDeliveredAwaitingInvoice();

}