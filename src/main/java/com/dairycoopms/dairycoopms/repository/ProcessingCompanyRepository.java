package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.ProcessingCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for ProcessingCompany entity.
 */
@Repository
public interface ProcessingCompanyRepository extends JpaRepository<ProcessingCompany, UUID> {

    List<ProcessingCompany> findByStatus(String status);

    List<ProcessingCompany> findByStatusOrderByNameAsc(String status);

    boolean existsByNameIgnoreCase(String name);
}