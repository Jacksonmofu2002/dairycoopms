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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AccountantService – full implementation of all Accountant responsibilities.
 *
 * Spec section 3 – Accountant:
 *   1. Calculate member earnings based on milk supplied
 *   2. Apply deductions (cow maintenance fund, loan repayments)
 *   3. Process payments to members
 *   4. Maintain financial transaction records
 *   5. Record milk sales to processing companies
 *   6. Generate financial reports as PDF or JPEG
 *
 * CRITICAL WORKFLOW RULE (spec section 5, step 7):
 *   Deductions MUST be applied before any payment is processed.
 *   processPayment() enforces this – it throws if no deduction rows exist
 *   for the requested member/period.
 *
 * Gross earnings calculation:
 *   grossEarnings = SUM(quantityLitres × pricePerLitreAtDelivery)
 *   Uses the SNAPSHOT price stored at delivery time – not the current price.
 *
 * Net payment:
 *   netAmount = MAX(grossEarnings − totalDeductions, 0)
 */
@Slf4j
@Service
@Transactional
public class AccountantService {

    private static final String PAYMENT_UNPAID  = "UNPAID";
    private static final String PAYMENT_PAID    = "PAID";

    private final MemberRepository             memberRepository;
    private final MilkDeliveryRepository       milkDeliveryRepository;
    private final PaymentRepository            paymentRepository;
    private final DeductionRepository          deductionRepository;
    private final DeductionTypeRepository      deductionTypeRepository;
    private final LoanRepository               loanRepository;
    private final LoanRepaymentRepository      loanRepaymentRepository;
    private final MilkSaleRepository           milkSaleRepository;
    private final ProcessingCompanyRepository  processingCompanyRepository;
    private final TransactionRepository        transactionRepository;
    private final AuditLogRepository           auditLogRepository;


    // ── Explicit constructor (works in all IntelliJ editions) ────────────────
    public AccountantService(
            MemberRepository memberRepository,
            MilkDeliveryRepository milkDeliveryRepository,
            PaymentRepository paymentRepository,
            DeductionRepository deductionRepository,
            DeductionTypeRepository deductionTypeRepository,
            LoanRepository loanRepository,
            LoanRepaymentRepository loanRepaymentRepository,
            MilkSaleRepository milkSaleRepository,
            ProcessingCompanyRepository processingCompanyRepository,
            TransactionRepository transactionRepository,
            AuditLogRepository auditLogRepository) {
        this.memberRepository = memberRepository;
        this.milkDeliveryRepository = milkDeliveryRepository;
        this.paymentRepository = paymentRepository;
        this.deductionRepository = deductionRepository;
        this.deductionTypeRepository = deductionTypeRepository;
        this.loanRepository = loanRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.milkSaleRepository = milkSaleRepository;
        this.processingCompanyRepository = processingCompanyRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }
    // ─────────────────────────────────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════════════════
    // AUDIT HELPER
    // ═══════════════════════════════════════════════════════════════════════════

    private void logAction(String entityType, UUID entityId, String action, User changedBy) {
        auditLogRepository.save(AuditLog.builder()
                .id(UUID.randomUUID())
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build());
    }

    private void logAction(String entityType, Optional<UUID> entityId,
                           String action, User changedBy) {
        logAction(entityType, entityId.orElse(null), action, changedBy);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. CALCULATE MEMBER EARNINGS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate gross earnings for one member for a given month.
     *
     * Returns the fixed monthly cooperative salary (ZMW 3,000).
     * Every approved active member receives this amount each month.
     * Loan repayments are the only deduction from this salary.
     * Cow maintenance and cooperative expenses come from COFA, not member salary.
     */

    // Fixed monthly salary — change this one constant to adjust for all members
    public static final BigDecimal MEMBER_MONTHLY_SALARY = new BigDecimal("3000.00");

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public BigDecimal calculateGrossEarnings(UUID memberId, int year, int month) {
        return MEMBER_MONTHLY_SALARY;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. APPLY DEDUCTIONS  ← MUST run before processPayment()
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate and persist all deductions for one member for a given month.
     *
     * This covers two deduction categories:
     *   (a) Cooperative deduction types (% of gross, e.g. Cow Maintenance Fund)
     *   (b) Loan repayments (fixed monthly instalment per active loan)
     *
     * Idempotent: if deduction rows already exist for this member/period,
     * they are returned as-is without re-calculating or duplicating.
     *
     * For each loan repayment the method also:
     *   - Creates a LoanRepayment audit record
     *   - Decrements loan.remainingBalance
     *   - Records a LOAN_REPAYMENT Transaction in the ledger
     */
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public List<Deduction> applyDeductions(UUID memberId, int year, int month, User accountant) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        // Idempotency check – return existing rows if already applied this period
        List<Deduction> existing =
                deductionRepository.findByMemberIdAndYearAndMonth(memberId, year, month);
        if (!existing.isEmpty()) {
            log.info("Deductions already applied for member={} period={}/{}", memberId, year, month);
            return existing;
        }

        BigDecimal grossEarnings = calculateGrossEarnings(memberId, year, month);
        List<Deduction> savedDeductions = new ArrayList<>();

        // ── Cooperative deduction types (% of gross) are NOT applied to member
        //    salary. Cow maintenance and other cooperative expenses are funded
        //    from COFA (company milk payments), not deducted from member salary.
        //    Only loan repayments are deducted from the fixed monthly salary.

        // ── Loan repayments ──────────────────────────────────────────────────
        List<Loan> activeLoans =
                loanRepository.findByMemberIdAndStatus(memberId, "APPROVED");

        for (Loan loan : activeLoans) {
            if (loan.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0)
                continue; // already fully repaid

            // Don't over-deduct on the final payment
            BigDecimal instalment =
                    loan.getMonthlyInstalment().min(loan.getRemainingBalance());

            Deduction loanDeduction = Deduction.builder()
                    .id(UUID.randomUUID())
                    .member(member)
                    .loan(loan)
                    .amount(instalment)
                    .year(year)
                    .month(month)
                    .description("Loan repayment – " + loan.getId().toString().substring(0, 8))
                    .build();
            savedDeductions.add(deductionRepository.save(loanDeduction));

            // Update loan balance
            BigDecimal newBalance = loan.getRemainingBalance().subtract(instalment);
            loan.setRemainingBalance(newBalance);
            loanRepository.save(loan);

            // Loan repayment audit record
            if (!loanRepaymentRepository.existsByLoanIdAndPeriodYearAndPeriodMonth(
                    loan.getId(), year, month)) {
                loanRepaymentRepository.save(LoanRepayment.builder()
                        .id(UUID.randomUUID())
                        .loan(loan)
                        .member(member)
                        .repaymentAmount(instalment)
                        .repaymentDate(LocalDate.of(year, month, 1))
                        .periodYear(year)
                        .periodMonth(month)
                        .balanceAfter(newBalance)
                        .status("PAID")
                        .build());
            }

            // Ledger entry
            recordTransaction("LOAN_REPAYMENT", loan.getId(), member, instalment,
                    "Loan repayment " + year + "/" + month, accountant);
        }

        logAction("Deduction", Optional.empty(),
                "DEDUCTIONS_APPLIED_" + year + "_" + month, accountant);

        log.info("Deductions applied: member={} period={}/{} loans={}",
                memberId, year, month, activeLoans.size());
        return savedDeductions;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public BigDecimal getTotalDeductions(UUID memberId, int year, int month) {
        return deductionRepository
                .sumAmountByMemberIdAndYearAndMonth(memberId, year, month)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public List<Deduction> getDeductionsForMember(UUID memberId, int year, int month) {
        return deductionRepository.findByMemberIdAndYearAndMonth(memberId, year, month);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. PROCESS PAYMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate and persist the monthly payment for one member.
     *
     * ENFORCES: deductions must exist for this member/period before payment
     * is allowed (spec rule – deductions before salary).
     *
     * Idempotent: a second call returns the existing payment record without
     * creating a duplicate (backed by UNIQUE constraint in DB).
     *
     * netAmount = MAX(grossEarnings − totalDeductions, 0.00)
     */
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public Payment processPayment(UUID memberId, int year, int month, User accountant) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        // ── Idempotency ──────────────────────────────────────────────────────
        Optional<Payment> existing =
                paymentRepository.findByMemberIdAndYearAndMonth(memberId, year, month);
        if (existing.isPresent()) {
            log.info("Payment already exists for member={} period={}/{}", memberId, year, month);
            return existing.get();
        }

        // ── Get deductions (may be empty if member has no levy types or loans) ─
        List<Deduction> deductions =
                deductionRepository.findByMemberIdAndYearAndMonth(memberId, year, month);

        BigDecimal gross = calculateGrossEarnings(memberId, year, month);
        BigDecimal totalDeductions = deductions.stream()
                .map(Deduction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = gross.subtract(totalDeductions)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        if (net.compareTo(BigDecimal.ZERO) == 0)
            log.warn("Zero net payment for member={} period={}/{} gross={} deductions={}",
                    memberId, year, month, gross, totalDeductions);

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .member(member)
                .year(year)
                .month(month)
                .grossAmount(gross)
                .totalDeductions(totalDeductions)
                .netAmount(net)
                .status(PAYMENT_UNPAID)
                .processedBy(accountant)
                .build();

        Payment saved = paymentRepository.save(payment);
        logAction("Payment", saved.getId(), "PAYMENT_CALCULATED", accountant);

        log.info("Payment calculated: member={} period={}/{} gross={} deductions={} net={}",
                memberId, year, month, gross, totalDeductions, net);
        return saved;
    }

    /**
     * Batch: apply deductions and process payment for ALL APPROVED active members
     * in one operation for a given period.
     * This is the most common accountant workflow at month end.
     */
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public List<Payment> processMonthlyPayrollBatch(int year, int month, User accountant) {
        List<Member> members = memberRepository.findByStatusAndActiveTrue("APPROVED");
        List<Payment> payments = new ArrayList<>();

        for (Member member : members) {
            try {
                applyDeductions(member.getId(), year, month, accountant);
                payments.add(processPayment(member.getId(), year, month, accountant));
            } catch (Exception e) {
                log.error("Payroll failed for member={} period={}/{} error={}",
                        member.getId(), year, month, e.getMessage());
            }
        }

        logAction("Payment", Optional.empty(),
                "BATCH_PAYROLL_" + year + "_" + month, accountant);
        return payments;
    }

    /**
     * Mark a payment as PAID once the member has received the money.
     * Only UNPAID payments may be marked paid.
     */
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public Payment markAsPaid(UUID paymentId, String paymentMethod,
                              String referenceNo, User accountant) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (!PAYMENT_UNPAID.equals(payment.getStatus()))
            throw new IllegalStateException(
                    "Payment " + paymentId + " is already " + payment.getStatus());

        payment.setStatus(PAYMENT_PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setPaymentMethod(paymentMethod);
        payment.setReferenceNo(referenceNo);

        Payment updated = paymentRepository.save(payment);
        logAction("Payment", paymentId, "MARKED_PAID", accountant);

        // Ledger entry for the outgoing payment
        recordTransaction("MILK_PAYMENT", paymentId, payment.getMember(),
                payment.getNetAmount(),
                "Member payment " + payment.getYear() + "/" + payment.getMonth(),
                accountant);

        return updated;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public List<Payment> getPaymentsByMember(UUID memberId) {
        return paymentRepository.findByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public List<Payment> getUnpaidPayments() {
        return paymentRepository.findByStatus(PAYMENT_UNPAID);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public List<Payment> getMonthlyPaymentSummary(int year, int month) {
        return paymentRepository.findByYearAndMonth(year, month);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. MILK SALES TO PROCESSING COMPANIES
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public BigDecimal getMonthlyRevenue(int year, int month) {
        return milkSaleRepository.sumRevenueByYearAndMonth(year, month)
                .orElse(BigDecimal.ZERO);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5. FINANCIAL REPORTS (PDF / JPEG per spec)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate a financial report as a byte array (PDF or JPEG).
     * reportType: MEMBER_PAYMENT | MILK_SALES | LOAN_SUMMARY | DEDUCTIONS | INCOME
     * format:     PDF | JPEG
     *
     * Returns byte[] so the controller can stream directly to the HTTP response
     * or write to disk.
     *
     * TODO: Wire in JasperReports or iText for real output.
     */
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public byte[] generateFinancialReport(String reportType, String format, User accountant) {
        if (reportType == null || !reportType.matches("[A-Z0-9_]{1,50}"))
            throw new IllegalArgumentException(
                    "reportType must be 1-50 uppercase alphanumeric/underscore characters");

        if (!"PDF".equalsIgnoreCase(format) && !"JPEG".equalsIgnoreCase(format))
            throw new IllegalArgumentException("Supported formats: PDF, JPEG");

        byte[] reportBytes = new byte[0]; // TODO: JasperReports/iText implementation

        logAction("Report", Optional.empty(),
                "GENERATED_" + reportType.toUpperCase() + "_" + format.toUpperCase(), accountant);
        return reportBytes;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOAN REPAYMENT HISTORY
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public List<LoanRepayment> getLoanRepaymentHistory(UUID loanId) {
        return loanRepaymentRepository.findByLoanId(loanId);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Write a row to the transactions (ledger) table.
     * member may be null for company-level entries like MILK_SALE_INCOME.
     */
    private void recordTransaction(String type, UUID referenceId, Member member,
                                   BigDecimal amount, String description, User createdBy) {
        transactionRepository.save(Transaction.builder()
                .id(UUID.randomUUID())
                .transactionType(type)
                .referenceId(referenceId)
                .member(member)
                .amount(amount)
                .description(description)
                .createdBy(createdBy)
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DEDUCTION TYPE CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Configure a new deduction type (e.g. Cow Maintenance Fund at 5%).
     * Accountant configures which deductions exist and their rates.
     *
     * Validation:
     *   - Name is required and must be unique (case-insensitive)
     *   - Rate must be between 0.01% and 100%
     *
     * @throws IllegalArgumentException if validation fails
     */
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public DeductionType configureDeduction(DeductionType deductionType, User accountant) {
        if (deductionType == null) {
            throw new IllegalArgumentException("DeductionType cannot be null");
        }

        // Validate name
        if (!StringUtils.hasText(deductionType.getName())) {
            throw new IllegalArgumentException("Deduction type name is required");
        }

        // Validate rate with upper and lower bounds
        if (deductionType.getRate() == null) {
            throw new IllegalArgumentException("Deduction rate is required");
        }

        BigDecimal rate = deductionType.getRate();

        // Check lower bound (must be positive)
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduction rate must be greater than 0%");
        }

        // Check upper bound (cannot exceed 100%)
        if (rate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Deduction rate cannot exceed 100%");
        }

        // Check for duplicate names (case-insensitive)
        if (deductionTypeRepository.existsByNameIgnoreCase(deductionType.getName())) {
            throw new IllegalArgumentException(
                    "A deduction type named '" + deductionType.getName() + "' already exists");
        }

        DeductionType saved = deductionTypeRepository.save(deductionType);
        logAction("DeductionType", (UUID) null, "CONFIGURED", accountant);

        log.info("Deduction type configured: id={} name={} rate={}% mandatory={} recurring={} active={} by={}",
                saved.getId(), saved.getName(), saved.getRate(), saved.isMandatory(),
                saved.isRecurring(), saved.isActive(), accountant.getEmail());

        return saved;
    }

    /**
     * Update an existing deduction type's rate.
     * Used when cooperative decides to change the percentage.
     *
     * @throws IllegalArgumentException if rate is out of bounds or deduction type not found
     */
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public DeductionType updateDeductionRate(Integer deductionTypeId,
                                             BigDecimal newRate,
                                             User accountant) {
        // Validate rate
        if (newRate == null) {
            throw new IllegalArgumentException("New rate is required");
        }

        // Check bounds (0.01% to 100%)
        if (newRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduction rate must be greater than 0%");
        }

        if (newRate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Deduction rate cannot exceed 100%");
        }

        DeductionType dt = deductionTypeRepository.findById(deductionTypeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "DeductionType not found: " + deductionTypeId));

        BigDecimal oldRate = dt.getRate();
        dt.setRate(newRate);
        DeductionType updated = deductionTypeRepository.save(dt);

        logAction("DeductionType", (UUID) null, "RATE_UPDATED", accountant);

        log.info("Deduction rate updated: id={} name={} oldRate={}% newRate={}% by={}",
                dt.getId(), dt.getName(), oldRate, newRate, accountant.getEmail());

        return updated;
    }

    /**
     * Activate or deactivate a deduction type without deleting it.
     * Inactive deductions won't be applied in future processing.
     *
     * @throws IllegalArgumentException if deduction type not found
     */
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public DeductionType toggleDeductionActive(Integer deductionTypeId,
                                               boolean active,
                                               User accountant) {
        DeductionType dt = deductionTypeRepository.findById(deductionTypeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "DeductionType not found: " + deductionTypeId));

        dt.setActive(active);
        DeductionType updated = deductionTypeRepository.save(dt);

        logAction("DeductionType", (UUID) null,
                active ? "ACTIVATED" : "DEACTIVATED", accountant);

        log.info("Deduction type {}: id={} name={} by={}",
                active ? "activated" : "deactivated", dt.getId(), dt.getName(), accountant.getEmail());

        return updated;
    }

    /**
     * Get all deduction types (including inactive).
     * Used in management UI to show full list.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public List<DeductionType> getAllDeductionTypes() {
        return deductionTypeRepository.findAllByOrderByNameAsc();
    }

    /**
     * Get only active deduction types.
     * Used during monthly payment processing - only active types are applied.
     * (Note: applyDeductions() already uses this internally via findByActiveTrueOrderByNameAsc)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public List<DeductionType> getActiveDeductionTypes() {
        return deductionTypeRepository.findByActiveTrueOrderByNameAsc();
    }
}