package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.StaffSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffSalaryRepository extends JpaRepository<StaffSalary, UUID> {
    List<StaffSalary> findByStatus(String status);
    List<StaffSalary> findByStatusOrderByYearDescMonthDesc(String status);
    List<StaffSalary> findByYearAndMonth(int year, int month);
    Optional<StaffSalary> findByUserIdAndYearAndMonth(UUID userId, int year, int month);
    List<StaffSalary> findByUserId(UUID userId);

    /** Latest paid salary not yet notified on the portal */
    List<StaffSalary> findByUserIdAndStatusAndSalaryNotificationReadFalseOrderByPaidAtDesc(UUID userId, String status);
}