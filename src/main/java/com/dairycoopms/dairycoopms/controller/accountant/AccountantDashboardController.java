package com.dairycoopms.dairycoopms.controller.accountant;

import com.dairycoopms.dairycoopms.repository.UserRepository;
import com.dairycoopms.dairycoopms.repository.MemberRepository;

import java.util.*;

import com.dairycoopms.dairycoopms.model.Member;
import com.dairycoopms.dairycoopms.model.MilkInquiry;
import com.dairycoopms.dairycoopms.model.CooperativeFundTransaction;

import java.util.Map;
import java.util.stream.Collectors;
import com.dairycoopms.dairycoopms.service.AdminService;
import com.dairycoopms.dairycoopms.service.LoanService;
import com.dairycoopms.dairycoopms.service.MilkInquiryService;
import com.dairycoopms.dairycoopms.service.EmailService;
import com.dairycoopms.dairycoopms.service.AccountantService;
import com.dairycoopms.dairycoopms.service.ReferenceNumberService;
import com.dairycoopms.dairycoopms.repository.LoanRepository;
import com.dairycoopms.dairycoopms.repository.LoanRepaymentRepository;
import com.dairycoopms.dairycoopms.repository.CooperativeFundRepository;
import com.dairycoopms.dairycoopms.repository.CooperativeFundTransactionRepository;
import com.dairycoopms.dairycoopms.model.CooperativeFund;
import com.dairycoopms.dairycoopms.repository.PaymentRepository;
import com.dairycoopms.dairycoopms.repository.StaffSalaryRepository;
import com.dairycoopms.dairycoopms.model.StaffSalary;
import com.dairycoopms.dairycoopms.model.Payment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import com.dairycoopms.dairycoopms.model.Loan;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/accountant")
@PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
public class AccountantDashboardController {

    private final LoanService loanService;
    private final LoanRepository loanRepository;
    private final CooperativeFundRepository fundRepository;
    private final CooperativeFundTransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final StaffSalaryRepository salaryRepository;
    private final AdminService adminService;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final MilkInquiryService milkInquiryService;
    private final com.dairycoopms.dairycoopms.service.CofaReconciliationService reconciliationService;
    private final EmailService emailService;
    private final AccountantService accountantService;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final ReferenceNumberService referenceNumberService;
    private final com.dairycoopms.dairycoopms.repository.DeductionRepository deductionRepository;

    public AccountantDashboardController(LoanService loanService,
                                         LoanRepository loanRepository,
                                         CooperativeFundRepository fundRepository,
                                         CooperativeFundTransactionRepository transactionRepository,
                                         PaymentRepository paymentRepository,
                                         StaffSalaryRepository salaryRepository,
                                         AdminService adminService,
                                         UserRepository userRepository,
                                         MemberRepository memberRepository,
                                         MilkInquiryService milkInquiryService,
                                         com.dairycoopms.dairycoopms.service.CofaReconciliationService reconciliationService,
                                         EmailService emailService,
                                         AccountantService accountantService,
                                         LoanRepaymentRepository loanRepaymentRepository,
                                         ReferenceNumberService referenceNumberService,
                                         com.dairycoopms.dairycoopms.repository.DeductionRepository deductionRepository) {
        this.loanService = loanService;
        this.loanRepository = loanRepository;
        this.fundRepository = fundRepository;
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
        this.salaryRepository = salaryRepository;
        this.adminService = adminService;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.milkInquiryService = milkInquiryService;
        this.reconciliationService = reconciliationService;
        this.emailService = emailService;
        this.accountantService = accountantService;
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.referenceNumberService = referenceNumberService;
        this.deductionRepository   = deductionRepository;
    }

    /**
     * Add badge counts to all accountant pages (like your Reports badge!)
     * Simple approach - just counts from existing database tables
     */
    @ModelAttribute
    public void addBadgeCounts(Model model) {
        // ── Pending milk payments (companies awaiting confirmation) ───────────
        long pendingMilkPayments = milkInquiryService.getAllInquiries(null).stream()
                .filter(i -> i.getStatus() == MilkInquiry.Status.PAID)
                .count();
        model.addAttribute("pendingMilkPayments", pendingMilkPayments);

        // ── Unsent receipts ───────────────────────────────────────────────────
        long unsentReceipts = milkInquiryService.getAllInquiries(null).stream()
                .filter(i -> i.getStatus() == MilkInquiry.Status.PAID && !i.isReceiptSent())
                .count();
        model.addAttribute("unsentReceipts", unsentReceipts);

        // ── Nav badge: pending loan applications ──────────────────────────────
        // Used by accountant-nav.html fragment to show the red badge on "Loan Management"
        long pendingLoansCount = loanRepository.findByStatus("PENDING").size();
        model.addAttribute("pendingLoansCount", pendingLoansCount);

        // ── Nav badge: invoiced milk payments awaiting member payment ─────────
        // Used by accountant-nav.html fragment to show the red badge on "Milk Payments"
        long invoicedCount = milkInquiryService.getAllInquiries(null).stream()
                .filter(i -> i.getStatus() == MilkInquiry.Status.INVOICED)
                .count();
        model.addAttribute("invoicedCount", invoicedCount);
    }

    //  Dashboard
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
            model.addAttribute("mustChangePassword", u.isMustChangePassword());
        });
        var pending = loanService.getPendingLoans();
        var approved = loanService.getApprovedLoans();
        var all = loanService.getAllLoans();
        model.addAttribute("pendingLoans", pending);
        model.addAttribute("approvedLoans", approved);
        model.addAttribute("pendingCount", pending.size());
        model.addAttribute("approvedCount", approved.size());
        model.addAttribute("totalCount", all.size());
        return "accountant/dashboard";
    }

    //  Loan list
    @GetMapping("/loans")
    public String loans(@RequestParam(required = false) String status, Model model,
                        @AuthenticationPrincipal UserDetails ud) {
        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
            model.addAttribute("mustChangePassword", u.isMustChangePassword());
        });
        var loans = (status != null && !status.isBlank())
                ? loanService.getAllLoans().stream()
                .filter(l -> l.getStatus().equalsIgnoreCase(status)).toList()
                : loanService.getAllLoans();
        model.addAttribute("loans", loans.stream().filter(l -> l != null).toList());
        model.addAttribute("filterStatus", status);
        model.addAttribute("pendingCount", loanService.getPendingLoans().size());
        model.addAttribute("approvedCount", loanService.getApprovedLoans().size());
        model.addAttribute("totalCount", loanService.getAllLoans().size());

        // Repayment history per loan for amortization schedule display
        var repaymentMap = new java.util.HashMap<UUID, java.util.List<com.dairycoopms.dairycoopms.model.LoanRepayment>>();
        loans.forEach(l -> repaymentMap.put(l.getId(),
                loanRepaymentRepository.findByLoanId(l.getId())));
        model.addAttribute("repaymentMap", repaymentMap);

        return "accountant/loans/list";
    }

    //  Approve loan
    @PostMapping("/loans/{id}/approve")
    public String approveLoan(@PathVariable UUID id,
                              @AuthenticationPrincipal UserDetails ud,
                              RedirectAttributes flash) {
        var officer = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            adminService.approveLoan(id, officer);
            flash.addFlashAttribute("successMessage", "Loan approved successfully.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/accountant/loans";
    }

    //  Reject loan
    @PostMapping("/loans/{id}/reject")
    public String rejectLoan(@PathVariable UUID id,
                             @RequestParam String reason,
                             @AuthenticationPrincipal UserDetails ud,
                             RedirectAttributes flash) {
        var officer = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            adminService.rejectLoan(id, reason, officer);
            flash.addFlashAttribute("successMessage", "Loan rejected.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/accountant/loans";
    }

    // ── Disbursements ─────────────────────────────────────────────────────
    @GetMapping("/disbursements")
    public String disbursements(@AuthenticationPrincipal UserDetails ud, Model model) {
        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
            model.addAttribute("mustChangePassword", u.isMustChangePassword());
        });
        // Loans approved but not yet disbursed
        var allApproved = loanRepository.findAll().stream()
                .filter(l -> "APPROVED".equals(l.getStatus()))
                .toList();

        var pending = allApproved.stream()
                .filter(l -> !"DISBURSED".equals(l.getDisbursementStatus()))
                .toList();

        var disbursed = allApproved.stream()
                .filter(l -> "DISBURSED".equals(l.getDisbursementStatus()))
                .toList();

        long noBank = pending.stream()
                .filter(l -> l.getMember() == null || l.getMember().getAccountNumber() == null)
                .count();

        // Cooperative fund balance
        CooperativeFund fund = fundRepository.findById(1)
                .orElseGet(CooperativeFund::new);
        model.addAttribute("fundBalance", fund.getBalance());
        model.addAttribute("fund", fund);

        // COFA Income Breakdown - Current Month
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // Get totals by transaction type for current month
        BigDecimal milkSalesIncome = transactionRepository.sumByTypeAndPeriod(
                CooperativeFundTransaction.TransactionType.MILK_SALE, monthStart, monthEnd);
        BigDecimal manualTopups = transactionRepository.sumByTypeAndPeriod(
                CooperativeFundTransaction.TransactionType.MANUAL_TOPUP, monthStart, monthEnd);
        BigDecimal loanRepayments = transactionRepository.sumByTypeAndPeriod(
                CooperativeFundTransaction.TransactionType.LOAN_REPAYMENT, monthStart, monthEnd);

        // Total income this month
        BigDecimal totalIncomeThisMonth = (milkSalesIncome != null ? milkSalesIncome : BigDecimal.ZERO)
                .add(manualTopups != null ? manualTopups : BigDecimal.ZERO)
                .add(loanRepayments != null ? loanRepayments : BigDecimal.ZERO);

        model.addAttribute("milkSalesIncome", milkSalesIncome != null ? milkSalesIncome : BigDecimal.ZERO);
        model.addAttribute("manualTopups", manualTopups != null ? manualTopups : BigDecimal.ZERO);
        model.addAttribute("loanRepayments", loanRepayments != null ? loanRepayments : BigDecimal.ZERO);
        model.addAttribute("totalIncomeThisMonth", totalIncomeThisMonth);
        model.addAttribute("currentMonthName", currentMonth.getMonth().toString() + " " + currentMonth.getYear());

        // Recent transactions (last 10)
        List<CooperativeFundTransaction> recentTransactions = transactionRepository.findTop20ByOrderByCreatedAtDesc();
        model.addAttribute("recentTransactions", recentTransactions.size() > 10 ?
                recentTransactions.subList(0, 10) : recentTransactions);

        model.addAttribute("pendingDisbursements", pending);
        model.addAttribute("disbursedLoans", disbursed);
        model.addAttribute("pendingNoBank", noBank);
        model.addAttribute("pendingCount", loanService.getPendingLoans().size());
        return "accountant/disbursements";
    }

    // ── Process disbursement ──────────────────────────────────────────────
    @PostMapping("/disbursements/{id}/process")
    public String processDisbursement(@PathVariable java.util.UUID id,
                                      @RequestParam(required = false) String notes,
                                      @AuthenticationPrincipal UserDetails ud,
                                      RedirectAttributes flash) {

        String reference = referenceNumberService.nextDisbursementRef();

        System.out.println("=== DISBURSEMENT REQUEST RECEIVED ===");
        System.out.println("Loan ID: " + id);
        System.out.println("Reference: " + reference);
        System.out.println("Notes: " + notes);
        System.out.println("User: " + ud.getUsername());

        var officer = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            System.out.println("Finding loan...");
            Loan loan = loanRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

            System.out.println("Loan found: " + loan.getId());
            System.out.println("Current status: " + loan.getStatus());
            System.out.println("Current disbursement status: " + loan.getDisbursementStatus());

            if (!"APPROVED".equals(loan.getStatus())) {
                System.out.println("ERROR: Loan not approved. Status: " + loan.getStatus());
                throw new IllegalStateException("Only approved loans can be disbursed. Current status: " + loan.getStatus());
            }

            if ("DISBURSED".equals(loan.getDisbursementStatus())) {
                System.out.println("ERROR: Loan already disbursed");
                throw new IllegalStateException("This loan has already been disbursed.");
            }

            if (!org.springframework.util.StringUtils.hasText(reference)) {
                System.out.println("ERROR: No reference provided");
                throw new IllegalArgumentException("Payment reference is required.");
            }

            // Check fund availability
            System.out.println("Checking fund availability...");
            CooperativeFund fund = fundRepository.findById(1)
                    .orElseThrow(() -> new IllegalStateException("Cooperative fund not initialised."));

            System.out.println("Fund balance: " + fund.getBalance());
            System.out.println("Loan amount: " + loan.getAmount());

            if (!fund.hasSufficientFunds(loan.getAmount())) {
                System.out.println("WARNING: Insufficient funds");
                // Mark as awaiting funds — member will see "Awaiting Payment"
                loan.setDisbursementStatus("AWAITING_FUNDS");
                loanRepository.save(loan);
                flash.addFlashAttribute("errorMessage",
                        "Insufficient funds. Fund balance: ZMW " + fund.getBalance() +
                                " | Required: ZMW " + loan.getAmount() +
                                ". Loan marked as AWAITING FUNDS. Top up the fund first.");
                return "redirect:/accountant/disbursements";
            }

            // Deduct from fund and disburse
            System.out.println("Processing disbursement...");
            BigDecimal newBalance = fund.getBalance().subtract(loan.getAmount());
            fund.setBalance(newBalance);
            fundRepository.save(fund);
            System.out.println("Fund updated. New balance: " + newBalance);

            loan.setDisbursementStatus("DISBURSED");
            loan.setDisbursedAt(java.time.LocalDateTime.now());
            loan.setDisbursedBy(officer);
            loan.setDisbursementReference(reference.trim());
            loan.setDisbursementNotes(notes);
            Loan savedLoan = loanRepository.save(loan);
            System.out.println("Loan saved. New disbursement status: " + savedLoan.getDisbursementStatus());

            // Log the disbursement transaction (negative amount)
            CooperativeFundTransaction transaction = CooperativeFundTransaction.builder()
                    .transactionType(CooperativeFundTransaction.TransactionType.LOAN_DISBURSEMENT)
                    .amount(loan.getAmount().negate()) // Negative for outgoing
                    .balanceAfter(newBalance)
                    .referenceNo(reference.trim())
                    .description("Loan disbursement to " + loan.getMember().getName() +
                            " (" + loan.getMember().getMemberNumber() + ")")
                    .performedBy(officer)
                    .createdAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(transaction);
            System.out.println("Transaction logged");

            flash.addFlashAttribute("successMessage",
                    "✅ Disbursement complete! ZMW " + loan.getAmount() +
                            " paid to " + loan.getMember().getName() +
                            " | Ref: " + reference +
                            " | New fund balance: ZMW " + newBalance);

            System.out.println("=== DISBURSEMENT COMPLETED SUCCESSFULLY ===");

        } catch (Exception e) {
            System.out.println("ERROR during disbursement: " + e.getClass().getName());
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
            flash.addFlashAttribute("errorMessage", "ERROR: " + e.getMessage());
        }
        return "redirect:/accountant/disbursements";
    }


    // ── Salaries page ──────────────────────────────────────────────────────
    @GetMapping("/salaries")
    public String salaries(@RequestParam(required = false) Integer year,
                           @RequestParam(required = false) Integer month,
                           @AuthenticationPrincipal UserDetails ud,
                           Model model) {
        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
            model.addAttribute("mustChangePassword", u.isMustChangePassword());
        });

        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        model.addAttribute("selectedYear", y);
        model.addAttribute("selectedMonth", m);

        // Staff users with salary configured
        var staffUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && !"MEMBER".equals(u.getRole()))
                .filter(u -> u.isActive())
                .toList();
        model.addAttribute("staffUsers", staffUsers);

        // Salary records for selected period
        model.addAttribute("salaryRecords", salaryRepository.findByYearAndMonth(y, m));

        // ALL unpaid salaries across every period (for overdue tracking)
        model.addAttribute("allUnpaidSalaries",
                salaryRepository.findByStatusOrderByYearDescMonthDesc("UNPAID"));

        // Today's year/month so template can compute "X months overdue"
        model.addAttribute("todayYear", now.getYear());
        model.addAttribute("todayMonth", now.getMonthValue());

        // Month names array indexed 1–12 for display in templates
        model.addAttribute("monthNames", new String[]{
                "", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        });

        // Member payments for selected period (milk payments)
        var periodPayments = paymentRepository.findByYearAndMonth(y, m);
        model.addAttribute("memberPayments", periodPayments);

        // Build a joined list: every approved member + their payment for this period (null if not generated)
        var paymentByMemberId = periodPayments.stream()
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.getMember().getId(),
                        p -> p,
                        (a, b) -> a));

        var memberPaymentRows = memberRepository.findByStatusAndActiveTrue("APPROVED").stream()
                .map(member -> {
                    var row = new java.util.LinkedHashMap<String, Object>();
                    var pay = paymentByMemberId.get(member.getId());
                    row.put("member",   member);
                    row.put("payment",  pay);
                    row.put("hasPay",   pay != null);
                    row.put("gross",    pay != null ? pay.getGrossAmount()    : java.math.BigDecimal.ZERO);
                    row.put("deductions", pay != null ? pay.getTotalDeductions() : java.math.BigDecimal.ZERO);
                    row.put("net",      pay != null ? pay.getNetAmount()      : java.math.BigDecimal.ZERO);
                    row.put("status",   pay != null ? pay.getStatus()         : "NOT_GENERATED");
                    row.put("referenceNo", pay != null ? pay.getReferenceNo() : null);
                    row.put("hasDeduction", pay != null && pay.getTotalDeductions() != null
                            && pay.getTotalDeductions().compareTo(java.math.BigDecimal.ZERO) > 0);

                    // ── Loan breakdown ────────────────────────────────────────
                    // Active loans with outstanding balance
                    var activeLoans = loanRepository
                            .findByMemberIdAndStatus(member.getId(), "APPROVED")
                            .stream()
                            .filter(l -> l.getRemainingBalance()
                                    .compareTo(java.math.BigDecimal.ZERO) > 0)
                            .toList();
                    row.put("activeLoans", activeLoans);
                    row.put("hasLoan", !activeLoans.isEmpty());

                    if (pay != null) {
                        // Payment generated — use the actual applied deductions
                        var applied = deductionRepository
                                .findByMemberIdAndYearAndMonth(member.getId(), y, m);
                        var loanDeds = applied.stream()
                                .filter(d -> d.getLoan() != null).toList();
                        var stdTotal = applied.stream()
                                .filter(d -> d.getLoan() == null)
                                .map(com.dairycoopms.dairycoopms.model.Deduction::getAmount)
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                        var loanTotal = loanDeds.stream()
                                .map(com.dairycoopms.dairycoopms.model.Deduction::getAmount)
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                        row.put("appliedLoanDeductions", loanDeds);
                        row.put("loanDeductionTotal",   loanTotal);
                        row.put("standardDeductionTotal", stdTotal);
                    } else {
                        // Not yet generated — project based on active loans
                        var projected = activeLoans.stream()
                                .map(l -> l.getMonthlyInstalment().min(l.getRemainingBalance()))
                                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                        row.put("appliedLoanDeductions", java.util.List.of());
                        row.put("loanDeductionTotal",   projected);
                        row.put("standardDeductionTotal", java.math.BigDecimal.ZERO);
                    }

                    return row;
                }).toList();
        model.addAttribute("memberPaymentRows", memberPaymentRows);

        // All unpaid member payments across periods
        var unpaidPayments = paymentRepository.findByStatus("UNPAID").stream()
                .sorted(java.util.Comparator.comparingInt(Payment::getYear)
                        .thenComparingInt(Payment::getMonth).reversed())
                .toList();
        model.addAttribute("allUnpaidMemberPayments", unpaidPayments);

        // For each unpaid payment, resolve loan deductions so template can show breakdown
        var loanDeductionByPaymentId = new java.util.HashMap<String, java.util.Map<String, Object>>();
        for (var pmt : unpaidPayments) {
            var pmtLoanDeds = deductionRepository
                    .findByMemberIdAndYearAndMonth(pmt.getMember().getId(), pmt.getYear(), pmt.getMonth())
                    .stream().filter(d -> d.getLoan() != null).toList();
            var pmtActiveLoans = loanRepository
                    .findByMemberIdAndStatus(pmt.getMember().getId(), "APPROVED")
                    .stream().filter(l -> l.getRemainingBalance().compareTo(java.math.BigDecimal.ZERO) > 0)
                    .toList();
            var loanDedTotal = pmtLoanDeds.stream()
                    .map(com.dairycoopms.dairycoopms.model.Deduction::getAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            var info = new java.util.HashMap<String, Object>();
            info.put("loanDeductions", pmtLoanDeds);
            info.put("activeLoans",    pmtActiveLoans);
            info.put("loanTotal",      loanDedTotal);
            info.put("hasLoan",        !pmtActiveLoans.isEmpty());
            loanDeductionByPaymentId.put(pmt.getId().toString(), info);
        }
        model.addAttribute("loanDeductionByPaymentId", loanDeductionByPaymentId);

        // Stats
        model.addAttribute("pendingStaffSalaries",
                salaryRepository.findByStatus("UNPAID").size());
        model.addAttribute("pendingMemberPayments",
                paymentRepository.findByStatus("UNPAID").size());
        model.addAttribute("pendingCount", loanService.getPendingLoans().size());

        // COFA balance — shown as warning if insufficient
        try {
            fundRepository.findAll().stream().findFirst().ifPresent(fund -> {
                model.addAttribute("cofaBalance", fund.getBalance());

                // Calculate total pending outflows
                BigDecimal pendingSalaries = salaryRepository.findByStatus("UNPAID").stream()
                        .map(s -> s.getNetSalary() != null ? s.getNetSalary() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal pendingMemberPay = paymentRepository.findByStatus("UNPAID").stream()
                        .map(p -> p.getNetAmount() != null ? p.getNetAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalPending = pendingSalaries.add(pendingMemberPay);

                model.addAttribute("totalPendingOutflow", totalPending);
                model.addAttribute("cofaSufficient", fund.hasSufficientFunds(totalPending));
            });
        } catch (Exception ignored) {
            model.addAttribute("cofaBalance", BigDecimal.ZERO);
            model.addAttribute("cofaSufficient", false);
        }

        return "accountant/salaries";
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SALARY RATES (fixed by cooperative policy)
    // ADMIN = executive grade: ZMW 8,000/month
    // All other staff = standard grade: ZMW 7,000/month
    // ═══════════════════════════════════════════════════════════════════════
    private static final BigDecimal EXECUTIVE_SALARY = new BigDecimal("8000.00");
    private static final BigDecimal STANDARD_SALARY = new BigDecimal("7000.00");
    private static final java.util.Set<String> EXECUTIVE_ROLES =
            java.util.Set.of("ADMIN");

    // ── Salary preview (calculates without saving) ─────────────────────────
    // Called via fetch() from the Calculate button. Returns JSON breakdown
    // per staff member so the accountant can review before confirming.
    @GetMapping("/salaries/preview-data")
    @ResponseBody
    public java.util.List<java.util.Map<String, Object>> previewSalaries(
            @RequestParam int year,
            @RequestParam int month) {
        var result = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (var user : userRepository.findAll()) {
            if (user.getRole() == null || "MEMBER".equals(user.getRole())) continue;
            if (!user.isActive()) continue;

            boolean already = salaryRepository
                    .findByUserIdAndYearAndMonth(user.getId(), year, month).isPresent();

            BigDecimal basic = EXECUTIVE_ROLES.contains(user.getRole())
                    ? EXECUTIVE_SALARY : STANDARD_SALARY;

            BigDecimal loanDeduction = BigDecimal.ZERO;
            String loanNote = null;
            if (user.getMember() != null) {
                for (var loan : loanRepository.findByMemberIdAndStatus(
                        user.getMember().getId(), "APPROVED")) {
                    if (loan.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal instalment = loan.getMonthlyInstalment()
                                .min(loan.getRemainingBalance());
                        loanDeduction = loanDeduction.add(instalment);
                    }
                }
                if (loanDeduction.compareTo(BigDecimal.ZERO) > 0) {
                    loanNote = "Monthly instalment deducted";
                }
            }

            BigDecimal net = basic.subtract(loanDeduction)
                    .max(BigDecimal.ZERO)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            var row = new java.util.LinkedHashMap<String, Object>();
            row.put("name", user.getFullName());
            row.put("role", user.getRole());
            row.put("executive", EXECUTIVE_ROLES.contains(user.getRole()));
            row.put("basicSalary", basic.setScale(2, java.math.RoundingMode.HALF_UP));
            row.put("loanDeduction", loanDeduction.setScale(2, java.math.RoundingMode.HALF_UP));
            row.put("netSalary", net);
            row.put("hasLoan", loanDeduction.compareTo(BigDecimal.ZERO) > 0);
            row.put("loanNote", loanNote);
            row.put("alreadyGenerated", already);
            result.add(row);
        }
        return result;
    }

    // ── Generate salary records for period ────────────────────────────────
    // Auto-assigns salary grade by role.
    // Auto-deducts loan instalments for staff who are also cooperative members.
    @PostMapping("/salaries/generate")
    public String generateSalaries(@RequestParam int year,
                                   @RequestParam int month,
                                   @AuthenticationPrincipal UserDetails ud,
                                   RedirectAttributes flash) {
        var officer = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        int count = 0;
        int withLoan = 0;

        for (var user : userRepository.findAll()) {
            if (user.getRole() == null || "MEMBER".equals(user.getRole())) continue;
            if (!user.isActive()) continue;
            // Idempotent — skip if already generated for this period
            if (salaryRepository.findByUserIdAndYearAndMonth(user.getId(), year, month).isPresent()) continue;

            // ── Step 1: Auto-assign basic salary by role ──────────────────
            BigDecimal basicSalary = EXECUTIVE_ROLES.contains(user.getRole())
                    ? EXECUTIVE_SALARY : STANDARD_SALARY;

            // ── Step 2: Loan deductions (only if staff is also a member) ──
            BigDecimal loanDeduction = BigDecimal.ZERO;
            String loanNote = null;
            if (user.getMember() != null) {
                var activeLoans = loanRepository.findByMemberIdAndStatus(
                        user.getMember().getId(), "APPROVED");
                for (var loan : activeLoans) {
                    if (loan.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal instalment = loan.getMonthlyInstalment()
                                .min(loan.getRemainingBalance());
                        loanDeduction = loanDeduction.add(instalment);
                    }
                }
                if (loanDeduction.compareTo(BigDecimal.ZERO) > 0) {
                    loanNote = "Loan deduction: ZMW " + loanDeduction.setScale(2, java.math.RoundingMode.HALF_UP);
                    withLoan++;
                }
            }

            // ── Step 3: Net salary = basic - loan deduction (min 0) ───────
            BigDecimal netSalary = basicSalary.subtract(loanDeduction)
                    .max(BigDecimal.ZERO)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            StaffSalary sal = StaffSalary.builder()
                    .id(java.util.UUID.randomUUID())
                    .user(user)
                    .year(year)
                    .month(month)
                    .basicSalary(basicSalary)
                    .deductions(loanDeduction)
                    .netSalary(netSalary)
                    .status("UNPAID")
                    .notes(loanNote)
                    .build();
            salaryRepository.save(sal);
            count++;
        }

        String msg = count + " salary record(s) generated for " + month + "/" + year
                + " | Executive (Admin): ZMW 8,000 · Standard staff: ZMW 7,000"
                + (withLoan > 0 ? " | " + withLoan + " staff member(s) have loan deductions applied." : " | No loan deductions this period.");
        flash.addFlashAttribute("successMessage", msg);
        return "redirect:/accountant/salaries?year=" + year + "&month=" + month;
    }

    // ── Generate milk payments for all members in a period ────────────────
    @PostMapping("/salaries/generate-member-payments")
    public String generateMemberPayments(@RequestParam int year,
                                         @RequestParam int month,
                                         @AuthenticationPrincipal UserDetails ud,
                                         RedirectAttributes flash) {
        var officer = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        int generated = 0, skipped = 0, failed = 0;
        var errors = new java.util.ArrayList<String>();

        var approvedMembers = memberRepository.findByStatusAndActiveTrue("APPROVED");
        for (var member : approvedMembers) {
            try {
                // Skip if already exists for this period
                if (paymentRepository.findByMemberIdAndYearAndMonth(
                        member.getId(), year, month).isPresent()) {
                    skipped++;
                    continue;
                }

                // Every approved member gets the fixed monthly salary (ZMW 3,000).
                // Deductions are loan repayments only — applied automatically.
                accountantService.applyDeductions(member.getId(), year, month, officer);
                accountantService.processPayment(member.getId(), year, month, officer);
                generated++;
            } catch (Exception e) {
                failed++;
                errors.add(member.getName() + ": " + e.getMessage());
            }
        }

        String msg = "✅ Generated " + generated + " milk payment record(s)";
        if (skipped > 0) msg += " · " + skipped + " already existed (skipped)";
        if (failed > 0) msg += " · " + failed + " failed";
        if (!errors.isEmpty()) {
            flash.addFlashAttribute("errorMessage",
                    "Some payments failed: " + String.join("; ", errors.subList(0, Math.min(errors.size(), 3))));
        }
        flash.addFlashAttribute("successMessage", msg);
        return "redirect:/accountant/salaries?year=" + year + "&month=" + month;
    }

    // ── Pay a member milk payment ──────────────────────────────────────────
    @PostMapping("/salaries/member-payments/{id}/pay")
    public String payMemberMilkPayment(@PathVariable UUID id,
                                       @RequestParam String method,
                                       @RequestParam(required = false) String notes,
                                       @AuthenticationPrincipal UserDetails ud,
                                       RedirectAttributes flash) {
        var officer = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            var payment = paymentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Payment record not found"));

            if ("PAID".equals(payment.getStatus()))
                throw new IllegalStateException("This payment is already marked as paid.");

            BigDecimal netAmount = payment.getNetAmount() != null
                    ? payment.getNetAmount() : BigDecimal.ZERO;

            // ── COFA balance check ─────────────────────────────────────────
            CooperativeFund fund = fundRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Cooperative Fund (COFA) has not been initialised."));

            if (!fund.hasSufficientFunds(netAmount)) {
                throw new IllegalStateException(
                        "Insufficient COFA balance. Current balance: ZMW " +
                                String.format("%,.2f", fund.getBalance()) +
                                " | Required: ZMW " + String.format("%,.2f", netAmount) +
                                ". Ensure company payments are received first.");
            }

            String reference = referenceNumberService.nextMilkPaymentRef();

            payment.setStatus("PAID");
            payment.setPaymentMethod(method);
            payment.setReferenceNo(reference);
            payment.setPaidAt(java.time.LocalDateTime.now());
            payment.setProcessedBy(officer);
            paymentRepository.save(payment);

            // ── Deduct from COFA and record transaction ────────────────────
            BigDecimal newBalance = fund.getBalance().subtract(netAmount);
            fund.setBalance(newBalance);
            fundRepository.save(fund);

            var tx = com.dairycoopms.dairycoopms.model.CooperativeFundTransaction.builder()
                    .transactionType(com.dairycoopms.dairycoopms.model.CooperativeFundTransaction.TransactionType.MEMBER_PAYMENT)
                    .amount(netAmount)
                    .balanceAfter(newBalance)
                    .referenceNo(reference)
                    .description("Milk Payment — " + payment.getMember().getName()
                            + " (" + payment.getMonth() + "/" + payment.getYear() + ")")
                    .performedBy(officer)
                    .build();
            transactionRepository.save(tx);

            flash.addFlashAttribute("successMessage",
                    "Payment of ZMW " + payment.getNetAmount() + " paid to " +
                            payment.getMember().getName() + " via " + method + " · Ref: " + reference);
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", "Failed to process payment: " + e.getMessage());
        }
        return "redirect:/accountant/salaries?year=" + java.time.LocalDate.now().getYear()
                + "&month=" + java.time.LocalDate.now().getMonthValue();
    }

    // ── Process staff salary payment ──────────────────────────────────────
    @PostMapping("/salaries/{id}/pay")
    public String paySalary(@PathVariable java.util.UUID id,
                            @RequestParam String method,
                            @RequestParam(required = false) String notes,
                            @AuthenticationPrincipal UserDetails ud,
                            RedirectAttributes flash) {
        var officer = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            var sal = salaryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Salary record not found"));

            if ("PAID".equals(sal.getStatus()))
                throw new IllegalStateException("This salary has already been paid.");
            if (!org.springframework.util.StringUtils.hasText(method))
                throw new IllegalArgumentException("Payment method is required.");

            String reference = referenceNumberService.nextSalaryRef();

            // ── COFA balance check ─────────────────────────────────────────
            BigDecimal netToPay = sal.getBasicSalary()
                    .subtract(sal.getDeductions() != null ? sal.getDeductions() : BigDecimal.ZERO)
                    .max(BigDecimal.ZERO)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            CooperativeFund fund = fundRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Cooperative Fund (COFA) has not been initialised. Contact the administrator."));

            if (!fund.hasSufficientFunds(netToPay)) {
                throw new IllegalStateException(
                        "Insufficient COFA balance. Current balance: ZMW " +
                                String.format("%,.2f", fund.getBalance()) +
                                " | Required: ZMW " + String.format("%,.2f", netToPay) +
                                ". Ensure company milk payments are received and credited to COFA first.");
            }
            BigDecimal correctNet = sal.getBasicSalary()
                    .subtract(sal.getDeductions() != null ? sal.getDeductions() : BigDecimal.ZERO)
                    .max(BigDecimal.ZERO);
            sal.setNetSalary(correctNet.setScale(2, java.math.RoundingMode.HALF_UP));

            // ── Mark paid ─────────────────────────────────────────────────
            sal.setStatus("PAID");
            sal.setPaidAt(LocalDateTime.now());
            sal.setProcessedBy(officer);
            sal.setReferenceNo(reference.trim());
            sal.setPaymentMethod(method);
            sal.setNotes(notes);
            sal.setSalaryNotificationRead(false);   // triggers portal banner
            salaryRepository.save(sal);

            // ── Deduct net salary from Cooperative Fund (COFA) ───────────
            BigDecimal salNewBalance = fund.getBalance()
                    .subtract(sal.getNetSalary())
                    .max(BigDecimal.ZERO);
            fund.setBalance(salNewBalance);
            fundRepository.save(fund);

            var salTx = com.dairycoopms.dairycoopms.model.CooperativeFundTransaction.builder()
                    .transactionType(com.dairycoopms.dairycoopms.model.CooperativeFundTransaction.TransactionType.SALARY_PAYMENT)
                    .amount(sal.getNetSalary())
                    .balanceAfter(salNewBalance)
                    .referenceNo(reference.trim())
                    .description("Salary — " + sal.getUser().getFullName()
                            + " (" + sal.getMonth() + "/" + sal.getYear() + ")")
                    .performedBy(officer)
                    .build();
            transactionRepository.save(salTx);

            // ── Human-readable method label for email ─────────────────────
            String methodLabel = switch (method) {
                case "BANK_TRANSFER" -> "Bank Transfer";
                case "AIRTEL_MONEY" -> "Airtel Money";
                case "MTN_MOBILE_MONEY" -> "MTN Mobile Money";
                case "CASH" -> "Cash";
                default -> method;
            };

            // ── Fire-and-forget email notification ────────────────────────
            emailService.sendSalaryPaymentNotification(sal, methodLabel);

            flash.addFlashAttribute("successMessage",
                    "✅ Salary paid to " + sal.getUser().getFullName() +
                            " — ZMW " + String.format("%,.2f", sal.getNetSalary()) +
                            " via " + methodLabel + " | Ref: " + reference);

        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", "❌ " + e.getMessage());
        }
        return "redirect:/accountant/salaries";
    }

    // ── Set staff salary ──────────────────────────────────────────────────
    @PostMapping("/salaries/set-salary")
    public String setStaffSalary(@RequestParam java.util.UUID userId,
                                 @RequestParam BigDecimal salary,
                                 @AuthenticationPrincipal UserDetails ud,
                                 RedirectAttributes flash) {
        try {
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            user.setBasicSalary(salary);
            userRepository.save(user);
            flash.addFlashAttribute("successMessage",
                    "Salary set for " + user.getFullName() + " — ZMW " + salary + "/month");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/accountant/salaries";
    }


    // ── Top up cooperative fund ───────────────────────────────────────────────
    @PostMapping("/fund/topup")
    public String topUpFund(@RequestParam java.math.BigDecimal amount,
                            @RequestParam(required = false) String notes,
                            @AuthenticationPrincipal UserDetails ud,
                            RedirectAttributes flash) {
        try {
            var officer = userRepository.findByEmail(ud.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Amount must be positive.");

            CooperativeFund fund = fundRepository.findById(1).orElseGet(CooperativeFund::new);
            BigDecimal newBalance = fund.getBalance().add(amount);
            fund.setBalance(newBalance);
            fund.setLastToppedUp(java.time.LocalDateTime.now());
            fund.setToppedUpBy(officer);
            fund.setNotes(notes);
            fundRepository.save(fund);

            // Log the transaction
            CooperativeFundTransaction transaction = CooperativeFundTransaction.builder()
                    .transactionType(CooperativeFundTransaction.TransactionType.MANUAL_TOPUP)
                    .amount(amount)
                    .balanceAfter(newBalance)
                    .referenceNo("TOPUP-" + java.time.LocalDate.now().toString())
                    .description(notes != null && !notes.isEmpty() ? notes : "Manual fund top-up")
                    .performedBy(officer)
                    .createdAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(transaction);

            flash.addFlashAttribute("successMessage",
                    "Fund topped up by ZMW " + amount + ". New balance: ZMW " + fund.getBalance());
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/accountant/disbursements";
    }

    private List<Member> getApprovedCoopMembers() {
        Set<String> staffEmails = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && !"MEMBER".equals(u.getRole()))
                .filter(u -> u.getEmail() != null)
                .map(u -> u.getEmail().toLowerCase())
                .collect(Collectors.toSet());

        return memberRepository.findByStatusAndActiveTrue("APPROVED").stream()
                .filter(m -> m.getEmail() == null ||
                        !staffEmails.contains(m.getEmail().toLowerCase()))
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MILK SALE PAYMENTS (Confirm Payment & Generate Receipt)
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/milk-payments")
    public String milkPayments(@RequestParam(required = false, defaultValue = "0") int year,
                               @RequestParam(required = false, defaultValue = "0") int month,
                               @AuthenticationPrincipal UserDetails ud, Model model) {
        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
            model.addAttribute("mustChangePassword", u.isMustChangePassword());
        });

        int y = (year == 0) ? java.time.LocalDate.now().getYear() : year;
        int m = (month == 0) ? java.time.LocalDate.now().getMonthValue() : month;
        model.addAttribute("selectedYear", y);
        model.addAttribute("selectedMonth", m);

        // Member milk payments for selected period
        var periodPayments = paymentRepository.findByYearAndMonth(y, m);
        model.addAttribute("periodPayments", periodPayments);

        // Stats
        long unpaidCount = periodPayments.stream().filter(p -> "UNPAID".equals(p.getStatus())).count();
        long paidCount = periodPayments.stream().filter(p -> "PAID".equals(p.getStatus())).count();
        BigDecimal totalPaid = periodPayments.stream()
                .filter(p -> "PAID".equals(p.getStatus()) && p.getNetAmount() != null)
                .map(com.dairycoopms.dairycoopms.model.Payment::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("unpaidCount", unpaidCount);
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("totalPaid", totalPaid);

        // COFA balance
        try {
            fundRepository.findAll().stream().findFirst().ifPresent(fund -> {
                model.addAttribute("cofaBalance", fund.getBalance());
                BigDecimal pendingOutflow = periodPayments.stream()
                        .filter(p -> "UNPAID".equals(p.getStatus()) && p.getNetAmount() != null)
                        .map(com.dairycoopms.dairycoopms.model.Payment::getNetAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                model.addAttribute("totalPendingOutflow", pendingOutflow);
                model.addAttribute("cofaSufficient", fund.hasSufficientFunds(pendingOutflow));
            });
        } catch (Exception ignored) {
        }

        // Processing-company invoices awaiting payment confirmation
        var invoicedInquiries = milkInquiryService.getInquiriesAwaitingPayment();
        model.addAttribute("invoicedInquiries", invoicedInquiries);
        model.addAttribute("invoicedCount", invoicedInquiries.size());
        BigDecimal invoicedTotal = invoicedInquiries.stream()
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("invoicedTotal", invoicedTotal);

        return "accountant/milk-payments";
    }

    @PostMapping("/milk-payments/{id}/confirm")
    public String confirmMilkPayment(@PathVariable UUID id,
                                     @AuthenticationPrincipal UserDetails ud,
                                     RedirectAttributes flash) {
        try {
            var accountant = userRepository.findByEmail(ud.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            MilkInquiry inquiry = milkInquiryService.confirmPayment(id, accountant.getId());

            flash.addFlashAttribute("successMessage",
                    String.format("Payment confirmed for %s! Receipt %s generated. COFA updated with ZMW %s",
                            inquiry.getReferenceNo(),
                            inquiry.getReceiptNo(),
                            inquiry.getTotalAmount()));

        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", "Failed to confirm payment: " + e.getMessage());
        }
        return "redirect:/accountant/milk-payments";
    }

    @GetMapping("/milk-payments/{id}/receipt")
    public String viewReceipt(@PathVariable UUID id,
                              @AuthenticationPrincipal UserDetails ud,
                              Model model,
                              RedirectAttributes flash) {
        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
        });

        return milkInquiryService.getInquiryById(id)
                .filter(MilkInquiry::isPaid)
                .map(inquiry -> {
                    model.addAttribute("inquiry", inquiry);
                    return "accountant/receipt";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("errorMessage", "Receipt not found or payment not yet confirmed");
                    return "redirect:/accountant/milk-payments";
                });
    }

    @GetMapping("/milk-payments/{id}/invoice")
    public String viewInvoice(@PathVariable UUID id,
                              @AuthenticationPrincipal UserDetails ud,
                              Model model,
                              RedirectAttributes flash) {
        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
        });

        return milkInquiryService.getInquiryById(id)
                .map(inquiry -> {
                    // Invoices are managed by Sales — redirect there
                    return "redirect:/sales/invoices/" + id + "/preview";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("errorMessage", "Invoice not found");
                    return "redirect:/accountant/milk-payments";
                });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COFA RECONCILIATION (Admin Tools)
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/cofa-reconciliation")
    public String cofaReconciliation(@AuthenticationPrincipal UserDetails ud, Model model) {
        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
            model.addAttribute("mustChangePassword", u.isMustChangePassword());
        });

        Map<String, Object> report = reconciliationService.getReconciliationReport();
        model.addAllAttributes(report);

        return "accountant/cofa-reconciliation";
    }

    @PostMapping("/cofa-reconciliation/add-missing")
    public String addMissingTransactions(@AuthenticationPrincipal UserDetails ud,
                                         RedirectAttributes flash) {
        try {
            Map<String, Object> result = reconciliationService.addMissingInquiryTransactions();
            flash.addFlashAttribute("successMessage", result.get("message"));
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", "Failed to add missing transactions: " + e.getMessage());
        }
        return "redirect:/accountant/cofa-reconciliation";
    }

    @PostMapping("/cofa-reconciliation/recalculate")
    public String recalculateBalance(@AuthenticationPrincipal UserDetails ud,
                                     RedirectAttributes flash) {
        try {
            Map<String, Object> result = reconciliationService.recalculateBalance();
            flash.addFlashAttribute("successMessage", result.get("message"));
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", "Failed to recalculate balance: " + e.getMessage());
        }
        return "redirect:/accountant/cofa-reconciliation";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FINANCIAL OVERVIEW PAGE
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/financial-overview")
    public String financialOverview(@AuthenticationPrincipal UserDetails ud, Model model) {
        // Get current user
        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
            model.addAttribute("mustChangePassword", u.isMustChangePassword());
        });

        // ── COFA Balance ──────────────────────────────────────────────────
        CooperativeFund cofa = fundRepository.findById(1).orElseGet(CooperativeFund::new);
        BigDecimal cofaBalance = cofa.getBalance();
        model.addAttribute("cofaBalance", cofaBalance);

        // ── Current Month & Period ────────────────────────────────────────
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        LocalDateTime monthStart = firstDayOfMonth.atStartOfDay();
        LocalDateTime monthEnd = lastDayOfMonth.atTime(23, 59, 59);

        // ════════════════════════════════════════════════════════════════════
        // MONTHLY SALARY EXPENDITURE (Staff + Members)
        // ════════════════════════════════════════════════════════════════════

        // ── STAFF SALARIES ────────────────────────────────────────────────
        List<StaffSalary> monthStaffSalaries = salaryRepository.findByYearAndMonth(today.getYear(), today.getMonthValue());
        List<Map<String, Object>> staffPaymentBreakdown = new ArrayList<>();
        BigDecimal totalStaffSalariesThisMonth = BigDecimal.ZERO;

        for (StaffSalary salary : monthStaffSalaries) {
            if ("PAID".equals(salary.getStatus())) {
                Map<String, Object> staffRow = new LinkedHashMap<>();
                staffRow.put("name", salary.getUser().getFullName());
                staffRow.put("role", salary.getUser().getRole());
                staffRow.put("salary", salary.getNetSalary());
                staffRow.put("status", salary.getStatus());
                staffPaymentBreakdown.add(staffRow);
                totalStaffSalariesThisMonth = totalStaffSalariesThisMonth.add(salary.getNetSalary());
            }
        }

        model.addAttribute("staffPaymentBreakdown", staffPaymentBreakdown);
        model.addAttribute("totalStaffSalariesThisMonth", totalStaffSalariesThisMonth);
        model.addAttribute("staffCount", staffPaymentBreakdown.size());

        // ── MEMBER MILK PAYMENTS ──────────────────────────────────────────
        List<Payment> monthMemberPayments = paymentRepository.findByYearAndMonth(today.getYear(), today.getMonthValue());
        List<Map<String, Object>> memberPaymentBreakdown = new ArrayList<>();
        BigDecimal totalMemberPaymentsThisMonth = BigDecimal.ZERO;

        for (Payment payment : monthMemberPayments) {
            if ("PAID".equals(payment.getStatus())) {
                Map<String, Object> memberRow = new LinkedHashMap<>();
                memberRow.put("memberNumber", payment.getMember().getMemberNumber());
                memberRow.put("name", payment.getMember().getName());
                memberRow.put("grossAmount", payment.getGrossAmount());
                memberRow.put("deductions", payment.getTotalDeductions());
                memberRow.put("netAmount", payment.getNetAmount());
                memberRow.put("status", payment.getStatus());
                memberPaymentBreakdown.add(memberRow);
                totalMemberPaymentsThisMonth = totalMemberPaymentsThisMonth.add(payment.getNetAmount());
            }
        }

        model.addAttribute("memberPaymentBreakdown", memberPaymentBreakdown);
        model.addAttribute("totalMemberPaymentsThisMonth", totalMemberPaymentsThisMonth);
        model.addAttribute("memberCount", memberPaymentBreakdown.size());

        // ── GRAND TOTAL MONTHLY EXPENDITURE ───────────────────────────────
        BigDecimal monthlyExpenditureTotal = totalStaffSalariesThisMonth.add(totalMemberPaymentsThisMonth);
        model.addAttribute("monthlyExpenditureTotal", monthlyExpenditureTotal);

        // ── ALL-TIME TOTALS ───────────────────────────────────────────────
        BigDecimal totalStaffSalariesAllTime = salaryRepository.findAll().stream()
                .filter(s -> "PAID".equals(s.getStatus()))
                .map(StaffSalary::getNetSalary)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalStaffSalariesAllTime", totalStaffSalariesAllTime);

        BigDecimal totalMemberPaymentsAllTime = paymentRepository.findAll().stream()
                .filter(p -> "PAID".equals(p.getStatus()))
                .map(Payment::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalMemberPaymentsAllTime", totalMemberPaymentsAllTime);

        // ── Loan Disbursements (This Month) ───────────────────────────────
        List<Loan> monthDisbursedLoans = loanRepository.findAll().stream()
                .filter(l -> "DISBURSED".equals(l.getDisbursementStatus()))
                .filter(l -> l.getDisbursedAt() != null &&
                        l.getDisbursedAt().isAfter(monthStart) &&
                        l.getDisbursedAt().isBefore(monthEnd))
                .toList();
        BigDecimal totalLoanDisbursementsThisMonth = monthDisbursedLoans.stream()
                .map(Loan::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalLoanDisbursementsThisMonth", totalLoanDisbursementsThisMonth);

        // ── Loan Disbursements (All Time) ─────────────────────────────────
        BigDecimal totalLoanDisbursementsAllTime = loanRepository.findAll().stream()
                .filter(l -> "DISBURSED".equals(l.getDisbursementStatus()))
                .map(Loan::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalLoanDisbursementsAllTime", totalLoanDisbursementsAllTime);

        // ── Transaction Summary ───────────────────────────────────────────
        List<CooperativeFundTransaction> recentTransactions = transactionRepository.findTop20ByOrderByCreatedAtDesc();
        model.addAttribute("recentTransactions", recentTransactions);

        // ── COFA Income Breakdown (This Month) ────────────────────────────
        BigDecimal milkSalesIncome = transactionRepository.sumByTypeAndPeriod(
                CooperativeFundTransaction.TransactionType.MILK_SALE, monthStart, monthEnd);
        BigDecimal manualTopups = transactionRepository.sumByTypeAndPeriod(
                CooperativeFundTransaction.TransactionType.MANUAL_TOPUP, monthStart, monthEnd);
        BigDecimal loanRepayments = transactionRepository.sumByTypeAndPeriod(
                CooperativeFundTransaction.TransactionType.LOAN_REPAYMENT, monthStart, monthEnd);

        BigDecimal totalIncomeThisMonth = (milkSalesIncome != null ? milkSalesIncome : BigDecimal.ZERO)
                .add(manualTopups != null ? manualTopups : BigDecimal.ZERO)
                .add(loanRepayments != null ? loanRepayments : BigDecimal.ZERO);

        model.addAttribute("milkSalesIncome", milkSalesIncome != null ? milkSalesIncome : BigDecimal.ZERO);
        model.addAttribute("manualTopups", manualTopups != null ? manualTopups : BigDecimal.ZERO);
        model.addAttribute("loanRepayments", loanRepayments != null ? loanRepayments : BigDecimal.ZERO);
        model.addAttribute("totalIncomeThisMonth", totalIncomeThisMonth);

        model.addAttribute("currentMonthYear", today.getMonth().toString() + " " + today.getYear());

        // ════════════════════════════════════════════════════════════════════
        // COW MAINTENANCE FUND (Only One Fund Type)
        // ════════════════════════════════════════════════════════════════════

        Map<Integer, BigDecimal> currentAllocations = new HashMap<>();
        BigDecimal totalCurrentAllocations = BigDecimal.ZERO;
        List<?> deductionTypes = new ArrayList<>();

        // Cow Maintenance = 10% of COFA balance
        try {
            deductionTypes = accountantService.getAllDeductionTypes();

            if (deductionTypes != null && !deductionTypes.isEmpty()) {
                for (Object dtObj : deductionTypes) {
                    try {
                        Integer typeId = (Integer) dtObj.getClass().getMethod("getId").invoke(dtObj);
                        String typeName = (String) dtObj.getClass().getMethod("getName").invoke(dtObj);
                        BigDecimal rate = (BigDecimal) dtObj.getClass().getMethod("getRate").invoke(dtObj);
                        Boolean isActive = (Boolean) dtObj.getClass().getMethod("isActive").invoke(dtObj);

                        // Only include Cow Maintenance Fund
                        if (isActive != null && isActive && rate != null &&
                                typeName != null && typeName.equalsIgnoreCase("Cow Maintenance")) {
                            BigDecimal allocation = cofaBalance
                                    .multiply(rate)
                                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                            currentAllocations.put(typeId, allocation);
                            totalCurrentAllocations = totalCurrentAllocations.add(allocation);
                        }
                    } catch (Exception e) {
                        System.out.println("Error processing deduction type: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error fetching deduction types: " + e.getMessage());
        }

        model.addAttribute("currentAllocations", currentAllocations);
        model.addAttribute("totalCurrentAllocations", totalCurrentAllocations);

        return "accountant/financial-overview";
    }

    @PostMapping(value = "/milk-payments/{id}/confirm", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> confirmMilkPaymentAjax(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails ud) {
        java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
        try {
            var accountant = userRepository.findByEmail(ud.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            MilkInquiry inquiry = milkInquiryService.confirmPayment(id, accountant.getId());
            r.put("success", true);
            r.put("message", String.format("Payment confirmed for %s! Receipt %s generated. COFA credited ZMW %s.",
                    inquiry.getReferenceNo(), inquiry.getReceiptNo(), inquiry.getTotalAmount()));
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", "Failed to confirm payment: " + e.getMessage());
        }
        return ResponseEntity.ok(r);
    }
}