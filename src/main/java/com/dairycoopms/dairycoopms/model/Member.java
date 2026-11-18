package com.dairycoopms.dairycoopms.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "members", schema = "dairycoopms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_members_member_no", columnNames = {"member_no"}),
                @UniqueConstraint(name = "uq_members_nrc",       columnNames = {"nrc"}),
                @UniqueConstraint(name = "uq_members_email",     columnNames = {"email"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "member_no", nullable = false, unique = true, length = 50)
    private String memberNumber;

    @Column(name = "full_name", nullable = false, length = 255)
    private String name;

    @Column(name = "nrc", nullable = false, unique = true, length = 100)
    private String nrc;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Column(name = "phone", nullable = false, length = 50)
    private String phoneNumber;

    @Column(name = "address", nullable = false, columnDefinition = "TEXT")
    private String address;

    // ── Bank account details (filled by member after loan approval) ────────
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_name", length = 255)
    private String accountName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // field named 'active' → Lombok generates isActive() correctly
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "notification_read", nullable = false)
    @Builder.Default
    private boolean notificationRead = true;

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