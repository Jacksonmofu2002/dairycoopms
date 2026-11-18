package com.dairycoopms.dairycoopms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Purchase Order created by companies after inquiry approval
 * This represents the company's formal order to buy milk
 */
@Entity
@Table(name = "purchase_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * PO number format: PO-COMPANY-YYYY-NNNN
     * Example: PO-PMT-2026-0001 (Parmalat)
     */
    @Column(name = "po_number", unique = true, nullable = false, length = 50)
    private String poNumber;

    /**
     * Link back to original inquiry
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id")
    private MilkInquiry inquiry;

    // ── Company Information (copied from inquiry for reference) ──────────

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "contact_person", nullable = false)
    private String contactPerson;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone", nullable = false, length = 50)
    private String phone;

    // ── Order Details ────────────────────────────────────────────────────

    @Column(name = "po_date", nullable = false)
    private LocalDate poDate;

    @Column(name = "quantity_litres", nullable = false, precision = 14, scale = 2)
    private BigDecimal quantityLitres;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    // ── Delivery Information ─────────────────────────────────────────────

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    // ── Payment Terms ────────────────────────────────────────────────────

    @Column(name = "payment_terms", length = 100)
    @Builder.Default
    private String paymentTerms = "Net 30 days";

    // ── Status Tracking ──────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.DRAFT;

    public enum Status {
        DRAFT,          // Company is creating PO
        SUBMITTED,      // PO submitted to sales
        APPROVED,       // Sales approved PO
        REJECTED        // Sales rejected PO
    }

    // ── Approval Information ─────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    // ── Link to Order/Invoice ────────────────────────────────────────────

    /**
     * After PO is approved and invoice created, this links to the MilkSale
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private MilkSale order;

    @Column(name = "invoice_no", length = 50)
    private String invoiceNo;

    @Column(name = "invoiced_at")
    private LocalDateTime invoicedAt;

    // ── Timestamps ───────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Submitted timestamp ──────────────────────────────────────────────

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // ── Helper Methods ───────────────────────────────────────────────────

    /**
     * Calculate total from quantity and price
     */
    @PrePersist
    @PreUpdate
    private void calculateTotal() {
        if (quantityLitres != null && unitPrice != null) {
            totalAmount = quantityLitres.multiply(unitPrice);
        }
    }

    /**
     * Check if PO can be submitted
     */
    public boolean canSubmit() {
        return status == Status.DRAFT &&
                poNumber != null &&
                quantityLitres != null &&
                unitPrice != null &&
                deliveryDate != null &&
                deliveryAddress != null;
    }

    /**
     * Check if PO can be approved
     */
    public boolean canApprove() {
        return status == Status.SUBMITTED;
    }

    /**
     * Check if PO can be rejected
     */
    public boolean canReject() {
        return status == Status.SUBMITTED;
    }

    /**
     * Mark as submitted
     */
    public void submit() {
        if (!canSubmit()) {
            throw new IllegalStateException("PO cannot be submitted in current state");
        }
        this.status = Status.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * Approve PO
     */
    public void approve(User approvedByUser) {
        if (!canApprove()) {
            throw new IllegalStateException("PO cannot be approved in current state");
        }
        this.status = Status.APPROVED;
        this.approvedBy = approvedByUser;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * Reject PO
     */
    public void reject(String reason) {
        if (!canReject()) {
            throw new IllegalStateException("PO cannot be rejected in current state");
        }
        this.status = Status.REJECTED;
        this.rejectionReason = reason;
        this.rejectedAt = LocalDateTime.now();
    }

    /**
     * Link to created order/invoice
     */
    public void linkToOrder(MilkSale sale) {
        this.order = sale;
        this.invoiceNo = sale.getReferenceNo();
        this.invoicedAt = LocalDateTime.now();
    }
}