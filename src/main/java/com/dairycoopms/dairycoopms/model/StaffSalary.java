package com.dairycoopms.dairycoopms.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "staff_salaries", schema = "dairycoopms")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StaffSalary {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "period_year", nullable = false)
    private int year;

    @Column(name = "period_month", nullable = false)
    private int month;

    @Column(name = "basic_salary", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(name = "deductions", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "net_salary", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal netSalary = BigDecimal.ZERO;

    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private String status = "UNPAID";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** False until the staff member opens their portal dashboard after this salary is paid */
    @Column(name = "salary_notification_read", nullable = false)
    @Builder.Default
    private boolean salaryNotificationRead = true;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}