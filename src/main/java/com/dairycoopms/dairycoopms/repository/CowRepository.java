package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.Cow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Cow entity.
 *
 * existsByTagNumber() is the duplicate tag guard used in AdminService.recordCow().
 * findUnassignedCows() supports the admin UI "assign cow" dropdown.
 */
@Repository
public interface CowRepository extends JpaRepository<Cow, UUID> {

    // ── Duplicate tag guard (AdminService.recordCow) ─────────────────────────
    boolean existsByTagNumber(String tagNumber);

    Optional<Cow> findByTagNumber(String tagNumber);

    List<Cow> findByStatus(String status);

    /**
     * Returns cows that have NO active assignment.
     * Used in AdminService.getUnassignedCows() for the assign-cow screen.
     */
    @Query("""
            SELECT c FROM Cow c
            WHERE c.status = 'ACTIVE'
              AND NOT EXISTS (
                  SELECT 1 FROM CowAssignment ca
                  WHERE ca.cow = c
                    AND ca.active = true
              )
            """)
    List<Cow> findUnassignedCows();

    long countByStatus(String status);

    // For sequential tag generation: TAG-2026-0001
    long count();
}