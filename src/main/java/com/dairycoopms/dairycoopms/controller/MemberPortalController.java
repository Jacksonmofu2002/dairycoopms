package com.dairycoopms.dairycoopms.controller;

import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.LoanRepository;
import com.dairycoopms.dairycoopms.service.CreditScoringService;
import com.dairycoopms.dairycoopms.repository.MilkPriceRepository;
import com.dairycoopms.dairycoopms.repository.MilkDeliveryRepository;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import com.dairycoopms.dairycoopms.repository.StaffSalaryRepository;
import com.dairycoopms.dairycoopms.repository.CowAssignmentRepository;
import com.dairycoopms.dairycoopms.repository.PaymentRepository;
import com.dairycoopms.dairycoopms.repository.DeductionRepository;
import com.dairycoopms.dairycoopms.repository.LoanRepaymentRepository;
import com.dairycoopms.dairycoopms.service.MemberSelfRegisterService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/member")
public class MemberPortalController {

    private final MemberSelfRegisterService selfRegisterService;
    private final UserRepository            userRepository;
    private final LoanRepository            loanRepository;
    private final MilkDeliveryRepository    milkDeliveryRepository;
    private final MilkPriceRepository       milkPriceRepository;
    private final CreditScoringService       creditScoringService;
    private final StaffSalaryRepository     staffSalaryRepository;
    private final CowAssignmentRepository    cowAssignmentRepository;
    private final PaymentRepository          paymentRepository;
    private final DeductionRepository        deductionRepository;
    private final LoanRepaymentRepository    loanRepaymentRepository;

    public MemberPortalController(MemberSelfRegisterService selfRegisterService,
                                  UserRepository userRepository,
                                  LoanRepository loanRepository,
                                  CreditScoringService creditScoringService,
                                  MilkDeliveryRepository milkDeliveryRepository,
                                  MilkPriceRepository milkPriceRepository,
                                  StaffSalaryRepository staffSalaryRepository,
                                  CowAssignmentRepository cowAssignmentRepository,
                                  PaymentRepository paymentRepository,
                                  DeductionRepository deductionRepository,
                                  LoanRepaymentRepository loanRepaymentRepository) {
        this.selfRegisterService   = selfRegisterService;
        this.userRepository        = userRepository;
        this.loanRepository        = loanRepository;
        this.milkDeliveryRepository = milkDeliveryRepository;
        this.milkPriceRepository   = milkPriceRepository;
        this.creditScoringService  = creditScoringService;
        this.staffSalaryRepository = staffSalaryRepository;
        this.cowAssignmentRepository = cowAssignmentRepository;
        this.paymentRepository     = paymentRepository;
        this.deductionRepository   = deductionRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
    }

    // ── helpers ───────────────────────────────────────────────────────────
    private User currentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void addCommon(Model model, User user) {
        model.addAttribute("user", user);
        boolean hasUnread = user.getMember() != null
                && !user.getMember().isNotificationRead();
        if (hasUnread) selfRegisterService.markNotificationRead(user);
        model.addAttribute("hasNewNotification", hasUnread);
        model.addAttribute("member", user.getMember());
        model.addAttribute("showProfileModal", user.getMember() == null);
        model.addAttribute("canResubmit",
                user.getMember() != null &&
                        "REJECTED".equals(user.getMember().getStatus()));

        // ── Salary payment notification ────────────────────────────────
        // Show banner for the most recent PAID salary not yet acknowledged
        var unreadSalaries = staffSalaryRepository
                .findByUserIdAndStatusAndSalaryNotificationReadFalseOrderByPaidAtDesc(
                        user.getId(), "PAID");
        if (!unreadSalaries.isEmpty()) {
            var latest = unreadSalaries.get(0);
            model.addAttribute("newSalaryNotification", latest);
            // Mark ALL unread as read so they don't stack up
            unreadSalaries.forEach(s -> s.setSalaryNotificationRead(true));
            staffSalaryRepository.saveAll(unreadSalaries);
        }
    }

    // ── Public ────────────────────────────────────────────────────────────
    @GetMapping("/register")
    public String showRegister() { return "member/register"; }

    @PostMapping("/register")
    public String createAccount(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                Model model) {
        try {
            selfRegisterService.createAccount(fullName, email, password, confirmPassword);
            return "redirect:/member/register/success";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "member/register";
        }
    }

    @GetMapping("/register/success")
    public String registerSuccess() { return "member/register-success"; }

    // ── Dashboard ─────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = currentUser(ud);
        addCommon(model, user);

        if (user.getMember() != null) {
            UUID mid = user.getMember().getId();
            LocalDate now = LocalDate.now();

            var allLoans = loanRepository.findByMemberId(mid);
            model.addAttribute("myLoans", allLoans);

            // Count active loans (DISBURSED only - money actually received)
            long activeCount = allLoans.stream()
                    .filter(l -> "APPROVED".equals(l.getStatus()) && "DISBURSED".equals(l.getDisbursementStatus()))
                    .count();
            model.addAttribute("activeLoans", activeCount);

            // Count pending loans (PENDING status - awaiting approval)
            long pendingCount = allLoans.stream()
                    .filter(l -> "PENDING".equals(l.getStatus()))
                    .count();
            model.addAttribute("pendingLoans", pendingCount);

            // ══════════════════════════════════════════════════════════════
            // DISBURSEMENT GATE: Only show loan details AFTER money is paid
            // ══════════════════════════════════════════════════════════════

            // Active loan for dashboard card - ONLY if DISBURSED (money received)
            allLoans.stream()
                    .filter(l -> "APPROVED".equals(l.getStatus()) && "DISBURSED".equals(l.getDisbursementStatus()))
                    .findFirst()
                    .ifPresent(loan -> model.addAttribute("activeLoan", loan));

            // Loan awaiting disbursement - APPROVED but not yet paid out
            allLoans.stream()
                    .filter(l -> "APPROVED".equals(l.getStatus()) && !"DISBURSED".equals(l.getDisbursementStatus()))
                    .findFirst()
                    .ifPresent(loan -> model.addAttribute("awaitingDisbursement", loan));

            // This month deliveries
            var monthDeliveries = milkDeliveryRepository.findByMemberIdAndYearAndMonth(
                    mid, now.getYear(), now.getMonthValue());
            model.addAttribute("thisMonthDeliveries", monthDeliveries);

            // Month totals
            java.math.BigDecimal monthLitres = monthDeliveries.stream()
                    .map(d -> d.getQuantityLitres())
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            model.addAttribute("monthLitres", monthLitres);
            model.addAttribute("monthDeliveryCount", monthDeliveries.size());

            // Current milk price
            milkPriceRepository.findByActiveTrue()
                    .ifPresent(p -> model.addAttribute("currentPrice", p.getPricePerLitre()));

            // Real stats (also update to DISBURSED only)
            model.addAttribute("activeLoansCount", activeCount);
        }
        return "member/dashboard";
    }

    // ── Profile submit ────────────────────────────────────────────────────
    @PostMapping("/profile/submit")
    public String submitProfile(@AuthenticationPrincipal UserDetails ud,
                                @RequestParam String nrc,
                                @RequestParam String phone,
                                @RequestParam String address,
                                RedirectAttributes flash) {
        User user = currentUser(ud);
        try {
            selfRegisterService.submitProfile(user, nrc, phone, address);
            flash.addFlashAttribute("successMessage",
                    "Profile submitted! Pending admin approval.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("profileError", e.getMessage());
            flash.addFlashAttribute("showProfileModal", true);
        }
        return "redirect:/member/dashboard";
    }

    // ── Loans ─────────────────────────────────────────────────────────────
    @GetMapping("/loans")
    public String loans(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = currentUser(ud);
        addCommon(model, user);
        if (user.getMember() != null) {
            var loans = loanRepository.findByMemberId(user.getMember().getId());
            model.addAttribute("loans", loans);

            // For each loan, attach actual repayment history so the amortization
            // schedule can show which instalments have already been paid.
            var repaymentsByLoanId = new java.util.HashMap<String,
                    java.util.List<com.dairycoopms.dairycoopms.model.LoanRepayment>>();
            for (var loan : loans) {
                var repayments = loanRepaymentRepository.findByLoanId(loan.getId())
                        .stream()
                        .sorted(java.util.Comparator
                                .comparingInt(com.dairycoopms.dairycoopms.model.LoanRepayment::getPeriodYear)
                                .thenComparingInt(com.dairycoopms.dairycoopms.model.LoanRepayment::getPeriodMonth))
                        .toList();
                repaymentsByLoanId.put(loan.getId().toString(), repayments);
            }
            model.addAttribute("repaymentsByLoanId", repaymentsByLoanId);

            // Month names needed by the amortization schedule
            model.addAttribute("monthNames", java.util.Map.ofEntries(
                    java.util.Map.entry(1,"January"),  java.util.Map.entry(2,"February"),
                    java.util.Map.entry(3,"March"),    java.util.Map.entry(4,"April"),
                    java.util.Map.entry(5,"May"),      java.util.Map.entry(6,"June"),
                    java.util.Map.entry(7,"July"),     java.util.Map.entry(8,"August"),
                    java.util.Map.entry(9,"September"),java.util.Map.entry(10,"October"),
                    java.util.Map.entry(11,"November"),java.util.Map.entry(12,"December")));
        }
        return "member/loans";
    }

    // ── Salary &amp; Payment History ─────────────────────────────────────────────
    @GetMapping("/salary-history")
    public String salaryHistory(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = currentUser(ud);
        addCommon(model, user);

        if (user.getMember() != null) {
            java.util.UUID memberId = user.getMember().getId();

            // All milk payments for this member, newest first
            var payments = paymentRepository.findByMemberId(memberId)
                    .stream()
                    .sorted(java.util.Comparator
                            .comparingInt(com.dairycoopms.dairycoopms.model.Payment::getYear)
                            .thenComparingInt(com.dairycoopms.dairycoopms.model.Payment::getMonth)
                            .reversed())
                    .toList();
            model.addAttribute("payments", payments);

            // Month names for display
            model.addAttribute("monthNames", java.util.Map.ofEntries(
                    java.util.Map.entry(1,"January"),  java.util.Map.entry(2,"February"),
                    java.util.Map.entry(3,"March"),    java.util.Map.entry(4,"April"),
                    java.util.Map.entry(5,"May"),      java.util.Map.entry(6,"June"),
                    java.util.Map.entry(7,"July"),     java.util.Map.entry(8,"August"),
                    java.util.Map.entry(9,"September"),java.util.Map.entry(10,"October"),
                    java.util.Map.entry(11,"November"),java.util.Map.entry(12,"December")));

            // Build deduction breakdown per payment
            // Map<paymentId, {stdTotal, loanDeds, loanTotal}>
            var deductionMap = new java.util.HashMap<String, java.util.Map<String, Object>>();
            for (var pmt : payments) {
                var deductions = deductionRepository
                        .findByMemberIdAndYearAndMonth(memberId, pmt.getYear(), pmt.getMonth());
                var loanDeds = deductions.stream()
                        .filter(d -> d.getLoan() != null).toList();
                var stdTotal = deductions.stream()
                        .filter(d -> d.getLoan() == null)
                        .map(com.dairycoopms.dairycoopms.model.Deduction::getAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                var info = new java.util.HashMap<String, Object>();
                info.put("loanDeds", loanDeds);
                info.put("stdTotal", stdTotal);
                deductionMap.put(pmt.getId().toString(), info);
            }
            model.addAttribute("deductionMap", deductionMap);

            // Summary totals
            java.math.BigDecimal totalGross = payments.stream()
                    .map(p -> p.getGrossAmount() != null ? p.getGrossAmount() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal totalNet = payments.stream()
                    .filter(p -> "PAID".equals(p.getStatus()))
                    .map(p -> p.getNetAmount() != null ? p.getNetAmount() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal totalDed = payments.stream()
                    .map(p -> p.getTotalDeductions() != null ? p.getTotalDeductions() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal totalLoanRepaid = loanRepaymentRepository.findByMemberId(memberId)
                    .stream()
                    .map(lr -> lr.getRepaymentAmount() != null ? lr.getRepaymentAmount() : java.math.BigDecimal.ZERO)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            model.addAttribute("totalGross",      totalGross);
            model.addAttribute("totalNet",         totalNet);
            model.addAttribute("totalDeductions",  totalDed);
            model.addAttribute("totalLoanRepaid",  totalLoanRepaid);
        }
        return "member/salary-history";
    }

    @GetMapping("/loans/apply")
    public String applyLoanForm(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = currentUser(ud);
        addCommon(model, user);
        return "member/loan-apply";
    }

    @PostMapping("/loans/apply")
    public String applyLoan(@AuthenticationPrincipal UserDetails ud,
                            @RequestParam BigDecimal amount,
                            @RequestParam int repaymentMonths,
                            @RequestParam String purpose,
                            RedirectAttributes flash) {
        User user = currentUser(ud);
        try {
            if (user.getMember() == null ||
                    !"APPROVED".equals(user.getMember().getStatus()))
                throw new IllegalStateException(
                        "Your membership must be approved before applying for a loan.");

            if (amount.compareTo(new java.math.BigDecimal("500")) < 0)
                throw new IllegalArgumentException(
                        "Minimum loan amount is ZMW 500.");

            // ── Run embedded credit scoring ────────────────────────────
            CreditScoringService.ScoreResult scoreResult =
                    creditScoringService.score(user.getMember().getId(), amount);

            // Calculate interest rate based on amount tier
            java.math.BigDecimal interestRate = calculateInterestRate(amount);
            java.math.BigDecimal interestAmount = amount.multiply(interestRate)
                    .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal totalRepayable = amount.add(interestAmount);
            java.math.BigDecimal monthly = totalRepayable.divide(
                    java.math.BigDecimal.valueOf(repaymentMonths), 2, java.math.RoundingMode.HALF_UP);

            // Loan status driven by score
            String status = scoreResult.isApproved() ? "APPROVED" : "REJECTED";

            com.dairycoopms.dairycoopms.model.Loan loan =
                    com.dairycoopms.dairycoopms.model.Loan.builder()
                            .id(java.util.UUID.randomUUID())
                            .member(user.getMember())
                            .amount(amount)
                            .remainingBalance(scoreResult.isApproved() ? totalRepayable : amount)
                            .repaymentMonths(repaymentMonths)
                            .monthlyInstalment(monthly)
                            .interestRate(interestRate)
                            .purpose(purpose)
                            .status(status)
                            .requestDate(LocalDate.now())
                            // Score fields
                            .creditScore(scoreResult.getScore())
                            .scoreDecision(scoreResult.getDecision())
                            .scoreReason(scoreResult.getReasonFull())
                            .scoreQualifyingDays(scoreResult.getQualifyingDays())
                            .scoreTotalLitres(scoreResult.getTotalLitres())
                            // If approved, set dates
                            .approvedDate(scoreResult.isApproved() ? LocalDate.now() : null)
                            .startDate(scoreResult.isApproved() ?
                                    LocalDate.now().withDayOfMonth(1).plusMonths(1) : null)
                            .maturityDate(scoreResult.isApproved() ?
                                    LocalDate.now().withDayOfMonth(1).plusMonths(1).plusMonths(repaymentMonths) : null)
                            .disbursementStatus(scoreResult.isApproved() ? "PENDING_DISBURSEMENT" : null)
                            .rejectionReason(scoreResult.isApproved() ? null : scoreResult.getReasonFull())
                            .build();

            loanRepository.save(loan);

            if (scoreResult.isApproved()) {
                flash.addFlashAttribute("successMessage",
                        "Congratulations! " + scoreResult.getReasonShort());
            } else {
                flash.addFlashAttribute("loanError",
                        scoreResult.getReasonShort());
            }

        } catch (Exception e) {
            flash.addFlashAttribute("loanError", e.getMessage());
        }
        return "redirect:/member/loans";
    }

    /** Interest rate tiers matching LoanService */
    private java.math.BigDecimal calculateInterestRate(java.math.BigDecimal amount) {
        double a = amount.doubleValue();
        double rate;
        if (a < 500)        rate = 0;
        else if (a <= 999)  rate = 10;
        else if (a <= 1500) rate = 15;
        else if (a <= 2000) rate = 20;
        else {
            int extra = (int)((a - 2000) / 1000);
            rate = 20 + (extra + 1) * 5;
        }
        return java.math.BigDecimal.valueOf(rate);
    }

    // ── Deliveries (read-only) ────────────────────────────────────────────
    @GetMapping("/deliveries")
    public String deliveries(@AuthenticationPrincipal UserDetails ud,
                             @RequestParam(required = false) Integer year,
                             @RequestParam(required = false) Integer month,
                             Model model) {
        User user = currentUser(ud);
        addCommon(model, user);
        if (user.getMember() != null) {
            LocalDate now = LocalDate.now();
            int y = year  != null ? year  : now.getYear();
            int m = month != null ? month : now.getMonthValue();
            model.addAttribute("deliveries",
                    milkDeliveryRepository.findByMemberIdAndYearAndMonth(
                            user.getMember().getId(), y, m));
            model.addAttribute("selectedYear",  y);
            model.addAttribute("selectedMonth", m);

            // Today's deliveries for confirmation section
            var todayList = milkDeliveryRepository.findByDeliveryDate(now).stream()
                    .filter(d -> d.getMember().getId().equals(user.getMember().getId()))
                    .toList();
            model.addAttribute("todayDeliveries", todayList);

            // Pre-compute period totals to avoid complex SpEL in template
            var allDeliveries = milkDeliveryRepository.findByMemberIdAndYearAndMonth(user.getMember().getId(), y, m);
            java.math.BigDecimal totalLitres = allDeliveries.stream()
                    .map(d -> d.getQuantityLitres())
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal totalValue = allDeliveries.stream()
                    .map(d -> d.getQuantityLitres().multiply(d.getPricePerLitreAtDelivery()))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            model.addAttribute("periodTotalLitres", totalLitres);
            model.addAttribute("periodTotalValue",  totalValue);
            // Current price for member to see
            milkPriceRepository.findByActiveTrue()
                    .ifPresent(p -> model.addAttribute("currentPrice", p.getPricePerLitre()));
        }
        return "member/deliveries";
    }

    // ── Confirm delivery ──────────────────────────────────────────────────
    @PostMapping("/deliveries/{id}/confirm")
    public String confirmDelivery(@AuthenticationPrincipal UserDetails ud,
                                  @PathVariable UUID id,
                                  RedirectAttributes flash) {
        User user = currentUser(ud);
        try {
            var delivery = milkDeliveryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Delivery not found"));

            // Ensure this delivery belongs to this member
            if (user.getMember() == null ||
                    !delivery.getMember().getId().equals(user.getMember().getId()))
                throw new IllegalStateException("You cannot confirm this delivery.");

            if (delivery.isConfirmedByMember())
                throw new IllegalStateException("Already confirmed.");

            delivery.setConfirmedByMember(true);
            delivery.setConfirmedAt(java.time.LocalDateTime.now());
            milkDeliveryRepository.save(delivery);
            flash.addFlashAttribute("successMessage", "Delivery confirmed successfully.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/member/deliveries";
    }

    // ── Save bank account details ─────────────────────────────────────────────
    @PostMapping("/bank-account")
    public String saveBankAccount(@AuthenticationPrincipal UserDetails ud,
                                  @RequestParam String bankName,
                                  @RequestParam String accountName,
                                  @RequestParam String accountNumber,
                                  @RequestParam(required = false) String branchName,
                                  RedirectAttributes flash) {
        User user = currentUser(ud);
        try {
            if (user.getMember() == null)
                throw new IllegalStateException("No member profile found.");
            var member = user.getMember();
            member.setBankName(bankName);
            member.setAccountName(accountName);
            member.setAccountNumber(accountNumber);
            member.setBranchName(branchName);
            // Save via MemberSelfRegisterService or directly via memberRepository
            selfRegisterService.saveBankDetails(member);
            flash.addFlashAttribute("successMessage", "Bank account details saved successfully.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/member/payments";
    }

    // ── Payments (read-only) ──────────────────────────────────────────────────
    @GetMapping("/payments")
    public String payments(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = currentUser(ud);
        addCommon(model, user);
        if (user.getMember() != null) {
            var allLoans = loanRepository.findByMemberId(user.getMember().getId());

            // Get only disbursed loans (money received)
            var disbursedLoans = allLoans.stream()
                    .filter(l -> "DISBURSED".equals(l.getDisbursementStatus()))
                    .toList();

            // Get loans awaiting payment (approved but not disbursed)
            var awaitingPayment = allLoans.stream()
                    .filter(l -> "APPROVED".equals(l.getStatus()) &&
                            !"DISBURSED".equals(l.getDisbursementStatus()))
                    .toList();

            model.addAttribute("disbursedLoans", disbursedLoans);
            model.addAttribute("awaitingPayment", awaitingPayment);
            model.addAttribute("loans", allLoans);
        }
        return "member/payments";
    }

    // ── My Cows (read-only) ───────────────────────────────────────────────────
    /**
     * Display all cows assigned to the logged-in member.
     * Shows full cow details: tag, name, breed, gender, purchase date, status, portrait, etc.
     * Only active assignments are shown by default.
     *
     * @Transactional ensures lazy-loaded Cow entities are accessible in the view template.
     */
    @GetMapping("/cows")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String myCows(@AuthenticationPrincipal UserDetails ud,
                         @RequestParam(required = false, defaultValue = "false") boolean showAll,
                         Model model) {
        User user = currentUser(ud);
        addCommon(model, user);

        if (user.getMember() != null) {
            UUID memberId = user.getMember().getId();

            // Fetch cow assignments with cow details eagerly loaded (optimized query)
            var allAssignments = cowAssignmentRepository.findByMemberIdWithCowDetails(memberId);

            // Preprocess portraits: add data URL prefix to avoid Thymeleaf concatenation limit
            allAssignments.forEach(assignment -> {
                if (assignment.getCow() != null && assignment.getCow().getPortrait() != null
                        && !assignment.getCow().getPortrait().isEmpty()) {
                    String portrait = assignment.getCow().getPortrait();
                    // Only add prefix if not already present
                    if (!portrait.startsWith("data:image/")) {
                        // Detect format: JPEG starts with /9j/, PNG starts with iVBOR
                        String prefix = portrait.startsWith("/9j/") ?
                                "data:image/jpeg;base64," : "data:image/png;base64,";
                        assignment.getCow().setPortrait(prefix + portrait);
                    }
                }
            });

            // Filter to only active assignments unless showAll is true
            var assignments = showAll ? allAssignments :
                    allAssignments.stream()
                            .filter(a -> a.isActive())
                            .toList();

            // Count active vs total
            long activeCount = allAssignments.stream().filter(a -> a.isActive()).count();
            long totalCount = allAssignments.size();

            model.addAttribute("cowAssignments", assignments);
            model.addAttribute("activeCount", activeCount);
            model.addAttribute("totalCount", totalCount);
            model.addAttribute("showAll", showAll);
        }

        return "member/cows";
    }
}