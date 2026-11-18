package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.CooperativeFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CooperativeFundRepository extends JpaRepository<CooperativeFund, Integer> {
    // Single row always at id=1
    // Usage: fundRepository.findById(1).orElseGet(CooperativeFund::new)

    /**
     * Get the COFA record (there's always only one with id=1).
     */
    Optional<CooperativeFund> findTopByOrderByIdDesc();
}