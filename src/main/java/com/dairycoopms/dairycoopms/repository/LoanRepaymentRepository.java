package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LoanRepayment entity.
 *
 * findByLoanIdAndPeriodYearAndPeriodMonth() checks whether a repayment
 * has already been recorded for this loan in the current month (idempotency).
 */
@Repository
public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, UUID> {

    List<LoanRepayment> findByLoanId(UUID loanId);

    List<LoanRepayment> findByMemberId(UUID memberId);

    Optional<LoanRepayment> findByLoanIdAndPeriodYearAndPeriodMonth(
            UUID loanId, int year, int month);

    boolean existsByLoanIdAndPeriodYearAndPeriodMonth(
            UUID loanId, int year, int month);
}