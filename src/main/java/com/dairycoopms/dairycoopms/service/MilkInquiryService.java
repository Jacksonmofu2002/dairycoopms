package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.*;
import com.dairycoopms.dairycoopms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * MilkInquiryService
 *
 * Business logic for the inquiry-based sales workflow:
 *   - Receive inquiries from public portal
 *   - Review and process inquiries (approve/reject)
 *   - Create orders from approved inquiries
 *   - Generate invoices
 *   - Track payments and update COFA
 */
@Service
@Transactional(readOnly = true)
public class MilkInquiryService {

    private final MilkInquiryRepository inquiryRepository;
    private final MilkSaleRepository saleRepository;
    private final MilkDeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final CooperativeFundRepository cofaRepository;
    private final CooperativeFundTransactionRepository transactionRepository;

    public MilkInquiryService(MilkInquiryRepository inquiryRepository,
                              MilkSaleRepository saleRepository,
                              MilkDeliveryRepository deliveryRepository,
                              UserRepository userRepository,
                              CooperativeFundRepository cofaRepository,
                              CooperativeFundTransactionRepository transactionRepository) {
        this.inquiryRepository = inquiryRepository;
        this.saleRepository = saleRepository;
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
        this.cofaRepository = cofaRepository;
        this.transactionRepository = transactionRepository;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PORTAL - SUBMIT INQUIRY (Public)
    // ═════════════���═════════════════════════════════════════════════════════════

    /**
     * Submit a new inquiry from the public portal.
     * Returns the created inquiry with reference number.
     */
    @Transactional
    public MilkInquiry submitInquiry(String companyName, String contactPerson, String position,
                                     String email, String phone, BigDecimal requestedLitres,
                                     LocalDate preferredDate, MilkInquiry.Frequency frequency,
                                     String message) {

        // Validate minimum order
        if (requestedLitres.compareTo(new BigDecimal("50")) < 0) {
            throw new IllegalArgumentException("Minimum order is 50 litres");
        }

        // Validate preferred date (at least 2 days in advance)
        if (preferredDate.isBefore(LocalDate.now().plusDays(2))) {
            throw new IllegalArgumentException("Preferred date must be at least 2 days from today");
        }

        // Generate reference number
        String referenceNo = generateReferenceNo();

        MilkInquiry inquiry = MilkInquiry.builder()
                .referenceNo(referenceNo)
                .companyName(companyName)
                .contactPerson(contactPerson)
                .position(position)
                .email(email)
                .phone(phone)
                .requestedLitres(requestedLitres)
                .preferredDate(preferredDate)
                .frequency(frequency != null ? frequency : MilkInquiry.Frequency.ONE_TIME)
                .message(message)
                .status(MilkInquiry.Status.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        return inquiryRepository.save(inquiry);
    }

    /**
     * Generate unique reference number (INQ-2026-0001 format).
     */
    private String generateReferenceNo() {
        int year = LocalDate.now().getYear();
        String prefix = "INQ-" + year + "-";

        List<String> existing = inquiryRepository.findLatestReferenceNoByPrefix(prefix);
        int nextNum = 1;

        if (!existing.isEmpty()) {
            String latest = existing.get(0);
            String numPart = latest.substring(prefix.length());
            try {
                nextNum = Integer.parseInt(numPart) + 1;
            } catch (NumberFormatException ignored) {
                nextNum = (int) inquiryRepository.countByYear(year) + 1;
            }
        }

        return String.format("%s%04d", prefix, nextNum);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PORTAL - CHECK STATUS (Public)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find inquiry by reference number (for public status check).
     */
    public Optional<MilkInquiry> findByReference(String referenceNo) {
        return inquiryRepository.findByReferenceNoIgnoreCase(referenceNo.trim());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SALES - DASHBOARD STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get dashboard summary for Sales Officer.
     * Now uses cumulative inventory that carries forward between months.
     */
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        // Pending inquiries count
        long pendingInquiries = inquiryRepository.countByStatus(MilkInquiry.Status.PENDING);
        summary.put("pendingInquiries", pendingInquiries);

        // Active orders (approved but not paid)
        long activeOrders = saleRepository.countByPaymentStatus("PENDING");
        summary.put("activeOrders", activeOrders);

        // Monthly revenue (for current month only)
        BigDecimal monthlyRevenue = saleRepository
                .sumRevenueByYearAndMonth(year, month)
                .orElse(BigDecimal.ZERO);
        summary.put("monthlyRevenue", monthlyRevenue);
        summary.put("currentMonth", YearMonth.of(year, month).getMonth().toString() + " " + year);

        // Pending payments
        List<MilkSale> pendingSales = saleRepository.findByPaymentStatus("PENDING");
        long pendingPayments = pendingSales.size();
        BigDecimal pendingAmount = pendingSales.stream()
                .map(MilkSale::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.put("pendingPayments", pendingPayments);
        summary.put("pendingAmount", pendingAmount);

        // Available inventory - CUMULATIVE (carries forward between months)
        BigDecimal collectedAllTime = getTotalCollectedMilkAllTime();
        BigDecimal soldAllTime = getTotalSoldMilkAllTime();
        BigDecimal reserved = getReservedMilk(); // Approved inquiries not yet sold
        BigDecimal available = collectedAllTime.subtract(soldAllTime).subtract(reserved);
        if (available.compareTo(BigDecimal.ZERO) < 0) available = BigDecimal.ZERO;

        summary.put("collectedLitres", collectedAllTime);
        summary.put("soldLitres", soldAllTime);
        summary.put("reservedLitres", reserved);
        summary.put("availableLitres", available);

        // Available percentage
        BigDecimal percent = BigDecimal.ZERO;
        if (collectedAllTime.compareTo(BigDecimal.ZERO) > 0) {
            percent = available.divide(collectedAllTime, 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        summary.put("availablePercent", percent);

        // Recent inquiries (last 5 pending)
        List<MilkInquiry> recentInquiries = inquiryRepository
                .findByStatusOrderBySubmittedAtDesc(MilkInquiry.Status.PENDING);
        if (recentInquiries.size() > 5) {
            recentInquiries = recentInquiries.subList(0, 5);
        }
        summary.put("recentInquiries", recentInquiries);

        return summary;
    }

    /**
     * Get reserved milk (approved inquiries not yet converted to sales).
     */
    private BigDecimal getReservedMilk() {
        List<MilkInquiry> approved = inquiryRepository
                .findByStatusOrderBySubmittedAtDesc(MilkInquiry.Status.APPROVED);
        return approved.stream()
                .map(MilkInquiry::getRequestedLitres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get monthly collected milk.
     */
    public BigDecimal getMonthlyCollectedMilk(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<Object[]> dailyTotals = deliveryRepository.getDailyTotals(from, to);
        return dailyTotals.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get monthly sold milk.
     */
    public BigDecimal getMonthlySoldMilk(int year, int month) {
        List<MilkSale> allSales = saleRepository.findAll();
        return allSales.stream()
                .filter(s -> s.getSaleDate().getYear() == year
                        && s.getSaleDate().getMonthValue() == month)
                .map(MilkSale::getQuantityLitres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total collected milk from all time (cumulative).
     * This carries inventory forward between months.
     */
    private BigDecimal getTotalCollectedMilkAllTime() {
        List<Object[]> allDailyTotals = deliveryRepository.getAllDailyTotals();
        return allDailyTotals.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total sold milk from all time (cumulative).
     * This carries inventory forward between months.
     */
    private BigDecimal getTotalSoldMilkAllTime() {
        List<MilkSale> allSales = saleRepository.findAll();
        return allSales.stream()
                .filter(sale -> !sale.getPaymentStatus().equals("CANCELLED"))
                .map(MilkSale::getQuantityLitres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get available litres for sale.
     * Now calculates cumulative inventory that carries forward between months.
     */
    public BigDecimal getAvailableLitres() {
        BigDecimal collected = getTotalCollectedMilkAllTime();
        BigDecimal sold = getTotalSoldMilkAllTime();
        BigDecimal reserved = getReservedMilk();

        BigDecimal available = collected.subtract(sold).subtract(reserved);
        return available.compareTo(BigDecimal.ZERO) > 0 ? available : BigDecimal.ZERO;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SALES - INQUIRY MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all inquiries with optional status filter.
     */
    public List<MilkInquiry> getAllInquiries(MilkInquiry.Status status) {
        if (status == null) {
            return inquiryRepository.findAllOrderBySubmittedAtDesc();
        }
        return inquiryRepository.findByStatusOrderBySubmittedAtDesc(status);
    }

    /**
     * Get inquiry by ID.
     */
    public Optional<MilkInquiry> getInquiryById(UUID id) {
        return inquiryRepository.findById(id);
    }

    /**
     * Get counts by status.
     */
    public Map<String, Long> getStatusCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("total", inquiryRepository.count());
        counts.put("pending", inquiryRepository.countByStatus(MilkInquiry.Status.PENDING));
        counts.put("approved", inquiryRepository.countByStatus(MilkInquiry.Status.APPROVED));
        counts.put("rejected", inquiryRepository.countByStatus(MilkInquiry.Status.REJECTED));
        counts.put("invoiced", inquiryRepository.countByStatus(MilkInquiry.Status.INVOICED));
        counts.put("paid", inquiryRepository.countByStatus(MilkInquiry.Status.PAID));
        return counts;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SALES - APPROVE / REJECT INQUIRY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Approve an inquiry and create a sale/order.
     */
    @Transactional
    public MilkSale approveInquiry(UUID inquiryId, BigDecimal pricePerLitre, UUID salesOfficerId) {
        MilkInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));

        if (inquiry.getStatus() != MilkInquiry.Status.PENDING) {
            throw new IllegalStateException("Inquiry has already been processed");
        }

        // Defensive guard: never create a second order for the same inquiry,
        // even if a retried/duplicate request slips past the status check above
        if (saleRepository.findByReferenceNo(inquiry.getReferenceNo()).isPresent()) {
            throw new IllegalStateException(
                    "An order already exists for inquiry " + inquiry.getReferenceNo());
        }

        // Check availability
        BigDecimal available = getAvailableLitres();
        if (available.compareTo(inquiry.getRequestedLitres()) < 0) {
            throw new IllegalStateException(
                    String.format("⚠️ INSUFFICIENT MILK INVENTORY! Requested: %.2f L, Available: %.2f L. " +
                                    "Please notify the sales department that milk is running out.",
                            inquiry.getRequestedLitres(), available)
            );
        }

        // Check if approving this will result in low inventory
        BigDecimal remainingAfterApproval = available.subtract(inquiry.getRequestedLitres());
        BigDecimal lowInventoryThreshold = new BigDecimal("500");

        if (remainingAfterApproval.compareTo(lowInventoryThreshold) < 0 &&
                remainingAfterApproval.compareTo(BigDecimal.ZERO) > 0) {
            // Log warning but allow approval to proceed
            System.out.println(String.format("⚠️ WARNING: Low inventory after approval! Remaining: %.2f L",
                    remainingAfterApproval));
        }

        // Get sales officer
        User salesOfficer = userRepository.findById(salesOfficerId).orElse(null);

        // Calculate total
        BigDecimal totalRevenue = inquiry.getRequestedLitres()
                .multiply(pricePerLitre)
                .setScale(2, RoundingMode.HALF_UP);

        // Create the sale
        MilkSale sale = MilkSale.builder()
                .processingCompany(null) // No registered company, using inquiry details
                .quantityLitres(inquiry.getRequestedLitres())
                .pricePerLitre(pricePerLitre)
                .totalRevenue(totalRevenue)
                .saleDate(LocalDate.now())
                .paymentStatus("PENDING")
                .referenceNo(inquiry.getReferenceNo())
                .notes("Created from inquiry: " + inquiry.getReferenceNo())
                .build();

        sale = saleRepository.save(sale);

        // Update inquiry
        inquiry.setStatus(MilkInquiry.Status.APPROVED);
        inquiry.setPricePerLitre(pricePerLitre);
        inquiry.setTotalAmount(totalRevenue);
        inquiry.setProcessedAt(LocalDateTime.now());
        inquiry.setProcessedBy(salesOfficer);
        inquiryRepository.save(inquiry);

        return sale;
    }

    /**
     * Reject an inquiry with a reason.
     */
    @Transactional
    public MilkInquiry rejectInquiry(UUID inquiryId, String reason, UUID salesOfficerId) {
        MilkInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));

        if (inquiry.getStatus() != MilkInquiry.Status.PENDING) {
            throw new IllegalStateException("Inquiry has already been processed");
        }

        User salesOfficer = userRepository.findById(salesOfficerId).orElse(null);

        inquiry.setStatus(MilkInquiry.Status.REJECTED);
        inquiry.setRejectionReason(reason);
        inquiry.setProcessedAt(LocalDateTime.now());
        inquiry.setProcessedBy(salesOfficer);

        return inquiryRepository.save(inquiry);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SALES - ORDER & PAYMENT MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all orders/sales.
     */
    public List<MilkSale> getAllOrders() {
        List<MilkSale> orders = saleRepository.findAll();
        orders.sort((a, b) -> b.getSaleDate().compareTo(a.getSaleDate()));
        return orders;
    }

    /**
     * Get order by ID.
     */
    public Optional<MilkSale> getOrderById(UUID id) {
        return saleRepository.findById(id);
    }

    /**
     * Get orders by payment status.
     */
    public List<MilkSale> getOrdersByPaymentStatus(String status) {
        if ("ALL".equals(status)) {
            return getAllOrders();
        }
        return saleRepository.findByPaymentStatus(status);
    }

    /**
     * Mark payment as received (sends to Accountant for COFA approval).
     */
    @Transactional
    public MilkSale markPaymentReceived(UUID saleId) {
        MilkSale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found"));

        if (!"PENDING".equals(sale.getPaymentStatus())) {
            throw new IllegalStateException("Payment already processed");
        }

        // Mark as received (awaiting accountant approval for COFA transfer)
        sale.setPaymentStatus("RECEIVED");
        sale = saleRepository.save(sale);

        // Update linked inquiry status (find by reference number)
        if (sale.getReferenceNo() != null && sale.getReferenceNo().startsWith("INQ-")) {
            inquiryRepository.findByReferenceNo(sale.getReferenceNo())
                    .ifPresent(inquiry -> {
                        inquiry.setStatus(MilkInquiry.Status.PAID);
                        inquiryRepository.save(inquiry);
                    });
        }

        return sale;
    }

    /**
     * Sync the linked MilkSale's payment status when its originating inquiry
     * is confirmed PAID (by accountant or company self-service). Mirrors the
     * inverse sync already done in markPaymentReceived() above — keeps the
     * two parallel payment-status fields from drifting apart.
     */
    @Transactional
    public void syncSalePaymentReceived(String referenceNo) {
        if (referenceNo == null) return;
        saleRepository.findByReferenceNo(referenceNo).ifPresent(sale -> {
            if (!"RECEIVED".equals(sale.getPaymentStatus())) {
                sale.setPaymentStatus("RECEIVED");
                sale.setPaymentReceivedAt(LocalDateTime.now());
                saleRepository.save(sale);
            }
        });
    }

    /**
     * Generate invoice for a delivered inquiry.
     * Called by Sales Officer after delivery is confirmed.
     */
    @Transactional
    public MilkInquiry generateInvoice(UUID inquiryId) {
        MilkInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));

        // Check if delivery was confirmed (deliveredAt is set)
        if (inquiry.getDeliveredAt() == null) {
            throw new IllegalStateException("Invoice can only be generated after delivery is confirmed");
        }

        // Generate invoice number
        String invoiceNo = generateInvoiceNo();

        inquiry.setInvoiceNo(invoiceNo);
        inquiry.setInvoicedAt(LocalDateTime.now());
        inquiry.setStatus(MilkInquiry.Status.INVOICED);

        return inquiryRepository.save(inquiry);
    }

    /**
     * Generate unique invoice number (INV-2026-0001 format).
     * Public so SalesController can generate invoice numbers for PO workflow.
     */
    public String generateInvoiceNo() {
        int year = LocalDate.now().getYear();
        String prefix = "INV-" + year + "-";

        // Find max invoice number for this year
        List<MilkInquiry> allInquiries = inquiryRepository.findAll();
        int maxNum = 0;

        for (MilkInquiry inq : allInquiries) {
            if (inq.getInvoiceNo() != null && inq.getInvoiceNo().startsWith(prefix)) {
                try {
                    int num = Integer.parseInt(inq.getInvoiceNo().substring(prefix.length()));
                    if (num > maxNum) maxNum = num;
                } catch (NumberFormatException ignored) {}
            }
        }

        return String.format("%s%04d", prefix, maxNum + 1);
    }

    /**
     * Confirm payment by Accountant - updates COFA and generates receipt.
     * Handles missing workflow steps automatically:
     * - If APPROVED: marks as DELIVERED and INVOICED
     * - If DELIVERED: marks as INVOICED
     * - If INVOICED: proceeds normally
     */
    @Transactional
    public MilkInquiry confirmPayment(UUID inquiryId, UUID accountantId) {
        MilkInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));

        // Validate inquiry can be paid
        if (inquiry.getStatus() == MilkInquiry.Status.PENDING) {
            throw new IllegalStateException("Cannot confirm payment for pending inquiry. It must be approved first.");
        }

        if (inquiry.getStatus() == MilkInquiry.Status.REJECTED) {
            throw new IllegalStateException("Cannot confirm payment for rejected inquiry.");
        }

        if (inquiry.getStatus() == MilkInquiry.Status.PAID) {
            throw new IllegalStateException("Payment has already been confirmed for this inquiry.");
        }

        User accountant = userRepository.findById(accountantId).orElse(null);

        // Auto-complete missing workflow steps
        if (inquiry.getStatus() == MilkInquiry.Status.APPROVED) {
            // Skip DELIVERED and INVOICED steps
            inquiry.setStatus(MilkInquiry.Status.DELIVERED);
            inquiry.setDeliveredAt(LocalDateTime.now());

            // Generate invoice number if not exists
            if (inquiry.getInvoiceNo() == null) {
                inquiry.setInvoiceNo(generateInvoiceNo());
                inquiry.setInvoicedAt(LocalDateTime.now());
            }
            inquiry.setStatus(MilkInquiry.Status.INVOICED);
        } else if (inquiry.getStatus() == MilkInquiry.Status.DELIVERED) {
            // Skip INVOICED step
            if (inquiry.getInvoiceNo() == null) {
                inquiry.setInvoiceNo(generateInvoiceNo());
                inquiry.setInvoicedAt(LocalDateTime.now());
            }
            inquiry.setStatus(MilkInquiry.Status.INVOICED);
        }

        // Generate receipt number
        String receiptNo = generateReceiptNo();

        inquiry.setReceiptNo(receiptNo);
        inquiry.setPaidAt(LocalDateTime.now());
        inquiry.setPaymentConfirmedBy(accountant);
        inquiry.setStatus(MilkInquiry.Status.PAID);

        inquiry = inquiryRepository.save(inquiry);

        // Sync the linked MilkSale (if one exists) so Sales-side pages — Orders,
        // the order invoice eligibility check — reflect that payment is confirmed,
        // instead of staying frozen on PENDING forever.
        syncSalePaymentReceived(inquiry.getReferenceNo());

        // Update COFA (Cooperative Fund Account) and log transaction
        updateCofa(inquiry.getTotalAmount(), inquiry.getReferenceNo(), inquiry.getCompanyName(), accountant);

        return inquiry;
    }

    /**
     * Generate unique receipt number (RCP-2026-0001 format).
     */
    private String generateReceiptNo() {
        int year = LocalDate.now().getYear();
        String prefix = "RCP-" + year + "-";

        List<MilkInquiry> allInquiries = inquiryRepository.findAll();
        int maxNum = 0;

        for (MilkInquiry inq : allInquiries) {
            if (inq.getReceiptNo() != null && inq.getReceiptNo().startsWith(prefix)) {
                try {
                    int num = Integer.parseInt(inq.getReceiptNo().substring(prefix.length()));
                    if (num > maxNum) maxNum = num;
                } catch (NumberFormatException ignored) {}
            }
        }

        return String.format("%s%04d", prefix, maxNum + 1);
    }

    /**
     * Update COFA with milk sale revenue and log the transaction.
     */
    private void updateCofa(BigDecimal amount, String referenceNo, String companyName, User accountant) {
        try {
            // Get current COFA or create if doesn't exist
            CooperativeFund cofa = cofaRepository.findTopByOrderByIdDesc()
                    .orElseGet(() -> {
                        CooperativeFund newCofa = new CooperativeFund();
                        newCofa.setBalance(BigDecimal.ZERO);
                        return newCofa;
                    });

            // Add milk sale revenue to COFA
            BigDecimal newBalance = cofa.getBalance().add(amount);
            cofa.setBalance(newBalance);
            cofa.setUpdatedAt(LocalDateTime.now());
            cofaRepository.save(cofa);

            // Log the transaction
            CooperativeFundTransaction transaction = CooperativeFundTransaction.builder()
                    .transactionType(CooperativeFundTransaction.TransactionType.MILK_SALE)
                    .amount(amount)
                    .balanceAfter(newBalance)
                    .referenceNo(referenceNo)
                    .description("Milk sale payment from " + companyName)
                    .performedBy(accountant)
                    .createdAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(transaction);

        } catch (Exception e) {
            // Log error but don't fail the payment confirmation
            System.err.println("Failed to update COFA: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get inquiries awaiting payment confirmation (for Accountant).
     */
    public List<MilkInquiry> getInquiriesAwaitingPayment() {
        return inquiryRepository.findByStatusOrderBySubmittedAtDesc(MilkInquiry.Status.INVOICED);
    }

    /**
     * Get all paid inquiries (for reports/receipts).
     */
    public List<MilkInquiry> getPaidInquiries() {
        return inquiryRepository.findByStatusOrderBySubmittedAtDesc(MilkInquiry.Status.PAID);
    }

    /**
     * Get approved inquiries awaiting delivery confirmation (for Sales dashboard).
     */
    public List<MilkInquiry> getInquiriesAwaitingDelivery() {
        return inquiryRepository.findByStatusOrderBySubmittedAtDesc(MilkInquiry.Status.APPROVED);
    }

    /**
     * Get delivered inquiries awaiting invoice (for Sales).
     * Returns orders that have been delivered but not yet invoiced.
     */
    public List<MilkInquiry> getDeliveredInquiriesAwaitingInvoice() {
        return inquiryRepository.findDeliveredAwaitingInvoice();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SALES - STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get orders statistics.
     */
    public Map<String, Object> getOrdersStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<MilkSale> allOrders = saleRepository.findAll();

        stats.put("totalOrders", allOrders.size());

        BigDecimal totalRevenue = allOrders.stream()
                .map(MilkSale::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        long pendingPayment = allOrders.stream()
                .filter(o -> "PENDING".equals(o.getPaymentStatus()))
                .count();
        stats.put("pendingPayment", pendingPayment);

        return stats;
    }

    /**
     * Get payments statistics.
     */
    public Map<String, Object> getPaymentsStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        List<MilkSale> pending = saleRepository.findByPaymentStatus("PENDING");
        List<MilkSale> received = saleRepository.findByPaymentStatus("RECEIVED");

        stats.put("pendingCount", pending.size());
        stats.put("pendingAmount", pending.stream()
                .map(MilkSale::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Received total
        BigDecimal receivedThisMonth = received.stream()
                .map(MilkSale::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("receivedThisMonth", receivedThisMonth);

        return stats;
    }
}