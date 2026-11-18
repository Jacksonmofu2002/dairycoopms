package com.dairycoopms.dairycoopms.controller.admin;

import com.dairycoopms.dairycoopms.model.MilkInquiry;
import com.dairycoopms.dairycoopms.repository.*;
import com.dairycoopms.dairycoopms.service.AdminService;
import com.dairycoopms.dairycoopms.service.LoanService;
import com.dairycoopms.dairycoopms.service.ManagerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final ManagerService              managerService;
    private final AdminService                adminService;
    private final LoanService                 loanService;
    private final MilkSaleRepository          milkSaleRepository;
    private final MilkDeliveryRepository      milkDeliveryRepository;
    private final UserRepository              userRepository;
    private final LoanRepository              loanRepository;
    private final StaffSalaryRepository       salaryRepository;
    private final MilkInquiryRepository       milkInquiryRepository;
    private final PaymentRepository           paymentRepository;
    private final ReportSubmissionRepository  reportSubmissionRepository;

    public AdminDashboardController(ManagerService managerService,
                                    AdminService adminService,
                                    LoanService loanService,
                                    MilkSaleRepository milkSaleRepository,
                                    MilkDeliveryRepository milkDeliveryRepository,
                                    UserRepository userRepository,
                                    LoanRepository loanRepository,
                                    StaffSalaryRepository salaryRepository,
                                    MilkInquiryRepository milkInquiryRepository,
                                    PaymentRepository paymentRepository,
                                    ReportSubmissionRepository reportSubmissionRepository) {
        this.managerService            = managerService;
        this.adminService              = adminService;
        this.loanService               = loanService;
        this.milkSaleRepository        = milkSaleRepository;
        this.milkDeliveryRepository    = milkDeliveryRepository;
        this.userRepository            = userRepository;
        this.loanRepository            = loanRepository;
        this.salaryRepository          = salaryRepository;
        this.milkInquiryRepository     = milkInquiryRepository;
        this.paymentRepository         = paymentRepository;
        this.reportSubmissionRepository = reportSubmissionRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {

        userRepository.findByEmail(ud.getUsername()).ifPresent(u -> {
            model.addAttribute("user", u);
            model.addAttribute("mustChangePassword", u.isMustChangePassword());
        });

        // ── KPI summary ───────────────────────────────────────────────────────
        try {
            Map<String, Object> summary = managerService.getDashboardSummary();
            model.addAllAttributes(summary);
            // Fix key name mismatches between service and template
            model.addAttribute("totalCount",   summary.getOrDefault("totalMembers", 0));
            model.addAttribute("pendingCount", summary.getOrDefault("pendingApprovals", 0));
        } catch (Exception e) {
            model.addAttribute("totalCount", 0);
            model.addAttribute("pendingCount", 0);
        }

        try {
            List<?> allPending = adminService.getMembersByStatus("PENDING");
            model.addAttribute("pendingMembers",
                    allPending.size() > 5 ? allPending.subList(0, 5) : allPending);
        } catch (Exception e) { model.addAttribute("pendingMembers", List.of()); }

        try {
            List<?> allPendingLoans = loanService.getPendingLoans();
            model.addAttribute("pendingLoansList",
                    allPendingLoans.size() > 5 ? allPendingLoans.subList(0, 5) : allPendingLoans);
        } catch (Exception e) { model.addAttribute("pendingLoansList", List.of()); }

        try {
            model.addAttribute("pendingSales",
                    milkSaleRepository.findByPaymentStatus("PENDING"));
        } catch (Exception e) { model.addAttribute("pendingSales", List.of()); }

        // ── Chart data ────────────────────────────────────────────────────────
        int year = LocalDate.now().getYear();
        String[] monthLabels = {"Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"};
        model.addAttribute("monthLabels", monthLabels);

        // 1. Monthly milk deliveries
        BigDecimal[] milkData = new BigDecimal[12];
        Arrays.fill(milkData, BigDecimal.ZERO);
        try {
            for (Object[] row : milkDeliveryRepository.getMonthlyTotals(year)) {
                int idx = ((Number) row[0]).intValue() - 1;
                milkData[idx] = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            }
        } catch (Exception ignored) {}
        model.addAttribute("monthlyMilkData", milkData);

        // 2. Loan breakdown
        long loanPending = 0, loanApproved = 0, loanRejected = 0, loanDisbursed = 0;
        BigDecimal totalLoanAmount = BigDecimal.ZERO;
        BigDecimal[] loanAmounts = new BigDecimal[12];
        Arrays.fill(loanAmounts, BigDecimal.ZERO);
        try {
            var allLoans = loanRepository.findAll();
            for (var l : allLoans) {
                String st = l.getStatus();
                if ("PENDING".equals(st))   loanPending++;
                if ("APPROVED".equals(st))  loanApproved++;
                if ("REJECTED".equals(st))  loanRejected++;
                if ("DISBURSED".equals(l.getDisbursementStatus())) loanDisbursed++;
                if (l.getAmount() != null) totalLoanAmount = totalLoanAmount.add(l.getAmount());
                if (l.getRequestDate() != null && l.getRequestDate().getYear() == year && l.getAmount() != null) {
                    int idx = l.getRequestDate().getMonthValue() - 1;
                    loanAmounts[idx] = loanAmounts[idx].add(l.getAmount());
                }
            }
            model.addAttribute("totalLoans", allLoans.size());
        } catch (Exception ignored) { model.addAttribute("totalLoans", 0); }
        model.addAttribute("loanPending",   loanPending);
        model.addAttribute("loanApproved",  loanApproved);
        model.addAttribute("loanRejected",  loanRejected);
        model.addAttribute("loanDisbursed", loanDisbursed);
        model.addAttribute("loanAmountsByMonth", loanAmounts);
        model.addAttribute("totalLoanAmount", totalLoanAmount.setScale(2, RoundingMode.HALF_UP));

        // 3. Salary totals
        BigDecimal[] salaryTotals = new BigDecimal[12];
        Arrays.fill(salaryTotals, BigDecimal.ZERO);
        try {
            salaryRepository.findByStatus("PAID").stream()
                    .filter(s -> s.getYear() == year)
                    .forEach(s -> {
                        int idx = s.getMonth() - 1;
                        salaryTotals[idx] = salaryTotals[idx].add(
                                s.getNetSalary() != null ? s.getNetSalary() : BigDecimal.ZERO);
                    });
        } catch (Exception ignored) {}
        model.addAttribute("salaryTotalsByMonth", salaryTotals);
        model.addAttribute("totalSalaryPaid",
                Arrays.stream(salaryTotals).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP));

        // 4. Inquiry counts + revenue
        Long[] inquiryCountLong = new Long[12];
        BigDecimal[] inquiryRevenue = new BigDecimal[12];
        Arrays.fill(inquiryRevenue, BigDecimal.ZERO);
        Arrays.fill(inquiryCountLong, 0L);
        try {
            milkInquiryRepository.findAll().stream()
                    .filter(i -> i.getSubmittedAt() != null && i.getSubmittedAt().getYear() == year)
                    .forEach(i -> {
                        int idx = i.getSubmittedAt().getMonthValue() - 1;
                        inquiryCountLong[idx]++;
                        if (i.getStatus() != null && "PAID".equals(i.getStatus().name())
                                && i.getTotalAmount() != null)
                            inquiryRevenue[idx] = inquiryRevenue[idx].add(i.getTotalAmount());
                    });
        } catch (Exception ignored) {}
        model.addAttribute("inquiryCountByMonth", inquiryCountLong);
        model.addAttribute("inquiryRevenueByMonth", inquiryRevenue);
        BigDecimal totalRevenue = Arrays.stream(inquiryRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalRevenue", totalRevenue.setScale(2, RoundingMode.HALF_UP));

        // 5. Member milk payments
        BigDecimal[] memberPayTotals = new BigDecimal[12];
        Arrays.fill(memberPayTotals, BigDecimal.ZERO);
        try {
            paymentRepository.findAll().stream()
                    .filter(p -> p.getYear() == year && "PAID".equals(p.getStatus()))
                    .forEach(p -> {
                        int idx = p.getMonth() - 1;
                        memberPayTotals[idx] = memberPayTotals[idx].add(
                                p.getNetAmount() != null ? p.getNetAmount() : BigDecimal.ZERO);
                    });
        } catch (Exception ignored) {}
        model.addAttribute("memberPayTotalsByMonth", memberPayTotals);

        // 6. Predictions
        int currentMonth = LocalDate.now().getMonthValue();
        BigDecimal[] milkPred = new BigDecimal[3];
        BigDecimal[] loanPred = new BigDecimal[3];
        BigDecimal growth = new BigDecimal("1.05");
        for (int i = 0; i < 3; i++) {
            int baseIdx = Math.max(0, currentMonth - 2 + i);
            milkPred[i] = milkData[baseIdx].multiply(growth.pow(i + 1)).setScale(2, RoundingMode.HALF_UP);
            loanPred[i] = loanAmounts[baseIdx].multiply(growth).setScale(2, RoundingMode.HALF_UP);
        }
        model.addAttribute("milkPredictions", milkPred);
        model.addAttribute("loanPredictions", loanPred);
        String[] predLabels = new String[3];
        for (int i = 0; i < 3; i++) {
            int m = (currentMonth + i - 1) % 12 + 1;
            predLabels[i] = Month.of(m).name().substring(0, 3);
        }
        model.addAttribute("predictionLabels", predLabels);

        // 7. Unread reports
        try {
            model.addAttribute("unreadReports",
                    reportSubmissionRepository.countByReadByAdminFalseAndSentAtNotNull());
        } catch (Exception ignored) { model.addAttribute("unreadReports", 0L); }

        return "admin/dashboard";
    }
}