package com.dairycoopms.dairycoopms.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loans", schema = "dairycoopms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "loan_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "outstanding_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal remainingBalance;

    @Column(name = "term_months", nullable = false)
    private int repaymentMonths;

    @Column(name = "monthly_instalment", nullable = false, precision = 14, scale = 2)
    private BigDecimal monthlyInstalment;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "purpose", nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ── Credit scoring fields ──────────────────────────────────────────────
    @Column(name = "credit_score", columnDefinition = "TINYINT")
    private Integer creditScore;           // 1-10

    @Column(name = "score_decision", length = 20)
    private String scoreDecision;          // AUTO_APPROVED | AUTO_REJECTED | MANUAL

    @Column(name = "score_reason", columnDefinition = "TEXT")
    private String scoreReason;            // human-readable explanation

    @Column(name = "score_qualifying_days", columnDefinition = "TINYINT")
    private Integer scoreQualifyingDays;   // days with 50+ litres

    @Column(name = "score_total_litres", precision = 10, scale = 2)
    private java.math.BigDecimal scoreTotalLitres;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_date")
    private LocalDate approvedDate;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    // ── Disbursement tracking ──────────────────────────────────────────────
    @Column(name = "disbursement_status", length = 30)
    private String disbursementStatus;   // null | PENDING_DISBURSEMENT | DISBURSED

    @Column(name = "disbursed_at")
    private java.time.LocalDateTime disbursedAt;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "disbursed_by")
    private com.dairycoopms.dairycoopms.model.User disbursedBy;

    @Column(name = "disbursement_reference", length = 100)
    private String disbursementReference;

    @Column(name = "disbursement_notes", columnDefinition = "TEXT")
    private String disbursementNotes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}