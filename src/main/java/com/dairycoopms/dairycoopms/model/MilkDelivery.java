package com.dairycoopms.dairycoopms.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "milk_deliveries", schema = "dairycoopms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_milk_deliveries_member_date_time",
                        columnNames = {"member_id", "delivery_date", "collected_at"})  // Changed this line
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cow_id")
    private Cow cow;

    @Column(name = "quantity_litres", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityLitres;

    @Column(name = "price_per_litre_at_delivery", nullable = false, precision = 10, scale = 4)
    private BigDecimal pricePerLitreAtDelivery;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime deliveryDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_officer_id")
    private User recordedBy;

    @Column(name = "delivery_status", nullable = false, length = 20)
    @Builder.Default
    private String deliveryStatus = "RECORDED";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "confirmed_by_member", nullable = false)
    @Builder.Default
    private boolean confirmedByMember = false;

    @Column(name = "confirmed_at")
    private java.time.LocalDateTime confirmedAt;

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