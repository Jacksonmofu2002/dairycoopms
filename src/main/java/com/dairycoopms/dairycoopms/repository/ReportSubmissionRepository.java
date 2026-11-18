package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.ReportSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportSubmissionRepository extends JpaRepository<ReportSubmission, UUID> {

    /** All unread reports for the admin notification badge */
    List<ReportSubmission> findByReadByAdminFalseOrderBySentAtDesc();

    /** All sent reports, newest first */
    List<ReportSubmission> findBySentAtNotNullOrderBySentAtDesc();

    /** Count of unread reports */
    long countByReadByAdminFalseAndSentAtNotNull();
}