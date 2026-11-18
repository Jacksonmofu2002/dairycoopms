package com.dairycoopms.dairycoopms.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "milk_prices", schema = "dairycoopms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilkPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "price_per_litre", nullable = false, precision = 10, scale = 4)
    private BigDecimal pricePerLitre;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    // field named 'active' → Lombok generates isActive() correctly
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_by")
    private User setBy;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}