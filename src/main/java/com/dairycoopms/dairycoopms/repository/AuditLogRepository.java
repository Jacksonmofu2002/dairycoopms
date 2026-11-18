package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AuditLog entity.
 *
 * Used by every service via logAction() to write the audit trail.
 * All reads are for admin audit screens and reports.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    List<AuditLog> findByChangedByIdOrderByChangedAtDesc(UUID userId);

    List<AuditLog> findByEntityTypeOrderByChangedAtDesc(String entityType);

    List<AuditLog> findByChangedAtBetweenOrderByChangedAtDesc(
            LocalDateTime from, LocalDateTime to);

    List<AuditLog> findTop50ByOrderByChangedAtDesc();
}