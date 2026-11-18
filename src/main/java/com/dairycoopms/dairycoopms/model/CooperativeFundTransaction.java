package com.dairycoopms.dairycoopms.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CooperativeFundTransaction - Tracks all COFA balance changes.
 *
 * This allows the Accountant to see where money is coming from:
 * - MILK_SALE: Revenue from milk sales to companies
 * - MANUAL_TOPUP: Manual top-up by accountant
 * - LOAN_DISBURSEMENT: Money paid out for member loans
 * - LOAN_REPAYMENT: Money received from loan repayments
 */
@Entity
@Table(name = "cooperative_fund_transactions", schema = "dairycoopms", indexes = {
        @Index(name = "idx_cft_type", columnList = "transaction_type"),
        @Index(name = "idx_cft_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CooperativeFundTransaction {

    public enum TransactionType {
        MILK_SALE,          // Income from milk sales
        MANUAL_TOPUP,       // Manual top-up by accountant
        LOAN_DISBURSEMENT,  // Outgoing: loan paid to member
        LOAN_REPAYMENT,     // Incoming: loan repayment from member
        SALARY_PAYMENT,     // Outgoing: staff salary
        MEMBER_PAYMENT,     // Outgoing: member milk payment
        DEDUCTION           // Other deductions
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    /**
     * Positive for income, negative for expenses.
     */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /**
     * Balance after this transaction.
     */
    @Column(name = "balance_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal balanceAfter;

    /**
     * Reference number (e.g., INQ-2026-0001, LOAN-001, etc.)
     */
    @Column(name = "reference_no", length = 50)
    private String referenceNo;

    /**
     * Description of the transaction.
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * User who performed/approved the transaction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}