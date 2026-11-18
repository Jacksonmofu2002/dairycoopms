package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.MilkPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for MilkPrice entity.
 *
 * findByActiveTrue() is the single most important query in this repository.
 * It is called by:
 *   - AdminService.setMilkPrice()       → to deactivate the current active price
 *   - AdminService.getCurrentMilkPrice()→ to retrieve the active price
 *   - CollectionOfficerService.recordDelivery() → to snapshot price at delivery time
 *
 * Only ONE row should ever return from findByActiveTrue() at any time.
 * The service enforces this – it deactivates the old one before saving the new one.
 */
@Repository
public interface MilkPriceRepository extends JpaRepository<MilkPrice, UUID> {

    /**
     * Returns the single currently active milk price.
     * Called as Optional so the service can throw a descriptive error if absent.
     */
    Optional<MilkPrice> findByActiveTrue();

    /**
     * Full price history, most recent first.
     * Used by AdminService.getMilkPriceHistory() and management reports.
     */
    List<MilkPrice> findAllByOrderByEffectiveDateDesc();
}