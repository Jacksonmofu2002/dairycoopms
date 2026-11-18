package com.dairycoopms.dairycoopms.controller.accountant;

import com.dairycoopms.dairycoopms.model.MilkInquiry;
import com.dairycoopms.dairycoopms.model.ReportSubmission;
import com.dairycoopms.dairycoopms.repository.*;
import com.dairycoopms.dairycoopms.model.AuditLog;
import com.dairycoopms.dairycoopms.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/accountant/reports")
@PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
public class AccountantReportController {

    private final UserRepository           userRepository;
    private final LoanRepository           loanRepository;
    private final StaffSalaryRepository    salaryRepository;
    private final PaymentRepository        paymentRepository;
    private final MilkInquiryRepository    milkInquiryRepository;
    private final MemberRepository         memberRepository;
    private final AuditLogRepository       auditLogRepository;
    private final CooperativeFundRepository fundRepository;
    private final EmailService             emailService;
    private final ReportSubmissionRepository reportSubmissionRepository;
    private final LoanRepaymentRepository  loanRepaymentRepository;
    private final CooperativeFundTransactionRepository fundTransactionRepository;
    private final MilkDeliveryRepository   milkDeliveryRepository;
    private final ObjectMapper             objectMapper = new ObjectMapper();

    public AccountantReportController(UserRepository userRepository,
                                      LoanRepository loanRepository,
                                      StaffSalaryRepository salaryRepository,
                                      PaymentRepository paymentRepository,
                                      MilkInquiryRepository milkInquiryRepository,
                                      MemberRepository memberRepository,
                                      AuditLogRepository auditLogRepository,
                                      CooperativeFundRepository fundRepository,
                                      EmailService emailService,
                                      ReportSubmissionRepository reportSubmissionRepository,
                                      LoanRepaymentRepository loanRepaymentRepository,
                                      CooperativeFundTransactionRepository fundTransactionRepository,
                                      MilkDeliveryRepository milkDeliveryRepository) {
        this.userRepository       = userRepository;
        this.loanRepository       = loanRepository;
        this.salaryRepository     = salaryRepository;
        this.paymentRepository    = paymentRepository;
        this.milkInquiryRepository = milkInquiryRepository;
        this.memberRepository     = memberRepository;
        this.auditLogRepository   = auditLogRepository;
        this.fundRepository       = fundRepository;
        this.emailService         = emailService;
        this.reportSubmissionRepository = reportSubmissionRepository;
        this.loanRepaymentRepository    = loanRepaymentRepository;
        this.fundTransactionRepository  = fundTransactionRepository;
        this.milkDeliveryRepository     = milkDeliveryRepository;
    }

    // ── Reports page ──────────────────────────────────────────────────────────
    @GetMapping
    public String reportsPage(@AuthenticationPrincipal UserDetails ud, Model model) {
        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
            model.addAttribute("mustChangePassword", u.isMustChangePassword());
        });
        return "accountant/reports";
    }

    // ── Clean PDF report view ─────────────────────────────────────────────────
    @GetMapping("/pdf")
    public String pdfReport(@AuthenticationPrincipal UserDetails ud, Model model) {
        var officerName = new String[]{"Finance Officer"};
        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
            officerName[0] = u.getFullName();
        });

        var fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        model.addAttribute("reportDate", LocalDateTime.now().format(fmt));
        model.addAttribute("officer",   officerName[0]);

        // Build the same data as generateReport() but as model attributes
        populateReportModel(model);

        return "accountant/report-pdf";
    }

    /** Shared data-gathering logic used by both the generate API and the PDF view */
    private void populateReportModel(Model model) {
        var fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

        // ── Loans ─────────────────────────────────────────────────────────────
        var allLoans  = loanRepository.findAll();
        var loans     = new LinkedHashMap<String, Object>();
        loans.put("total",                allLoans.size());
        loans.put("pending",              allLoans.stream().filter(l -> "PENDING".equals(l.getStatus())).count());
        loans.put("approved",             allLoans.stream().filter(l -> "APPROVED".equals(l.getStatus())).count());
        loans.put("rejected",             allLoans.stream().filter(l -> "REJECTED".equals(l.getStatus())).count());
        loans.put("disbursed",            allLoans.stream().filter(l -> "DISBURSED".equals(l.getDisbursementStatus())).count());
        loans.put("totalAmountApproved",  fmt(allLoans.stream()
                .filter(l -> "APPROVED".equals(l.getStatus()))
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        loans.put("totalAmountDisbursed", fmt(allLoans.stream()
                .filter(l -> "DISBURSED".equals(l.getDisbursementStatus()))
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        loans.put("recent", allLoans.stream()
                .filter(l -> l.getRequestDate() != null)
                .sorted(Comparator.comparing(l -> l.getRequestDate(), Comparator.reverseOrder()))
                .limit(20)
                .map(l -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("member",    l.getMember() != null ? l.getMember().getName() : "—");
                    m.put("amount",    fmt(l.getAmount()));
                    m.put("status",    l.getStatus());
                    m.put("disbStatus", l.getDisbursementStatus() != null ? l.getDisbursementStatus() : "—");
                    m.put("date",      l.getRequestDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                    m.put("purpose",   l.getPurpose() != null && l.getPurpose().length() > 50
                            ? l.getPurpose().substring(0,50)+"…" : l.getPurpose());
                    return m;
                }).collect(Collectors.toList()));
        model.addAttribute("loans", loans);

        // ── Active loan portfolio ──────────────────────────────────────────────
        var activeLoans = loanRepository.findByStatus("APPROVED").stream()
                .filter(l -> l.getRemainingBalance() != null && l.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        var portfolio = new LinkedHashMap<String, Object>();
        portfolio.put("activeCount",      activeLoans.size());
        portfolio.put("totalOriginal",    fmt(activeLoans.stream().map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)));
        portfolio.put("totalOutstanding", fmt(activeLoans.stream().map(l -> l.getRemainingBalance() != null ? l.getRemainingBalance() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)));
        portfolio.put("loans", activeLoans.stream().map(l -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("member",     l.getMember() != null ? l.getMember().getName() : "—");
            m.put("original",   fmt(l.getAmount()));
            m.put("remaining",  fmt(l.getRemainingBalance()));
            m.put("instalment", fmt(l.getMonthlyInstalment()));
            m.put("months",     l.getRepaymentMonths());
            m.put("disbursed",  "DISBURSED".equals(l.getDisbursementStatus()) ? "Yes" : "Pending");
            return m;
        }).collect(Collectors.toList()));
        model.addAttribute("loanPortfolio", portfolio);

        // ── Loan repayments ────────────────────────────────────────────────────
        var allRepayments = loanRepaymentRepository.findAll();
        var repayments = new LinkedHashMap<String, Object>();
        repayments.put("totalRecords", allRepayments.size());
        repayments.put("totalRepaid",  fmt(allRepayments.stream()
                .map(r -> r.getRepaymentAmount() != null ? r.getRepaymentAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        repayments.put("recent", allRepayments.stream()
                .filter(r -> r.getRepaymentDate() != null)
                .sorted(Comparator.comparing(r -> r.getRepaymentDate(), Comparator.reverseOrder()))
                .limit(20)
                .map(r -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("member",  r.getMember() != null ? r.getMember().getName() : "—");
                    m.put("period",  r.getPeriodMonth() + "/" + r.getPeriodYear());
                    m.put("amount",  fmt(r.getRepaymentAmount()));
                    m.put("balance", fmt(r.getBalanceAfter()));
                    m.put("date",    r.getRepaymentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                    return m;
                }).collect(Collectors.toList()));
        model.addAttribute("loanRepayments", repayments);

        // ── Staff salaries ─────────────────────────────────────────────────────
        var allSalaries = salaryRepository.findAll();
        var salaries = new LinkedHashMap<String, Object>();
        salaries.put("totalRecords",    allSalaries.size());
        salaries.put("paid",            allSalaries.stream().filter(s -> "PAID".equals(s.getStatus())).count());
        salaries.put("unpaid",          allSalaries.stream().filter(s -> "UNPAID".equals(s.getStatus())).count());
        salaries.put("totalPaid",       fmt(allSalaries.stream().filter(s -> "PAID".equals(s.getStatus())).map(s -> s.getNetSalary() != null ? s.getNetSalary() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)));
        salaries.put("totalDeductions", fmt(allSalaries.stream().map(s -> s.getDeductions() != null ? s.getDeductions() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)));
        salaries.put("recent", allSalaries.stream()
                .filter(s -> "PAID".equals(s.getStatus()) && s.getPaidAt() != null)
                .sorted(Comparator.comparing(s -> s.getPaidAt(), Comparator.reverseOrder()))
                .limit(15)
                .map(s -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("staff",     s.getUser() != null ? s.getUser().getFullName() : "—");
                    m.put("period",    s.getMonth() + "/" + s.getYear());
                    m.put("basic",     fmt(s.getBasicSalary()));
                    m.put("deduction", fmt(s.getDeductions()));
                    m.put("net",       fmt(s.getNetSalary()));
                    m.put("method",    s.getPaymentMethod() != null ? s.getPaymentMethod().replace("_"," ") : "—");
                    m.put("ref",       s.getReferenceNo() != null ? s.getReferenceNo() : "—");
                    m.put("paidAt",    s.getPaidAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                    return m;
                }).collect(Collectors.toList()));
        model.addAttribute("salaries", salaries);

        // ── Member payments ────────────────────────────────────────────────────
        var allPayments = paymentRepository.findAll();
        var milkPay = new LinkedHashMap<String, Object>();
        milkPay.put("total",        allPayments.size());
        milkPay.put("paid",         allPayments.stream().filter(p -> "PAID".equals(p.getStatus())).count());
        milkPay.put("unpaid",       allPayments.stream().filter(p -> "UNPAID".equals(p.getStatus())).count());
        milkPay.put("totalGross",   fmt(allPayments.stream().map(p -> p.getGrossAmount() != null ? p.getGrossAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)));
        milkPay.put("totalNetPaid", fmt(allPayments.stream().filter(p -> "PAID".equals(p.getStatus())).map(p -> p.getNetAmount() != null ? p.getNetAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add)));
        milkPay.put("recent", allPayments.stream()
                .sorted(Comparator.comparingInt(p -> -(p.getYear() * 12 + p.getMonth())))
                .limit(15)
                .map(p -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("member",     p.getMember() != null ? p.getMember().getName() : "—");
                    m.put("period",     p.getMonth() + "/" + p.getYear());
                    m.put("gross",      fmt(p.getGrossAmount()));
                    m.put("deductions", fmt(p.getTotalDeductions()));
                    m.put("net",        fmt(p.getNetAmount()));
                    m.put("status",     p.getStatus());
                    return m;
                }).collect(Collectors.toList()));
        model.addAttribute("milkPayments", milkPay);

        // ── Milk inquiries ─────────────────────────────────────────────────────
        var allInquiries = milkInquiryRepository.findAll();
        var inquiries = new LinkedHashMap<String, Object>();
        inquiries.put("total",        allInquiries.size());
        inquiries.put("pending",      allInquiries.stream().filter(i -> i.getStatus() == MilkInquiry.Status.PENDING).count());
        inquiries.put("approved",     allInquiries.stream().filter(i -> i.getStatus() == MilkInquiry.Status.APPROVED).count());
        inquiries.put("rejected",     allInquiries.stream().filter(i -> i.getStatus() == MilkInquiry.Status.REJECTED).count());
        inquiries.put("paid",         allInquiries.stream().filter(i -> i.getStatus() == MilkInquiry.Status.PAID).count());
        inquiries.put("totalRevenue", fmt(allInquiries.stream()
                .filter(i -> i.getStatus() == MilkInquiry.Status.PAID)
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        inquiries.put("recent", allInquiries.stream()
                .filter(i -> i.getSubmittedAt() != null)
                .sorted(Comparator.comparing(i -> i.getSubmittedAt(), Comparator.reverseOrder()))
                .limit(15)
                .map(i -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("ref",     i.getReferenceNo());
                    m.put("company", i.getCompanyName());
                    m.put("qty",     i.getRequestedLitres() != null ? i.getRequestedLitres() + " L" : "—");
                    m.put("amount",  fmt(i.getTotalAmount()));
                    m.put("status",  i.getStatus().name());
                    m.put("date",    i.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                    return m;
                }).collect(Collectors.toList()));
        model.addAttribute("milkInquiries", inquiries);

        // ── Milk deliveries ────────────────────────────────────────────────────
        var allDeliveries = milkDeliveryRepository.findAll();
        var deliveries = new LinkedHashMap<String, Object>();
        deliveries.put("totalRecords",      allDeliveries.size());
        deliveries.put("totalLitres",       fmt(allDeliveries.stream()
                .map(d -> d.getQuantityLitres() != null ? d.getQuantityLitres() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        deliveries.put("membersDelivering", allDeliveries.stream()
                .filter(d -> d.getMember() != null)
                .map(d -> d.getMember().getId()).distinct().count());
        model.addAttribute("milkDeliveries", deliveries);

        // ── COFA ──────────────────────────────────────────────────────────────
        var fund = fundRepository.findAll().stream().findFirst();
        var fundMap = new LinkedHashMap<String, Object>();
        fund.ifPresent(f -> fundMap.put("balance", fmt(f.getBalance())));
        var allTxns = fundTransactionRepository.findAll();
        fundMap.put("totalTxns",    allTxns.size());
        fundMap.put("totalInflows", fmt(allTxns.stream()
                .filter(t -> t.getTransactionType() != null
                        && (t.getTransactionType().name().equals("MILK_SALE")
                        || t.getTransactionType().name().equals("LOAN_REPAYMENT")
                        || t.getTransactionType().name().equals("MANUAL_TOPUP")))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        fundMap.put("totalOutflows", fmt(allTxns.stream()
                .filter(t -> t.getTransactionType() != null
                        && (t.getTransactionType().name().equals("LOAN_DISBURSEMENT")
                        || t.getTransactionType().name().equals("SALARY_PAYMENT")))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        model.addAttribute("cofa", fundMap);

        // ── Audit trail ────────────────────────────────────────────────────────
        var auditLogs = auditLogRepository.findTop50ByOrderByChangedAtDesc().stream()
                .map(a -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("entity",  a.getEntityType());
                    m.put("action",  a.getAction().replace("_", " "));
                    m.put("by",      a.getChangedBy() != null ? a.getChangedBy().getFullName() : "System");
                    m.put("at",      a.getChangedAt() != null ? a.getChangedAt().format(fmt) : "—");
                    return m;
                }).collect(Collectors.toList());
        model.addAttribute("auditTrail", auditLogs);

        // ── Summary ────────────────────────────────────────────────────────────
        var summary = new LinkedHashMap<String, Object>();
        summary.put("totalMembers",  memberRepository.count());
        summary.put("activeMembers", memberRepository.findByStatusAndActiveTrue("APPROVED").size());
        summary.put("totalStaff",    userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && !"MEMBER".equals(u.getRole())).count());
        model.addAttribute("summary", summary);
    }

    // ── Save draft report (called after generate, before send) ───────────────
    @PostMapping("/save-draft")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveDraft(
            @RequestBody Map<String, Object> reportData,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            var officer = userRepository.findByEmail(ud.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String json = objectMapper.writeValueAsString(reportData);
            var submission = ReportSubmission.builder()
                    .submittedBy(officer)
                    .reportData(json)
                    .periodLabel(String.valueOf(reportData.getOrDefault("generatedAt", "—")))
                    .build();
            var saved = reportSubmissionRepository.save(submission);
            return ResponseEntity.ok(Map.of("id", saved.getId().toString(), "status", "saved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Send report to admin ──────────────────────────────────────────────────
    @PostMapping("/send/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendToAdmin(
            @PathVariable UUID id) {
        try {
            var submission = reportSubmissionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Report not found"));
            submission.setSentAt(java.time.LocalDateTime.now());
            reportSubmissionRepository.save(submission);
            return ResponseEntity.ok(Map.of("status", "sent"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Generate report data — called via fetch() ─────────────────────────────
    @PostMapping("/generate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateReport() {
        var report = new LinkedHashMap<String, Object>();
        var fmt    = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        report.put("generatedAt", LocalDateTime.now().format(fmt));

        // ── 1. LOANS ──────────────────────────────────────────────────────────
        var allLoans     = loanRepository.findAll();
        long lTotal      = allLoans.size();
        long lPending    = allLoans.stream().filter(l -> "PENDING".equals(l.getStatus())).count();
        long lApproved   = allLoans.stream().filter(l -> "APPROVED".equals(l.getStatus())).count();
        long lRejected   = allLoans.stream().filter(l -> "REJECTED".equals(l.getStatus())).count();
        long lDisbursed  = allLoans.stream().filter(l -> "DISBURSED".equals(l.getDisbursementStatus())).count();
        BigDecimal lTotalAmount = allLoans.stream()
                .filter(l -> "APPROVED".equals(l.getStatus()) || "DISBURSED".equals(l.getDisbursementStatus()))
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lTotalDisbursed = allLoans.stream()
                .filter(l -> "DISBURSED".equals(l.getDisbursementStatus()))
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Recent loan activities (last 20)
        var recentLoans = allLoans.stream()
                .filter(l -> l.getRequestDate() != null)
                .sorted(Comparator.comparing(l -> l.getRequestDate(), Comparator.reverseOrder()))
                .limit(20)
                .map(l -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("member",   l.getMember() != null ? l.getMember().getName() : "—");
                    m.put("amount",   fmt(l.getAmount()));
                    m.put("status",   l.getStatus());
                    m.put("disbStatus", l.getDisbursementStatus() != null ? l.getDisbursementStatus() : "—");
                    m.put("date",     l.getRequestDate() != null ? l.getRequestDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—");
                    m.put("purpose",  l.getPurpose() != null && l.getPurpose().length() > 50
                            ? l.getPurpose().substring(0,50)+"…" : l.getPurpose());
                    return m;
                }).collect(Collectors.toList());

        var loans = new LinkedHashMap<String, Object>();
        loans.put("total", lTotal);
        loans.put("pending", lPending);
        loans.put("approved", lApproved);
        loans.put("rejected", lRejected);
        loans.put("disbursed", lDisbursed);
        loans.put("totalAmountApproved", fmt(lTotalAmount));
        loans.put("totalAmountDisbursed", fmt(lTotalDisbursed));
        loans.put("recent", recentLoans);
        report.put("loans", loans);

        // ── 2. SALARIES ───────────────────────────────────────────────────────
        var allSalaries  = salaryRepository.findAll();
        long sPaid       = allSalaries.stream().filter(s -> "PAID".equals(s.getStatus())).count();
        long sUnpaid     = allSalaries.stream().filter(s -> "UNPAID".equals(s.getStatus())).count();
        BigDecimal sTotalPaid = allSalaries.stream()
                .filter(s -> "PAID".equals(s.getStatus()))
                .map(s -> s.getNetSalary() != null ? s.getNetSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sTotalDeductions = allSalaries.stream()
                .map(s -> s.getDeductions() != null ? s.getDeductions() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var recentSalaries = allSalaries.stream()
                .filter(s -> "PAID".equals(s.getStatus()) && s.getPaidAt() != null)
                .sorted(Comparator.comparing(s -> s.getPaidAt(), Comparator.reverseOrder()))
                .limit(15)
                .map(s -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("staff",      s.getUser() != null ? s.getUser().getFullName() : "—");
                    m.put("period",     s.getMonth() + "/" + s.getYear());
                    m.put("basic",      fmt(s.getBasicSalary()));
                    m.put("deduction",  fmt(s.getDeductions()));
                    m.put("net",        fmt(s.getNetSalary()));
                    m.put("method",     s.getPaymentMethod() != null ? s.getPaymentMethod().replace("_"," ") : "—");
                    m.put("ref",        s.getReferenceNo() != null ? s.getReferenceNo() : "—");
                    m.put("paidAt",     s.getPaidAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                    return m;
                }).collect(Collectors.toList());

        var salaries = new LinkedHashMap<String, Object>();
        salaries.put("totalRecords",   allSalaries.size());
        salaries.put("paid",           sPaid);
        salaries.put("unpaid",         sUnpaid);
        salaries.put("totalPaid",      fmt(sTotalPaid));
        salaries.put("totalDeductions",fmt(sTotalDeductions));
        salaries.put("recent",         recentSalaries);
        report.put("salaries", salaries);

        // ── 3. MEMBER MILK PAYMENTS ───────────────────────────────────────────
        var allPayments  = paymentRepository.findAll();
        long mPaid       = allPayments.stream().filter(p -> "PAID".equals(p.getStatus())).count();
        long mUnpaid     = allPayments.stream().filter(p -> "UNPAID".equals(p.getStatus())).count();
        BigDecimal mTotalGross = allPayments.stream()
                .map(p -> p.getGrossAmount() != null ? p.getGrossAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mTotalNet = allPayments.stream()
                .filter(p -> "PAID".equals(p.getStatus()))
                .map(p -> p.getNetAmount() != null ? p.getNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var recentPayments = allPayments.stream()
                .sorted(Comparator.comparingInt(p -> -(p.getYear() * 12 + p.getMonth())))
                .limit(15)
                .map(p -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("member",  p.getMember() != null ? p.getMember().getName() : "—");
                    m.put("period",  p.getMonth() + "/" + p.getYear());
                    m.put("gross",   fmt(p.getGrossAmount()));
                    m.put("deductions", fmt(p.getTotalDeductions()));
                    m.put("net",     fmt(p.getNetAmount()));
                    m.put("status",  p.getStatus());
                    return m;
                }).collect(Collectors.toList());

        var milkPay = new LinkedHashMap<String, Object>();
        milkPay.put("total",       allPayments.size());
        milkPay.put("paid",        mPaid);
        milkPay.put("unpaid",      mUnpaid);
        milkPay.put("totalGross",  fmt(mTotalGross));
        milkPay.put("totalNetPaid",fmt(mTotalNet));
        milkPay.put("recent",      recentPayments);
        report.put("milkPayments", milkPay);

        // ── 4. COMPANY MILK INQUIRIES (external requests) ────────────────────
        var allInquiries = milkInquiryRepository.findAll();
        long iTotal   = allInquiries.size();
        long iPending = allInquiries.stream().filter(i -> i.getStatus() == MilkInquiry.Status.PENDING).count();
        long iApproved= allInquiries.stream().filter(i -> i.getStatus() == MilkInquiry.Status.APPROVED).count();
        long iRejected= allInquiries.stream().filter(i -> i.getStatus() == MilkInquiry.Status.REJECTED).count();
        long iPaid    = allInquiries.stream().filter(i -> i.getStatus() == MilkInquiry.Status.PAID).count();
        BigDecimal iTotalRevenue = allInquiries.stream()
                .filter(i -> i.getStatus() == MilkInquiry.Status.PAID)
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var recentInquiries = allInquiries.stream()
                .filter(i -> i.getSubmittedAt() != null)
                .sorted(Comparator.comparing(i -> i.getSubmittedAt(), Comparator.reverseOrder()))
                .limit(15)
                .map(i -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("ref",     i.getReferenceNo());
                    m.put("company", i.getCompanyName());
                    m.put("qty",     i.getRequestedLitres() != null ? i.getRequestedLitres() + " L" : "—");
                    m.put("amount",  fmt(i.getTotalAmount()));
                    m.put("status",  i.getStatus().name());
                    m.put("date",    i.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                    return m;
                }).collect(Collectors.toList());

        var inquiries = new LinkedHashMap<String, Object>();
        inquiries.put("total",        iTotal);
        inquiries.put("pending",      iPending);
        inquiries.put("approved",     iApproved);
        inquiries.put("rejected",     iRejected);
        inquiries.put("paid",         iPaid);
        inquiries.put("totalRevenue", fmt(iTotalRevenue));
        inquiries.put("recent",       recentInquiries);
        report.put("milkInquiries", inquiries);

        // ── 5. COOPERATIVE FUND ───────────────────────────────────────────────
        var fund = fundRepository.findAll().stream().findFirst();
        var fundMap = new LinkedHashMap<String, Object>();
        fund.ifPresent(f -> {
            fundMap.put("balance", fmt(f.getBalance()));
            fundMap.put("name",    "Cooperative Fund Account (COFA)");
        });

        // COFA transaction summary (inflows vs outflows)
        var allTxns = fundTransactionRepository.findAll();
        BigDecimal cofaInflows = allTxns.stream()
                .filter(t -> t.getTransactionType() != null
                        && (t.getTransactionType().name().equals("MILK_SALE")
                        || t.getTransactionType().name().equals("LOAN_REPAYMENT")
                        || t.getTransactionType().name().equals("MANUAL_TOPUP")))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cofaOutflows = allTxns.stream()
                .filter(t -> t.getTransactionType() != null
                        && (t.getTransactionType().name().equals("LOAN_DISBURSEMENT")
                        || t.getTransactionType().name().equals("SALARY_PAYMENT")))
                .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        fundMap.put("totalInflows",  fmt(cofaInflows));
        fundMap.put("totalOutflows", fmt(cofaOutflows));
        fundMap.put("totalTxns",     allTxns.size());
        report.put("cofa", fundMap);

        // ── 6. LOAN REPAYMENTS (recovered from member salaries) ──────────────
        var allRepayments = loanRepaymentRepository.findAll();
        BigDecimal totalRepaid = allRepayments.stream()
                .map(r -> r.getRepaymentAmount() != null ? r.getRepaymentAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var recentRepayments = allRepayments.stream()
                .filter(r -> r.getRepaymentDate() != null)
                .sorted(Comparator.comparing(r -> r.getRepaymentDate(), Comparator.reverseOrder()))
                .limit(20)
                .map(r -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("member",  r.getMember() != null ? r.getMember().getName() : "—");
                    m.put("period",  r.getPeriodMonth() + "/" + r.getPeriodYear());
                    m.put("amount",  fmt(r.getRepaymentAmount()));
                    m.put("balance", fmt(r.getBalanceAfter()));
                    m.put("date",    r.getRepaymentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                    return m;
                }).collect(Collectors.toList());

        var repayments = new LinkedHashMap<String, Object>();
        repayments.put("totalRecords", allRepayments.size());
        repayments.put("totalRepaid",  fmt(totalRepaid));
        repayments.put("recent",       recentRepayments);
        report.put("loanRepayments", repayments);

        // ── 7. ACTIVE LOAN PORTFOLIO ──────────────────────────────────────────
        var activeLoans = loanRepository.findByStatus("APPROVED").stream()
                .filter(l -> l.getRemainingBalance() != null
                        && l.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        BigDecimal totalOutstanding = activeLoans.stream()
                .map(l -> l.getRemainingBalance() != null ? l.getRemainingBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOriginal = activeLoans.stream()
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var activeLoanRows = activeLoans.stream()
                .map(l -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("member",      l.getMember() != null ? l.getMember().getName() : "—");
                    m.put("original",    fmt(l.getAmount()));
                    m.put("remaining",   fmt(l.getRemainingBalance()));
                    m.put("instalment",  fmt(l.getMonthlyInstalment()));
                    m.put("months",      l.getRepaymentMonths());
                    m.put("disbursed",   "DISBURSED".equals(l.getDisbursementStatus()) ? "Yes" : "Pending");
                    return m;
                }).collect(Collectors.toList());

        var portfolio = new LinkedHashMap<String, Object>();
        portfolio.put("activeCount",    activeLoans.size());
        portfolio.put("totalOriginal",  fmt(totalOriginal));
        portfolio.put("totalOutstanding", fmt(totalOutstanding));
        portfolio.put("loans",          activeLoanRows);
        report.put("loanPortfolio", portfolio);

        // ── 8. MILK DELIVERIES STATISTICS ─────────────────────────────────────
        var allDeliveries = milkDeliveryRepository.findAll();
        BigDecimal totalLitres = allDeliveries.stream()
                .map(d -> d.getQuantityLitres() != null ? d.getQuantityLitres() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long membersWithDeliveries = allDeliveries.stream()
                .filter(d -> d.getMember() != null)
                .map(d -> d.getMember().getId())
                .distinct().count();
        var deliveries = new LinkedHashMap<String, Object>();
        deliveries.put("totalRecords",       allDeliveries.size());
        deliveries.put("totalLitres",        fmt(totalLitres));
        deliveries.put("membersDelivering",  membersWithDeliveries);
        report.put("milkDeliveries", deliveries);

        // ── 9. AUDIT TRAIL (last 50 activities) ──────────────────────────────
        var auditLogs = auditLogRepository.findTop50ByOrderByChangedAtDesc().stream()
                .map(a -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("entity",    a.getEntityType());
                    m.put("action",    a.getAction().replace("_", " "));
                    m.put("by",        a.getChangedBy() != null ? a.getChangedBy().getFullName() : "System");
                    m.put("at",        a.getChangedAt() != null ? a.getChangedAt().format(fmt) : "—");
                    return m;
                }).collect(Collectors.toList());
        report.put("auditTrail", auditLogs);

        // ── 7. SUMMARY STATS ─────────────────────────────────────────────────
        var summary = new LinkedHashMap<String, Object>();
        summary.put("totalMembers",   memberRepository.count());
        summary.put("activeMembers",  memberRepository.findByStatusAndActiveTrue("APPROVED").size());
        summary.put("totalStaff",     userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && !"MEMBER".equals(u.getRole())).count());
        report.put("summary", summary);

        // ── Notify admin via portal (no email) ───────────────────────────────
        // The accountant will click "Send to Admin" separately after reviewing

        return ResponseEntity.ok(report);
    }

    /** Remove email-based notify — portal-only via send endpoint */

    private String fmt(BigDecimal val) {
        if (val == null) return "0.00";
        return String.format("%,.2f", val.setScale(2, RoundingMode.HALF_UP));
    }
}