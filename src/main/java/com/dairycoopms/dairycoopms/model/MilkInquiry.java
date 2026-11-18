package com.dairycoopms.dairycoopms.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * MilkInquiry - Represents a milk purchase request from a processing company.
 *
 * WORKFLOW:
 *   1. Company submits inquiry via public portal (no login required)
 *   2. Sales Officer reviews inquiry and checks milk availability
 *   3. Sales Officer approves (creates MilkSale/Invoice) or rejects (with reason)
 *   4. If approved: Invoice sent to company, payment tracked
 *   5. When paid: Sales marks payment received → Accountant approves → COFA updated
 *
 * STATUSES: PENDING → APPROVED/REJECTED → INVOICED → PAID
 */
@Entity
@Table(name = "milk_inquiries", schema = "dairycoopms", indexes = {
        @Index(name = "idx_inquiry_status", columnList = "status"),
        @Index(name = "idx_inquiry_submitted", columnList = "submitted_at"),
        @Index(name = "idx_inquiry_company", columnList = "company_name"),
        @Index(name = "idx_inquiry_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkInquiry {

    // ═══════════════════════════════════════════════════════════════════════════
    // EMBEDDED ENUMS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Status of a milk purchase inquiry.
     *
     * COMPLETE WORKFLOW:
     * 1. PENDING → Company submits inquiry via portal
     * 2. APPROVED → Sales approves inquiry, awaits company PO
     * 3. PO_CREATED → Company creates Purchase Order (draft)
     * 4. PO_SUBMITTED → Company submits PO to sales
     * 5. PO_APPROVED → Sales approves PO, milk ready for delivery
     * 6. DELIVERED → Milk physically delivered to company
     * 7. INVOICED → Invoice generated and sent to company
     * 8. PAID → Payment confirmed, transaction complete
     * OR REJECTED → Sales rejects at any stage (insufficient inventory, etc.)
     */
    public enum Status {
        PENDING,        // Just submitted, awaiting review by Sales Officer
        APPROVED,       // Approved by Sales, awaiting company PO
        PO_CREATED,     // Company created Purchase Order
        PO_SUBMITTED,   // Company submitted PO to sales
        PO_APPROVED,    // Sales approved PO, ready for delivery
        DELIVERED,      // Milk delivered to company, awaiting invoicing
        REJECTED,       // Rejected by Sales (insufficient inventory, etc.)
        INVOICED,       // Invoice generated and sent to company
        PAID            // Payment confirmed by Accountant, receipt ready
    }

    /**
     * Frequency options for milk supply requests.
     */
    public enum Frequency {
        ONE_TIME,     // Single order
        DAILY,        // Daily recurring supply
        WEEKLY,       // Weekly recurring supply
        MONTHLY       // Monthly recurring supply
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIMARY KEY
    // ═══════════════════════════════════════════════════════════════════════════

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    /**
     * Unique reference number for tracking (e.g., INQ-2026-0001).
     * Used by companies to check inquiry status on portal.
     */
    @Column(name = "reference_no", nullable = false, unique = true, length = 50)
    private String referenceNo;

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPANY / CONTACT INFORMATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Company or business name (e.g., "Parmalat Zambia Ltd").
     */
    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    /**
     * Contact person's full name.
     */
    @Column(name = "contact_person", nullable = false, length = 255)
    private String contactPerson;

    /**
     * Contact person's position/title (optional, e.g., "Procurement Manager").
     */
    @Column(name = "position", length = 100)
    private String position;

    /**
     * Email address for communication and invoice delivery.
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Phone number for contact.
     */
    @Column(name = "phone", nullable = false, length = 50)
    private String phone;

    // ═══════════════════════════════════════════════════════════════════════════
    // REQUEST DETAILS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Requested quantity in litres. Minimum: 100L.
     */
    @Column(name = "requested_litres", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedLitres;

    /**
     * Preferred delivery/pickup date. Must be at least 2 days in advance.
     */
    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    /**
     * Supply frequency preference.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    @Builder.Default
    private Frequency frequency = Frequency.ONE_TIME;

    /**
     * Additional message, requirements, or delivery instructions from the company.
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    // ═══════════════════════════════════════════════════════════════════════════
    // PRICING (set when approved)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Price per litre set by Sales Officer when approving (ZMW).
     */
    @Column(name = "price_per_litre", precision = 10, scale = 4)
    private BigDecimal pricePerLitre;

    /**
     * Total amount = requestedLitres × pricePerLitre (ZMW).
     */
    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    // ═══════════════════════════════════════════════════════════════════════════
    // STATUS & PROCESSING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Current status of the inquiry.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * Reason for rejection (required if status = REJECTED).
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /**
     * When the inquiry was submitted via the portal.
     */
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    /**
     * When the inquiry was processed (approved/rejected) by Sales Officer.
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Sales Officer who processed (approved/rejected) this inquiry.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    // ═══════════════════════════════════════════════════════════════════════════
    // DELIVERY TRACKING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * When the company confirmed delivery received.
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // ═══════════════════════════════════════════════════════════════════════════
    // PURCHASE ORDER TRACKING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Purchase Order number created by company (e.g., PO-PMT-2026-0001).
     */
    @Column(name = "purchase_order_no", length = 50)
    private String purchaseOrderNo;

    /**
     * When the company created the PO.
     */
    @Column(name = "po_created_at")
    private LocalDateTime poCreatedAt;

    /**
     * When the company submitted the PO to sales.
     */
    @Column(name = "po_submitted_at")
    private LocalDateTime poSubmittedAt;

    // ═══════════════════════════════════════════════════════════════════════════
    // INVOICE TRACKING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Invoice number (e.g., INV-2026-0001).
     */
    @Column(name = "invoice_no", unique = true, length = 50)
    private String invoiceNo;

    /**
     * When the invoice was generated.
     */
    @Column(name = "invoiced_at")
    private LocalDateTime invoicedAt;

    // Invoice signing & sending (mirrors receipt flow)
    @Column(name = "invoice_signed", nullable = false)
    @Builder.Default
    private boolean invoiceSigned = false;

    @Column(name = "invoice_signed_at")
    private LocalDateTime invoiceSignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_signed_by")
    private User invoiceSignedBy;

    @Column(name = "invoice_sent", nullable = false)
    @Builder.Default
    private boolean invoiceSent = false;

    @Column(name = "invoice_sent_at")
    private LocalDateTime invoiceSentAt;

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENT TRACKING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * When the accountant confirmed payment.
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Receipt number (e.g., RCP-2026-0001).
     */
    @Column(name = "receipt_no", unique = true, length = 50)
    private String receiptNo;

    @Column(name = "receipt_sent", nullable = false)
    @Builder.Default
    private boolean receiptSent = false;

    @Column(name = "receipt_sent_at")
    private LocalDateTime receiptSentAt;

    // ── Signature fields ─────────────────────────────────────────────────────
    @Column(name = "is_signed", nullable = false)
    @Builder.Default
    private boolean signed = false;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by")
    private User signedBy;

    /**
     * Accountant who confirmed the payment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_confirmed_by")
    private User paymentConfirmedBy;

    // ═══════════════════════════════════════════════════════════════════════════
    // AUDIT TIMESTAMPS
    // ═══════════════════════════════════════════════════════════════════════════

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (submittedAt == null) submittedAt = now;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if inquiry is still pending review.
     */
    public boolean isPending() {
        return status == Status.PENDING;
    }

    /**
     * Check if inquiry has been approved (awaiting delivery confirmation).
     */
    public boolean isApproved() {
        return status == Status.APPROVED;
    }

    /**
     * Check if delivery has been confirmed by company.
     */
    public boolean isDelivered() {
        return status == Status.DELIVERED || status == Status.INVOICED || status == Status.PAID;
    }

    /**
     * Check if invoice has been generated.
     */
    public boolean isInvoiced() {
        return status == Status.INVOICED || status == Status.PAID;
    }

    /**
     * Check if inquiry has been rejected.
     */
    public boolean isRejected() {
        return status == Status.REJECTED;
    }

    /**
     * Check if payment has been confirmed by accountant.
     */
    public boolean isPaid() {
        return status == Status.PAID;
    }

    // receiptSent, receiptSentAt, receiptNo getters/setters generated by @Data
    /**
     * Check if awaiting delivery (approved but not yet delivered).
     */
    public boolean isAwaitingDelivery() {
        return status == Status.APPROVED;
    }

    /**
     * Check if awaiting payment (invoiced but not yet paid).
     */
    public boolean isAwaitingPayment() {
        return status == Status.INVOICED;
    }

    /**
     * Calculate total amount based on litres and price.
     */
    public void calculateTotal() {
        if (requestedLitres != null && pricePerLitre != null) {
            this.totalAmount = requestedLitres.multiply(pricePerLitre)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PURCHASE ORDER WORKFLOW HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if a PO has been created for this inquiry.
     */
    public boolean hasPurchaseOrder() {
        return purchaseOrderNo != null &&
                (status == Status.PO_CREATED ||
                        status == Status.PO_SUBMITTED ||
                        status == Status.PO_APPROVED ||
                        status == Status.DELIVERED ||
                        status == Status.INVOICED ||
                        status == Status.PAID);
    }

    /**
     * Check if PO has been submitted by company.
     */
    public boolean isPOSubmitted() {
        return status == Status.PO_SUBMITTED ||
                status == Status.PO_APPROVED ||
                status == Status.DELIVERED ||
                status == Status.INVOICED ||
                status == Status.PAID;
    }

    /**
     * Check if PO has been approved by sales.
     */
    public boolean isPOApproved() {
        return status == Status.PO_APPROVED ||
                status == Status.DELIVERED ||
                status == Status.INVOICED ||
                status == Status.PAID;
    }

    /**
     * Check if awaiting PO from company (approved inquiry but no PO yet).
     */
    public boolean isAwaitingPO() {
        return status == Status.APPROVED;
    }

    /**
     * Check if awaiting PO submission (company created but hasn't submitted).
     */
    public boolean isAwaitingPOSubmission() {
        return status == Status.PO_CREATED;
    }

    /**
     * Check if awaiting PO approval (company submitted, sales hasn't approved).
     */
    public boolean isAwaitingPOApproval() {
        return status == Status.PO_SUBMITTED;
    }

    /**
     * Check if ready for delivery (PO approved, not yet delivered).
     */
    public boolean isReadyForDelivery() {
        return status == Status.PO_APPROVED;
    }

    /**
     * Check if delivered but not yet invoiced.
     */
    public boolean isAwaitingInvoicing() {
        return status == Status.DELIVERED;
    }

    /**
     * Get current stage description for display.
     */
    public String getStatusDescription() {
        if (status == null) return "Unknown";

        return switch (status) {
            case PENDING -> "Awaiting Sales Review";
            case APPROVED -> "Awaiting Purchase Order from Company";
            case PO_CREATED -> "Purchase Order Created (Draft)";
            case PO_SUBMITTED -> "Purchase Order Awaiting Sales Approval";
            case PO_APPROVED -> "Purchase Order Approved - Ready for Delivery";
            case DELIVERED -> "Delivered - Awaiting Invoice";
            case REJECTED -> "Rejected";
            case INVOICED -> "Invoice Sent - Awaiting Payment";
            case PAID -> "Payment Confirmed - Complete";
        };
    }

    /**
     * Get next action description for current user type.
     * @param userRole SALES, ACCOUNTS_OFFICER, or company portal user
     */
    public String getNextAction(String userRole) {
        if (status == null) return "Unknown";

        return switch (status) {
            case PENDING -> userRole.equals("SALES") ?
                    "Review and approve/reject inquiry" :
                    "Awaiting sales review";
            case APPROVED -> "Company needs to create Purchase Order";
            case PO_CREATED -> "Company needs to submit Purchase Order";
            case PO_SUBMITTED -> userRole.equals("SALES") ?
                    "Review and approve/reject Purchase Order" :
                    "Awaiting sales approval of PO";
            case PO_APPROVED -> userRole.equals("SALES") ?
                    "Arrange delivery and mark as delivered" :
                    "Awaiting delivery";
            case DELIVERED -> userRole.equals("SALES") ?
                    "Generate and send invoice" :
                    "Awaiting invoice";
            case INVOICED -> userRole.equals("ACCOUNTS_OFFICER") ?
                    "Confirm payment receipt" :
                    "Awaiting payment confirmation";
            case PAID -> "Transaction complete";
            case REJECTED -> "No further action required";
        };
    }
}