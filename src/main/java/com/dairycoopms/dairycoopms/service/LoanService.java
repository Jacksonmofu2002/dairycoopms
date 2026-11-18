package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.*;
import com.dairycoopms.dairycoopms.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * LoanService
 *
 * CORRECTED LOAN WORKFLOW:
 *
 *   Step 1 – LOAN_OFFICER meets a member, captures their request and submits
 *             it into the system via requestLoan(). Status = PENDING.
 *
 *   Step 2 – ADMIN reviews all PENDING loans and either approves or rejects.
 *             Approval/rejection is handled in AdminService (approveLoan /
 *             rejectLoan) which keeps @PreAuthorize("hasRole('ADMIN')").
 *
 *   Step 3 – ACCOUNTANT sees all APPROVED loans, disburses money to the
 *             member's account, and deducts monthly repayments from payroll.
 *
 * Rules:
 *   - Only LOAN_OFFICER submits loan requests (on behalf of members)
 *   - Only ADMIN approves or rejects
 *   - Only ACCOUNTANT handles disbursement and repayment deductions
 *   - A member can only have ONE pending application at a time
 *   - A member cannot request a new loan while one is still unpaid
 */
@Slf4j
@Service
@Transactional
public class LoanService {

    private static final String STATUS_PENDING  = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";

    private final LoanRepository      loanRepository;
    private final MemberRepository    memberRepository;
    private final AuditLogRepository  auditLogRepository;

    public LoanService(LoanRepository loanRepository,
                       MemberRepository memberRepository,
                       AuditLogRepository auditLogRepository) {
        this.loanRepository   = loanRepository;
        this.memberRepository = memberRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ── Audit ─────────────────────────────────────────────────────────────────

    private void logAction(String entityType, UUID entityId,
                           String action, User changedBy) {
        auditLogRepository.save(AuditLog.builder()
                .id(UUID.randomUUID())
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STEP 1 – LOAN_OFFICER submits a loan request on behalf of a member
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * The LOAN_OFFICER captures a member's loan request and submits it.
     * The ADMIN will then review it and approve or reject.
     *
     * @param memberId       the cooperative member requesting the loan
     * @param amount         loan amount in ZMW – must be positive
     * @param repaymentMonths repayment period 1–60 months
     * @param purpose        reason for the loan – mandatory
     * @param loanOfficer    the logged-in LOAN_OFFICER submitting the request
     */
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public Loan requestLoan(UUID memberId,
                            BigDecimal amount,
                            int repaymentMonths,
                            String purpose,
                            User loanOfficer) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Loan amount must be positive.");

        if (amount.compareTo(new BigDecimal("500")) < 0)
            throw new IllegalArgumentException("Minimum loan amount is ZMW 500. Amounts below ZMW 500 are not eligible.");

        if (repaymentMonths < 1 || repaymentMonths > 60)
            throw new IllegalArgumentException(
                    "Repayment period must be between 1 and 60 months.");

        if (!StringUtils.hasText(purpose))
            throw new IllegalArgumentException("Loan purpose is required.");

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found: " + memberId));

        if (!"APPROVED".equals(member.getStatus()))
            throw new IllegalStateException(
                    "Member " + member.getName() + " is not APPROVED. "
                            + "Only approved cooperative members may apply for loans.");

        if (!member.isActive())
            throw new IllegalStateException(
                    "Member " + member.getName() + " account is inactive.");

        // One-pending rule
        if (loanRepository.existsByMemberIdAndStatus(memberId, STATUS_PENDING))
            throw new IllegalStateException(
                    member.getName() + " already has a pending loan application. "
                            + "It must be decided (approved or rejected) before a new one can be submitted.");

        // One-active rule
        if (loanRepository.existsByMemberIdAndStatusAndRemainingBalanceGreaterThan(
                memberId, STATUS_APPROVED, BigDecimal.ZERO))
            throw new IllegalStateException(
                    member.getName() + " still has an active loan with an outstanding balance. "
                            + "It must be fully repaid before applying for another.");

        // Calculate interest rate based on loan amount tiers
        BigDecimal interestRate = calculateInterestRate(amount);

        // Total repayable = principal + interest
        BigDecimal interestAmount = amount.multiply(interestRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalRepayable = amount.add(interestAmount);

        BigDecimal monthlyInstalment = totalRepayable
                .divide(BigDecimal.valueOf(repaymentMonths), 2, RoundingMode.HALF_UP);

        Loan loan = Loan.builder()
                .id(UUID.randomUUID())
                .member(member)
                .amount(amount)
                .remainingBalance(totalRepayable)
                .repaymentMonths(repaymentMonths)
                .monthlyInstalment(monthlyInstalment)
                .interestRate(interestRate)
                .purpose(purpose)
                .status(STATUS_PENDING)
                .requestDate(LocalDate.now())
                .build();

        Loan saved = loanRepository.save(loan);
        logAction("Loan", saved.getId(), "REQUESTED_BY_OFFICER", loanOfficer);

        log.info("Loan request submitted by officer={} for member={} amount={} months={}",
                loanOfficer.getEmail(), member.getMemberNumber(), amount, repaymentMonths);
        return saved;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // STEP 2 – LOAN_OFFICER views (read-only queries)
    // ═════════════════════════════════════════════════════════════════════════

    /** All members who are APPROVED and active – shown in the loan request form dropdown */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public List<Member> getEligibleMembers() {
        return memberRepository.findByStatusAndActiveTrue("APPROVED");
    }

    /** All loans submitted by this officer or visible to them */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER') or hasRole('ADMIN')")
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER') or hasRole('ADMIN')")
    public List<Loan> getPendingLoans() {
        return loanRepository.findByStatus(STATUS_PENDING);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER') or hasRole('ADMIN')")
    public List<Loan> getApprovedLoans() {
        return loanRepository.findByStatus(STATUS_APPROVED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER') or hasRole('ADMIN')")
    public List<Loan> getLoansByMember(UUID memberId) {
        return loanRepository.findByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER') or hasRole('ADMIN')")
    public Loan getLoanById(UUID loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Loan not found: " + loanId));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BALANCE TRACKING  (used by Accountant dashboard)
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public BigDecimal getRemainingBalance(UUID loanId) {
        return getLoanById(loanId).getRemainingBalance();
    }

    @Transactional(readOnly = true)
    public boolean isFullyRepaid(UUID loanId) {
        return getLoanById(loanId).getRemainingBalance()
                .compareTo(BigDecimal.ZERO) <= 0;
    }

    @Transactional(readOnly = true)
    public BigDecimal getRepaymentProgress(UUID loanId) {
        Loan loan = getLoanById(loanId);
        if (loan.getAmount().compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.valueOf(100);
        BigDecimal repaid = loan.getAmount().subtract(loan.getRemainingBalance());
        return repaid.multiply(BigDecimal.valueOf(100))
                .divide(loan.getAmount(), 1, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return loanRepository.countByStatus(status);
    }

    /**
     * Interest rate tiers:
     *   ZMW 500  - 999  : 10%
     *   ZMW 1000 - 1500 : 15%
     *   ZMW 1600 - 2000 : 20%
     *   ZMW 2001+       : 20% + 5% per additional ZMW 1000 above 2000
     */
    public static BigDecimal calculateInterestRate(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        double amt = amount.doubleValue();
        double rate;
        if (amt < 500) {
            rate = 0;
        } else if (amt <= 999) {
            rate = 10;
        } else if (amt <= 1500) {
            rate = 15;
        } else if (amt <= 2000) {
            rate = 20;
        } else {
            // 5% per additional ZMW 1000 above 2000
            int extraThousands = (int)((amt - 2000) / 1000);
            rate = 20 + (extraThousands + 1) * 5;
        }
        return BigDecimal.valueOf(rate);
    }
}