package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.*;
import com.dairycoopms.dairycoopms.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ManagerService  full implementation of all Manager responsibilities.
 *
 * Spec section 3  Manager:
 *   1. View dashboards and reports
 *   2. Monitor milk production levels
 *   3. Track cooperative income
 *   4. Monitor cow allocation among members
 *
 * Managers are READ-ONLY. All methods are @Transactional(readOnly = true).
 * No data is ever written through this service.
 */
@Service
@Transactional(readOnly = true)
public class ManagerService {

    private final MemberRepository        memberRepository;
    private final AdminService            adminService;
    private final MilkPriceRepository     milkPriceRepository;
    private final CowRepository           cowRepository;
    private final CowAssignmentRepository cowAssignmentRepository;
    private final MilkDeliveryRepository  milkDeliveryRepository;
    private final MilkSaleRepository      milkSaleRepository;
    private final PaymentRepository       paymentRepository;
    private final LoanRepository          loanRepository;
    private final TransactionRepository   transactionRepository;

    //  Explicit constructor (works in all IntelliJ editions)
    public ManagerService(
            MemberRepository memberRepository,
            AdminService adminService,
            MilkPriceRepository milkPriceRepository,
            CowRepository cowRepository,
            CowAssignmentRepository cowAssignmentRepository,
            MilkDeliveryRepository milkDeliveryRepository,
            MilkSaleRepository milkSaleRepository,
            PaymentRepository paymentRepository,
            LoanRepository loanRepository,
            TransactionRepository transactionRepository) {
        this.memberRepository = memberRepository;
        this.adminService     = adminService;
        this.milkPriceRepository = milkPriceRepository;
        this.cowRepository = cowRepository;
        this.cowAssignmentRepository = cowAssignmentRepository;
        this.milkDeliveryRepository = milkDeliveryRepository;
        this.milkSaleRepository = milkSaleRepository;
        this.paymentRepository = paymentRepository;
        this.loanRepository = loanRepository;
        this.transactionRepository = transactionRepository;
    }
    //


    //
    // 1. DASHBOARD SUMMARY
    //

    /**
     * Returns the key-value map that drives the management dashboard.
     * Every value is current at the moment of the call.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();

        //  Members - use filtered list to exclude staff/admin accounts
        java.util.List<com.dairycoopms.dairycoopms.model.Member> allRealMembers =
                adminService.getAllMembers();
        long totalM   = allRealMembers.size();
        long activeM  = allRealMembers.stream().filter(m -> m.isActive()).count();
        long pendingM = adminService.getMembersByStatus("PENDING").size();
        summary.put("totalMembers",     totalM);
        summary.put("activeMembers",    activeM);
        summary.put("pendingApprovals", pendingM);

        //  Cows
        long totalCows    = cowRepository.count();
        long assignedCows = cowAssignmentRepository.countByActiveTrue();
        summary.put("totalCows",      totalCows);
        summary.put("assignedCows",   assignedCows);
        summary.put("unassignedCows", totalCows - assignedCows);

        //  Milk
        BigDecimal todayLitres = milkDeliveryRepository
                .sumQuantityByDeliveryDate(LocalDate.now())
                .orElse(BigDecimal.ZERO);
        summary.put("todayMilkLitres",   todayLitres);
        summary.put("todayDeliveries",
                milkDeliveryRepository.findByDeliveryDate(LocalDate.now()).size());

        //  Sales & Payments
        summary.put("pendingSalePayments",
                milkSaleRepository.countByPaymentStatus("PENDING"));
        summary.put("unpaidMemberPayments",
                paymentRepository.countByStatus("UNPAID"));

        //  Loans
        summary.put("activeLoans",   loanRepository.countByStatus("APPROVED"));
        summary.put("pendingLoans",  loanRepository.countByStatus("PENDING"));

        // -- Milk Price --
        milkPriceRepository.findByActiveTrue().ifPresent(p ->
                summary.put("currentMilkPrice", p.getPricePerLitre()));
        if (!summary.containsKey("currentMilkPrice"))
            summary.put("currentMilkPrice", null);

        return summary;
    }

    //
    // 2. MILK PRODUCTION MONITORING
    //

    /**
     * Daily milk totals for a date range.
     * Returns [deliveryDate, totalLitres]  used for trend charts.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Object[]> getDailyMilkTotals(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return milkDeliveryRepository.getDailyTotals(from, to);
    }

    /**
     * Monthly milk totals for a full year.
     * Returns [month(1-12), totalLitres]  used for annual production chart.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Object[]> getMonthlyMilkTotals(int year) {
        return milkDeliveryRepository.getMonthlyTotals(year);
    }

    /**
     * Top N milk-producing members for a given month.
     * Returns [Member, totalLitres] ordered by litres descending.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Object[]> getTopProducers(int year, int month, int limit) {
        if (limit <= 0) throw new IllegalArgumentException("Limit must be positive");
        return milkDeliveryRepository.getTopProducers(year, month, PageRequest.of(0, limit));
    }

    /**
     * All deliveries within a date range (detail view).
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<MilkDelivery> getDeliveriesInRange(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return milkDeliveryRepository.findByDeliveryDateBetween(from, to);
    }

    //
    // 3. COOPERATIVE INCOME TRACKING
    //

    /**
     * Total revenue from milk sales to processing companies in a given month.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public BigDecimal getMonthlyRevenue(int year, int month) {
        return milkSaleRepository.sumRevenueByYearAndMonth(year, month)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * All milk sales  full history.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<MilkSale> getAllMilkSales() {
        return milkSaleRepository.findAll();
    }

    /**
     * Sales awaiting payment from processing companies.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<MilkSale> getPendingSalePayments() {
        return milkSaleRepository.findByPaymentStatus("PENDING");
    }

    /**
     * Revenue broken down by processing company for a given month.
     * Returns [ProcessingCompany, totalRevenue] pairs.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Object[]> getRevenueByCompany(int year, int month) {
        return milkSaleRepository.getRevenueByCompany(year, month);
    }

    /**
     * Full financial transaction ledger  all monetary events.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Transaction> getTransactionLedger() {
        return transactionRepository.findAll();
    }

    /**
     * Transaction history for a specific member.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Transaction> getMemberTransactions(UUID memberId) {
        return transactionRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    //
    // 4. COW ALLOCATION MONITORING
    //

    /**
     * All currently active cow assignments.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<CowAssignment> getAllActiveAssignments() {
        return cowAssignmentRepository.findByActiveTrue();
    }

    /**
     * Number of cows per member  [Member, cowCount] pairs.
     * Drives the cow allocation pie/bar chart on the dashboard.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Object[]> getCowCountPerMember() {
        return cowAssignmentRepository.getCowCountPerMember();
    }

    /**
     * All assignments for a specific member.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<CowAssignment> getAssignmentsByMember(UUID memberId) {
        return cowAssignmentRepository.findByMemberId(memberId);
    }

    /**
     * APPROVED active members with NO cow assigned.
     * These members are unproductive  flag for management attention.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Member> getMembersWithNoCows() {
        return memberRepository.findMembersWithNoActiveCowAssignment();
    }

    //
    // 5. REPORTS & PAYMENT MONITORING
    //

    /**
     * All payment records for a given month  monthly payment report.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Payment> getMonthlyPaymentReport(int year, int month) {
        return paymentRepository.findByYearAndMonth(year, month);
    }

    /**
     * All UNPAID member payments  outstanding payments report.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Payment> getOutstandingPayments() {
        return paymentRepository.findByStatus("UNPAID");
    }

    /**
     * All APPROVED loans with remaining balance  active loan portfolio.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Loan> getActiveLoanPortfolio() {
        return loanRepository.findByStatus("APPROVED");
    }

    /**
     * All PENDING loan applications awaiting officer decision.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Loan> getPendingLoanApplications() {
        return loanRepository.findByStatus("PENDING");
    }

    /**
     * All members  full member list report.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    /**
     * All cows  full livestock report.
     */
    @PreAuthorize("hasRole('SALES') or hasRole('ADMIN')")
    public List<Cow> getAllCows() {
        return cowRepository.findAll();
    }

    //
    // PRIVATE HELPERS
    //

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null)
            throw new IllegalArgumentException("Both from and to dates are required");
        if (from.isAfter(to))
            throw new IllegalArgumentException(
                    "From date (" + from + ") must not be after to date (" + to + ")");
    }
}