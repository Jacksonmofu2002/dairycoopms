package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.DeductionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DeductionType entity.
 * Uses Integer PK (AUTO_INCREMENT, not UUID) – matches the DB schema.
 *
 * existsByNameIgnoreCase() is used in AdminService.configureDeduction()
 * to prevent duplicate deduction type names.
 *
 * findByActiveTrueOrderByNameAsc() is used in AccountantService.applyDeductions()
 * to get all currently active deduction types to apply each month.
 */
@Repository
public interface DeductionTypeRepository extends JpaRepository<DeductionType, Integer> {

    boolean existsByNameIgnoreCase(String name);

    Optional<DeductionType> findByNameIgnoreCase(String name);

    /** All active deduction types – used by applyDeductions(). */
    List<DeductionType> findByActiveTrueOrderByNameAsc();

    /** All deduction types including inactive – for admin management screen. */
    List<DeductionType> findAllByOrderByNameAsc();
}