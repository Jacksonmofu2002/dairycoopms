package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

    /**
     * Find PO by PO number
     */
    Optional<PurchaseOrder> findByPoNumber(String poNumber);

    /**
     * Find PO by inquiry ID
     */
    Optional<PurchaseOrder> findByInquiryId(UUID inquiryId);

    /**
     * Find all POs by company name
     */
    List<PurchaseOrder> findByCompanyNameContainingIgnoreCaseOrderByCreatedAtDesc(String companyName);

    /**
     * Find all POs by status
     */
    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(PurchaseOrder.Status status);

    /**
     * Find all POs by company email (for company portal)
     */
    List<PurchaseOrder> findByEmailOrderByCreatedAtDesc(String email);

    /**
     * Count POs by status
     */
    long countByStatus(PurchaseOrder.Status status);

    /**
     * Find all submitted POs (for sales to review)
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.status = 'SUBMITTED' ORDER BY po.submittedAt DESC")
    List<PurchaseOrder> findAllSubmitted();

    /**
     * Find all approved POs
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.status = 'APPROVED' ORDER BY po.approvedAt DESC")
    List<PurchaseOrder> findAllApproved();

    /**
     * Find all rejected POs
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.status = 'REJECTED' ORDER BY po.rejectedAt DESC")
    List<PurchaseOrder> findAllRejected();

    /**
     * Find approved POs without invoice
     */
    @Query("SELECT po FROM PurchaseOrder po WHERE po.status = 'APPROVED' AND po.invoiceNo IS NULL ORDER BY po.approvedAt DESC")
    List<PurchaseOrder> findApprovedWithoutInvoice();

    /**
     * Check if inquiry already has PO
     */
    boolean existsByInquiryId(UUID inquiryId);

    /**
     * Get latest PO number for company (for generating next PO number)
     */
    @Query("SELECT po.poNumber FROM PurchaseOrder po WHERE po.companyName = :companyName ORDER BY po.createdAt DESC")
    List<String> findLatestPoNumberForCompany(@Param("companyName") String companyName);

    /**
     * Find all POs by company name ordered by PO date (for company portal)
     */
    List<PurchaseOrder> findByCompanyNameOrderByPoDateDesc(String companyName);

    /**
     * Find POs by company name and status ordered by PO date (for company portal filtering)
     */
    List<PurchaseOrder> findByCompanyNameAndStatusOrderByPoDateDesc(String companyName, PurchaseOrder.Status status);

    /**
     * Find POs by PO number prefix (for generating unique PO numbers)
     */
    List<PurchaseOrder> findByPoNumberStartingWithOrderByPoNumberDesc(String prefix);
}