package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Loan entity.
 *
 * Method names map directly to service calls:
 *
 *   LoanService.requestLoan():
 *     existsByMemberIdAndStatus()                              → pending loan check
 *     existsByMemberIdAndStatusAndRemainingBalanceGreaterThan()→ active loan check
 *
 *   AdminService / LoanService:
 *     findByStatus()     → getPendingLoans(), getActiveLoans()
 *     findByMemberId()   → getLoansByMember()
 *     countByStatus()    → dashboard active loan count
 *
 *   AccountantService.applyDeductions():
 *     findByMemberIdAndStatus() → gets APPROVED loans to deduct repayments
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    // ── Status queries ───────────────────────────────────────────────────────

    List<Loan> findByStatus(String status);

    long countByStatus(String status);

    // ── Member queries ───────────────────────────────────────────────────────

    List<Loan> findByMemberId(UUID memberId);

    List<Loan> findByMemberIdAndStatus(UUID memberId, String status);

    // ── Duplicate loan guards (LoanService.requestLoan) ──────────────────────

    /**
     * True if the member already has a loan in the given status.
     * Used to block a second PENDING loan application.
     */
    boolean existsByMemberIdAndStatus(UUID memberId, String status);

    /**
     * True if the member has an APPROVED loan with outstanding balance > 0.
     * Used to block a new loan while one is still being repaid.
     * Maps to: WHERE member_id = ? AND status = ? AND outstanding_balance > ?
     */
    boolean existsByMemberIdAndStatusAndRemainingBalanceGreaterThan(
            UUID memberId, String status, BigDecimal balance);
}