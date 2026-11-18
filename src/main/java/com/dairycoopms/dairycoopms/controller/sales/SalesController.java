package com.dairycoopms.dairycoopms.controller.sales;

import com.dairycoopms.dairycoopms.model.*;
import com.dairycoopms.dairycoopms.repository.MilkInquiryRepository;
import com.dairycoopms.dairycoopms.repository.PurchaseOrderRepository;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import com.dairycoopms.dairycoopms.service.MilkInquiryService;
import com.dairycoopms.dairycoopms.service.PurchaseOrderService;
import com.dairycoopms.dairycoopms.service.SalesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * SalesController - Inquiry-Based Workflow
 *
 * Handles all Sales Officer endpoints under /sales/**
 *
 * NEW WORKFLOW:
 *   - Dashboard shows pending inquiries and inventory overview
 *   - Inquiries come from public portal (companies submit requests)
 *   - Sales officer reviews, approves/rejects inquiries
 *   - Approved inquiries become orders with invoices
 *   - Track payments and send to accountant for COFA transfer
 */
@Controller
@RequestMapping("/sales")
@PreAuthorize("hasRole('SALES')")
public class SalesController {

    private final MilkInquiryService     inquiryService;
    private final SalesService           salesService;
    private final PurchaseOrderService   purchaseOrderService;
    private final UserRepository         userRepository;
    private final MilkInquiryRepository  milkInquiryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public SalesController(MilkInquiryService inquiryService,
                           SalesService salesService,
                           PurchaseOrderService purchaseOrderService,
                           UserRepository userRepository,
                           MilkInquiryRepository milkInquiryRepository,
                           PurchaseOrderRepository purchaseOrderRepository) {
        this.inquiryService         = inquiryService;
        this.salesService           = salesService;
        this.purchaseOrderService   = purchaseOrderService;
        this.userRepository         = userRepository;
        this.milkInquiryRepository  = milkInquiryRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    /**
     * Add navigation badge counts to all sales pages
     * This ensures header notifications show on every page
     */
    @ModelAttribute
    public void addNavigationCounts(@AuthenticationPrincipal UserDetails ud, Model model) {
        // Count pending inquiries for header badge
        long pendingInquiries = milkInquiryRepository.findAll().stream()
                .filter(i -> i.getStatus() == MilkInquiry.Status.PENDING)
                .count();
        model.addAttribute("pendingInquiries", pendingInquiries);

        // Purchase orders submitted by companies, awaiting Sales review
        long submittedPOCount = purchaseOrderRepository.countByStatus(PurchaseOrder.Status.SUBMITTED);
        model.addAttribute("submittedPOCount", submittedPOCount);

        // Deliveries confirmed but not yet invoiced
        long awaitingInvoiceCount = inquiryService.getDeliveredInquiriesAwaitingInvoice().size();
        model.addAttribute("awaitingInvoiceCount", awaitingInvoiceCount);

        // Invoices generated but not yet sent to the company
        long unsentInvoiceCount = milkInquiryRepository.findAll().stream()
                .filter(i -> (i.getStatus() == MilkInquiry.Status.INVOICED || i.getStatus() == MilkInquiry.Status.PAID)
                        && !i.isInvoiceSent())
                .count();
        model.addAttribute("unsentInvoiceCount", unsentInvoiceCount);

        // Invoices sent and awaiting payment from the company
        long awaitingPaymentCount = inquiryService.getInquiriesAwaitingPayment().size();
        model.addAttribute("awaitingPaymentCount", awaitingPaymentCount);

        // Resolve the current sales officer's display name for the nav chip,
        // so every page shows a name instead of falling back to the login email
        if (ud != null) {
            userRepository.findByEmail(ud.getUsername())
                    .ifPresent(u -> model.addAttribute("currentUserName", u.getFullName()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {

        userRepository.findByEmail(ud.getUsername()).ifPresent(u ->
                model.addAttribute("mustChangePassword", u.isMustChangePassword()));

        Map<String, Object> summary = inquiryService.getDashboardSummary();
        model.addAllAttributes(summary);

        // Get invoiced inquiries that have payment submissions (companies submitted payment info)
        List<MilkInquiry> invoicedInquiries = inquiryService.getInquiriesAwaitingPayment();
        long paymentsAwaitingConfirmation = invoicedInquiries.stream()
                .filter(inq -> inq.getMessage() != null && inq.getMessage().contains("PAYMENT SUBMITTED"))
                .count();

        model.addAttribute("paymentsAwaitingConfirmation", paymentsAwaitingConfirmation);

        return "sales/dashboard";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INQUIRIES
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/inquiries")
    public String listInquiries(@RequestParam(required = false) String status, Model model) {

        MilkInquiry.Status filterStatus = null;
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            try {
                filterStatus = MilkInquiry.Status.valueOf(status);
            } catch (IllegalArgumentException ignored) {}
        }

        List<MilkInquiry> inquiries = inquiryService.getAllInquiries(filterStatus);
        Map<String, Long> counts = inquiryService.getStatusCounts();

        model.addAttribute("inquiries", inquiries);
        model.addAttribute("status", status);
        model.addAttribute("totalCount", counts.get("total"));
        model.addAttribute("pendingCount", counts.get("pending"));
        model.addAttribute("approvedCount", counts.get("approved"));
        model.addAttribute("rejectedCount", counts.get("rejected"));

        return "sales/inquiries/list";
    }

    @GetMapping("/inquiries/{id}")
    public String viewInquiry(@PathVariable UUID id, Model model, RedirectAttributes ra) {

        return inquiryService.getInquiryById(id)
                .map(inquiry -> {
                    model.addAttribute("inquiry", inquiry);
                    model.addAttribute("availableLitres", inquiryService.getAvailableLitres());
                    return "sales/inquiries/detail";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("errorMessage", "Inquiry not found");
                    return "redirect:/sales/inquiries";
                });
    }

    // Replace approveInquiry(...) return to redirect to the new detail page
    @PostMapping("/inquiries/{id}/approve")
    public String approveInquiry(
            @PathVariable UUID id,
            @RequestParam BigDecimal pricePerLitre,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {

        try {
            User salesOfficer = userRepository.findByEmail(ud.getUsername()).orElse(null);
            UUID salesOfficerId = salesOfficer != null ? salesOfficer.getId() : null;

            // Check available inventory before approval
            BigDecimal availableLitres = inquiryService.getAvailableLitres();

            MilkSale sale = inquiryService.approveInquiry(id, pricePerLitre, salesOfficerId);

            // Check if inventory is running low after approval
            BigDecimal remainingLitres = inquiryService.getAvailableLitres();
            BigDecimal lowInventoryThreshold = new BigDecimal("500"); // 500 litres threshold

            String successMsg = String.format("Inquiry %s approved at ZMW %s/L. The company can now submit a Purchase Order to proceed.",
                    sale.getReferenceNo(), pricePerLitre);

            if (remainingLitres.compareTo(lowInventoryThreshold) < 0) {
                successMsg += " ⚠️ WARNING: Low inventory! Only " + remainingLitres + " L remaining.";
            }

            ra.addFlashAttribute("successMessage", successMsg);

            // Redirect to order detail (not invoice) so approving doesn't render an invoice
            return "redirect:/sales/orders/" + sale.getId() + "/detail";

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to approve: " + e.getMessage());
            return "redirect:/sales/inquiries/" + id;
        }
    }

    @PostMapping("/inquiries/{id}/reject")
    public String rejectInquiry(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails ud,
            RedirectAttributes ra) {

        try {
            User salesOfficer = userRepository.findByEmail(ud.getUsername()).orElse(null);
            UUID salesOfficerId = salesOfficer != null ? salesOfficer.getId() : null;

            MilkInquiry inquiry = inquiryService.rejectInquiry(id, reason, salesOfficerId);

            ra.addFlashAttribute("successMessage",
                    String.format("Inquiry %s rejected", inquiry.getReferenceNo()));

            return "redirect:/sales/inquiries";

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to reject: " + e.getMessage());
            return "redirect:/sales/inquiries/" + id;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DELIVERED ORDERS (Awaiting Invoice Generation)
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/delivered")
    public String listDeliveredOrders(Model model) {
        List<MilkInquiry> delivered = inquiryService.getDeliveredInquiriesAwaitingInvoice();
        List<MilkInquiry> invoiced = inquiryService.getInquiriesAwaitingPayment();

        model.addAttribute("deliveredOrders", delivered);
        model.addAttribute("invoicedOrders", invoiced);
        model.addAttribute("deliveredCount", delivered.size());
        model.addAttribute("invoicedCount", invoiced.size());

        return "sales/delivered/list";
    }

    @PostMapping("/delivered/{id}/generate-invoice")
    public String generateInvoice(
            @PathVariable UUID id,
            RedirectAttributes ra) {

        try {
            MilkInquiry inquiry = inquiryService.generateInvoice(id);

            ra.addFlashAttribute("successMessage",
                    String.format("Invoice %s generated for %s! Company can now view and process payment.",
                            inquiry.getInvoiceNo(), inquiry.getReferenceNo()));

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to generate invoice: " + e.getMessage());
        }

        return "redirect:/sales/delivered";
    }

    @GetMapping("/invoice/{id}")
    public String viewGeneratedInvoice(@PathVariable UUID id, Model model, RedirectAttributes ra) {
        // Redirect to the newer invoice preview endpoint
        return "redirect:/sales/invoices/" + id + "/preview";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ORDERS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/orders")
    public String listOrders(Model model) {

        List<MilkSale> orders = inquiryService.getAllOrders();
        Map<String, Object> stats = inquiryService.getOrdersStatistics();

        // MilkSale.processingCompany is always null by design (inquiry-based
        // sales aren't linked to a registered company) — resolve the real
        // company name from the originating inquiry instead.
        Map<String, String> companyNames = new java.util.HashMap<>();
        for (MilkSale order : orders) {
            if (order.getReferenceNo() != null) {
                inquiryService.findByReference(order.getReferenceNo())
                        .ifPresent(inq -> companyNames.put(order.getReferenceNo(), inq.getCompanyName()));
            }
        }

        model.addAttribute("orders", orders);
        model.addAttribute("companyNames", companyNames);
        model.addAllAttributes(stats);

        return "sales/orders/list";
    }

    // Simplified viewOrder: prioritize MilkSale (order) then inquiry
    @GetMapping("/orders/{id}")
    public String viewOrder(@PathVariable UUID id, RedirectAttributes ra) {
        // First try to find an actual order
        var orderOpt = inquiryService.getOrderById(id);
        if (orderOpt.isPresent()) {
            // show non-invoice order summary
            return "redirect:/sales/orders/" + id + "/detail";
        }

        // Otherwise if it's an inquiry id, keep existing inquiry-handling logic
        var inquiryOpt = inquiryService.getInquiryById(id);
        if (inquiryOpt.isPresent()) {
            MilkInquiry inq = inquiryOpt.get();

            if (inq.getStatus() == MilkInquiry.Status.PO_APPROVED ||
                    inq.getStatus() == MilkInquiry.Status.PO_SUBMITTED) {
                return "redirect:/sales/purchase-orders/" + id + "/review";
            }

            if (inq.getInvoiceNo() != null) {
                return "redirect:/sales/invoices/" + id + "/preview";
            }

            return "redirect:/sales/inquiries/" + id;
        }

        ra.addFlashAttribute("errorMessage", "Order not found");
        return "redirect:/sales/dashboard";
    }

    // New: order detail page (non-invoice)
    @GetMapping("/orders/{id}/detail")
    public String orderDetail(@PathVariable UUID id, Model model, RedirectAttributes ra) {
        return inquiryService.getOrderById(id)
                .map(order -> {
                    model.addAttribute("order", order);
                    return "sales/orders/detail";
                })
                .orElseGet(() -> {
                    // If not a sale, check if it's an inquiry and redirect to the inquiry detail
                    return inquiryService.getInquiryById(id)
                            .map(inq -> "redirect:/sales/inquiries/" + id)
                            .orElseGet(() -> {
                                ra.addFlashAttribute("errorMessage", "Order not found");
                                return "redirect:/sales/orders";
                            });
                });
    }

    @GetMapping("/orders/{id}/invoice")
    public String viewInvoice(@PathVariable UUID id, Model model, RedirectAttributes ra) {
        return inquiryService.getOrderById(id)
                .map(order -> {
                    // Eligibility check: only show invoice when order is delivered/paid (adjust as needed)
                    boolean invoiceReady = "RECEIVED".equals(order.getPaymentStatus())
                            || order.getPaymentReceivedAt() != null;

                    if (!invoiceReady) {
                        ra.addFlashAttribute("warning", "Invoice is not available until delivery/payment is completed.");
                        return "redirect:/sales/orders/" + id + "/detail";
                    }

                    // Add the order to the model
                    model.addAttribute("order", order);

                    // Optionally add the original inquiry if your invoice template needs it.
                    // inquiryService.findByReference(...) was used elsewhere, so if available:
                    inquiryService.findByReference(order.getReferenceNo())
                            .ifPresent(inq -> model.addAttribute("inquiry", inq));

                    // If you need the current user in the template, add it here as well (like in other handlers).
                    return "sales/orders/invoice";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("errorMessage", "Order not found");
                    return "redirect:/sales/orders";
                });
    }

    @PostMapping("/orders/{id}/mark-paid")
    public String markOrderPaid(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            MilkSale sale = inquiryService.markPaymentReceived(id);
            ra.addFlashAttribute("successMessage",
                    String.format("Payment of ZMW %s marked as received! Sent to Accountant for approval.",
                            sale.getTotalRevenue()));
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to update payment: " + e.getMessage());
        }
        return "redirect:/sales/payments";
    }

    @PostMapping("/orders/{id}/email")
    public String emailInvoice(
            @PathVariable UUID id,
            @RequestParam String email,
            @RequestParam(required = false) String message,
            RedirectAttributes ra) {

        try {
            // Mark inquiry as invoiced using reference number
            inquiryService.getOrderById(id).ifPresent(order -> {
                if (order.getReferenceNo() != null && order.getReferenceNo().startsWith("INQ-")) {
                    inquiryService.findByReference(order.getReferenceNo())
                            .ifPresent(inq -> inquiryService.generateInvoice(inq.getId()));
                }
            });

            ra.addFlashAttribute("successMessage",
                    "Invoice sent to " + email + " (Email functionality to be implemented)");

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to send email: " + e.getMessage());
        }

        return "redirect:/sales/orders/" + id;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PAYMENTS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/payments")
    public String payments(@RequestParam(required = false) String status, Model model) {

        String filterStatus = (status == null || status.isEmpty()) ? "PENDING" : status;

        // "PENDING" = invoice sent, awaiting payment (INVOICED)
        // "RECEIVED" = payment confirmed (PAID)
        // "ALL" = both
        List<MilkInquiry> payments;
        if ("ALL".equals(filterStatus)) {
            payments = milkInquiryRepository.findByStatusInOrderBySubmittedAtDesc(
                    List.of(MilkInquiry.Status.INVOICED, MilkInquiry.Status.PAID));
        } else if ("RECEIVED".equals(filterStatus)) {
            payments = milkInquiryRepository.findByStatusOrderBySubmittedAtDesc(MilkInquiry.Status.PAID);
        } else {
            payments = milkInquiryRepository.findByStatusOrderBySubmittedAtDesc(MilkInquiry.Status.INVOICED);
        }

        // Stats are always computed across the full invoiced+paid set, regardless of the active tab
        List<MilkInquiry> allInvoicedOrPaid = milkInquiryRepository.findByStatusInOrderBySubmittedAtDesc(
                List.of(MilkInquiry.Status.INVOICED, MilkInquiry.Status.PAID));

        long pendingCount = allInvoicedOrPaid.stream()
                .filter(i -> i.getStatus() == MilkInquiry.Status.INVOICED)
                .count();

        BigDecimal pendingAmount = allInvoicedOrPaid.stream()
                .filter(i -> i.getStatus() == MilkInquiry.Status.INVOICED)
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        java.time.LocalDateTime startOfMonth = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
        BigDecimal receivedThisMonth = allInvoicedOrPaid.stream()
                .filter(i -> i.getStatus() == MilkInquiry.Status.PAID
                        && i.getPaidAt() != null
                        && !i.getPaidAt().isBefore(startOfMonth))
                .map(i -> i.getTotalAmount() != null ? i.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("payments", payments);
        model.addAttribute("status", filterStatus);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("pendingAmount", pendingAmount);
        model.addAttribute("receivedThisMonth", receivedThisMonth);

        return "sales/payments";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PURCHASE ORDERS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/purchase-orders")
    public String listPurchaseOrders(@AuthenticationPrincipal UserDetails ud, Model model) {
        userRepository.findByEmail(ud.getUsername())
                .ifPresent(u -> model.addAttribute("user", u));

        List<PurchaseOrder> allPos = purchaseOrderService.getAllPos();

        // Count by status
        long submitted = allPos.stream().filter(po -> po.getStatus() == PurchaseOrder.Status.SUBMITTED).count();
        long approved = allPos.stream().filter(po -> po.getStatus() == PurchaseOrder.Status.APPROVED).count();
        long rejected = allPos.stream().filter(po -> po.getStatus() == PurchaseOrder.Status.REJECTED).count();

        model.addAttribute("purchaseOrders", allPos);
        model.addAttribute("submittedCount", submitted);
        model.addAttribute("approvedCount", approved);
        model.addAttribute("rejectedCount", rejected);

        return "sales/purchase-orders/list";
    }

    @GetMapping("/purchase-orders/{id}/review")
    public String reviewPurchaseOrder(@PathVariable UUID id,
                                      @AuthenticationPrincipal UserDetails ud,
                                      Model model,
                                      RedirectAttributes ra) {
        userRepository.findByEmail(ud.getUsername())
                .ifPresent(u -> model.addAttribute("user", u));

        PurchaseOrder po = purchaseOrderService.getPoById(id);
        if (po == null) {
            ra.addFlashAttribute("errorMessage", "Purchase Order not found");
            return "redirect:/sales/purchase-orders";
        }

        model.addAttribute("po", po);
        model.addAttribute("inquiry", po.getInquiry());

        return "sales/purchase-orders/review";
    }

    @PostMapping("/purchase-orders/{id}/approve")
    public String approvePurchaseOrder(@PathVariable UUID id,
                                       @AuthenticationPrincipal UserDetails ud,
                                       RedirectAttributes ra) {
        try {
            User user = userRepository.findByEmail(ud.getUsername()).orElse(null);
            purchaseOrderService.approvePo(id, user);
            ra.addFlashAttribute("successMessage", "Purchase Order approved successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/sales/purchase-orders";
    }

    @PostMapping("/purchase-orders/{id}/reject")
    public String rejectPurchaseOrder(@PathVariable UUID id,
                                      @RequestParam String reason,
                                      RedirectAttributes ra) {
        try {
            purchaseOrderService.rejectPo(id, reason);
            ra.addFlashAttribute("successMessage", "Purchase Order rejected");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/sales/purchase-orders";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROCESSING COMPANIES
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/companies")
    public String listCompanies(Model model) {

        List<ProcessingCompany> companies = salesService.getAllCompanies();

        long activeCount = companies.stream()
                .filter(c -> "ACTIVE".equals(c.getStatus()))
                .count();

        model.addAttribute("companies", companies);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("inactiveCount", companies.size() - activeCount);

        return "sales/companies/list";
    }

    @GetMapping("/companies/add")
    public String addCompanyForm(Model model) {
        model.addAttribute("company", new ProcessingCompany());
        return "sales/companies/add";
    }

    @PostMapping("/companies/add")
    public String addCompany(
            @RequestParam String name,
            @RequestParam(required = false) String contactPerson,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String address,
            RedirectAttributes ra) {

        try {
            ProcessingCompany company = salesService.addCompany(
                    name, contactPerson, phone, email, address);

            ra.addFlashAttribute("successMessage",
                    "Processing company '" + company.getName() + "' added successfully!");

            return "redirect:/sales/companies";

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to add company: " + e.getMessage());
            return "redirect:/sales/companies/add";
        }
    }

    @GetMapping("/companies/{id}/edit")
    public String editCompanyForm(@PathVariable UUID id, Model model, RedirectAttributes ra) {

        return salesService.getCompanyById(id)
                .map(company -> {
                    model.addAttribute("company", company);
                    return "sales/companies/edit";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("errorMessage", "Company not found");
                    return "redirect:/sales/companies";
                });
    }

    @PostMapping("/companies/{id}/edit")
    public String updateCompany(
            @PathVariable UUID id,
            @RequestParam String name,
            @RequestParam(required = false) String contactPerson,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String address,
            RedirectAttributes ra) {

        try {
            ProcessingCompany company = salesService.updateCompany(
                    id, name, contactPerson, phone, email, address);

            ra.addFlashAttribute("successMessage",
                    "Company '" + company.getName() + "' updated successfully!");

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to update company: " + e.getMessage());
        }

        return "redirect:/sales/companies";
    }

    @PostMapping("/companies/{id}/toggle-status")
    public String toggleCompanyStatus(@PathVariable UUID id, RedirectAttributes ra) {

        try {
            ProcessingCompany company = salesService.getCompanyById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Company not found"));

            if ("ACTIVE".equals(company.getStatus())) {
                salesService.deactivateCompany(id);
                ra.addFlashAttribute("successMessage",
                        "Company '" + company.getName() + "' deactivated");
            } else {
                salesService.activateCompany(id);
                ra.addFlashAttribute("successMessage",
                        "Company '" + company.getName() + "' activated");
            }

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to update status: " + e.getMessage());
        }

        return "redirect:/sales/companies";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INVOICE MANAGEMENT — Sales Officer signs and sends invoices
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/invoices")
    public String invoiceList(@AuthenticationPrincipal UserDetails ud,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {
        userRepository.findByEmail(ud.getUsername())
                .ifPresent(u -> model.addAttribute("user", u));

        // Get all invoiced orders (for stats)
        List<MilkInquiry> allInvoiced = milkInquiryRepository.findAll().stream()
                .filter(i -> i.getStatus() == MilkInquiry.Status.INVOICED
                        || i.getStatus() == MilkInquiry.Status.PAID
                        || (i.getStatus() == MilkInquiry.Status.PO_APPROVED && i.getDeliveredAt() != null))
                .sorted((a, b) -> {
                    boolean aSent = a.isInvoiceSent(), bSent = b.isInvoiceSent();
                    if (aSent != bSent) return aSent ? 1 : -1;
                    if (a.getInvoicedAt() != null && b.getInvoicedAt() != null)
                        return b.getInvoicedAt().compareTo(a.getInvoicedAt());
                    return 0;
                }).toList();

        // Pagination (5 per page)
        int pageSize = 5;
        int totalItems = allInvoiced.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);

        List<MilkInquiry> paginatedInvoices = startIndex < totalItems
                ? allInvoiced.subList(startIndex, endIndex)
                : List.of();

        model.addAttribute("invoicedOrders", paginatedInvoices);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("unsentInvoiceCount",
                allInvoiced.stream().filter(i -> !i.isInvoiceSent()).count());
        return "sales/invoices";
    }

    @GetMapping("/invoices/{id}/preview")
    public String previewInvoice(@PathVariable UUID id,
                                 @AuthenticationPrincipal UserDetails ud,
                                 Model model, RedirectAttributes ra) {
        MilkInquiry inq = milkInquiryRepository.findById(id).orElse(null);
        if (inq == null) {
            ra.addFlashAttribute("errorMessage", "Invoice not found.");
            return "redirect:/sales/invoices";
        }
        model.addAttribute("inquiry", inq);
        // Inject current user for nav display
        userRepository.findByEmail(ud.getUsername())
                .ifPresent(u -> model.addAttribute("user", u));
        // Eagerly resolve lazy signer to avoid LazyInitializationException
        if (inq.isInvoiceSigned() && inq.getInvoiceSignedBy() != null) {
            model.addAttribute("invoiceSignerName", inq.getInvoiceSignedBy().getFullName());
            model.addAttribute("invoiceSignerSignature", inq.getInvoiceSignedBy().getSignatureData());
        }
        return "sales/invoice-preview";
    }

    @PostMapping("/invoices/{id}/sign")
    public String signInvoice(@PathVariable UUID id,
                              @AuthenticationPrincipal UserDetails ud,
                              RedirectAttributes ra) {
        var user = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getSignatureData() == null || user.getSignatureData().isBlank()) {
            ra.addFlashAttribute("errorMessage",
                    "No signature saved. Draw and save your signature first.");
            return "redirect:/sales/invoices/" + id + "/preview";
        }
        milkInquiryRepository.findById(id).ifPresent(inq -> {
            inq.setInvoiceSigned(true);
            inq.setInvoiceSignedAt(java.time.LocalDateTime.now());
            inq.setInvoiceSignedBy(user);
            milkInquiryRepository.save(inq);
        });
        ra.addFlashAttribute("successMessage", "Invoice signed successfully.");
        return "redirect:/sales/invoices/" + id + "/preview";
    }

    @PostMapping("/invoices/{id}/send")
    public String sendInvoice(@PathVariable UUID id, RedirectAttributes ra) {
        MilkInquiry inq = milkInquiryRepository.findById(id).orElse(null);
        if (inq == null) {
            ra.addFlashAttribute("errorMessage", "Invoice not found.");
            return "redirect:/sales/invoices";
        }
        if (!inq.isInvoiceSigned()) {
            ra.addFlashAttribute("errorMessage", "Sign the invoice first before sending.");
            return "redirect:/sales/invoices/" + id + "/preview";
        }
        if (inq.isInvoiceSent()) {
            ra.addFlashAttribute("errorMessage", "Invoice already sent.");
            return "redirect:/sales/invoices";
        }
        inq.setInvoiceSent(true);
        inq.setInvoiceSentAt(java.time.LocalDateTime.now());
        inq.setInvoicedAt(java.time.LocalDateTime.now());

        // Generate invoice number if not already set (for PO workflow inquiries)
        if (inq.getInvoiceNo() == null || inq.getInvoiceNo().isBlank()) {
            String invoiceNo = inquiryService.generateInvoiceNo();
            inq.setInvoiceNo(invoiceNo);
        }

        // Change status to INVOICED when invoice is sent
        // This activates the Pay Now button on company side
        inq.setStatus(MilkInquiry.Status.INVOICED);

        milkInquiryRepository.save(inq);
        ra.addFlashAttribute("successMessage",
                "Invoice sent to " + inq.getCompanyName()
                        + " (" + inq.getReferenceNo() + "). "
                        + "Company can now view it from their portal.");
        return "redirect:/sales/invoices";
    }

    @PostMapping("/invoices/signature/save")
    @ResponseBody
    public String saveSalesSignature(@RequestParam String signatureData,
                                     @AuthenticationPrincipal UserDetails ud) {
        try {
            var user = userRepository.findByEmail(ud.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setSignatureData(signatureData);
            userRepository.save(user);
            return "ok";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @GetMapping("/invoices/signature/mine")
    @ResponseBody
    public String getSalesSignature(@AuthenticationPrincipal UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .filter(u -> u.getSignatureData() != null && !u.getSignatureData().isBlank())
                .map(u -> "{\"data\":\"" + u.getSignatureData() + "\"}")
                .orElse("{}");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AJAX ENDPOINTS — detected by X-Requested-With: XMLHttpRequest header.
    // Each mirrors its existing redirect counterpart exactly, but returns JSON
    // instead of redirecting, so the page updates in place without reloading.
    // ═══════════════════════════════════════════════════════════════════════════

    /** Builds current badge counts for the nav — included in every AJAX response. */
    private Map<String, Object> buildBadgeUpdates() {
        Map<String, Object> badges = new LinkedHashMap<>();
        badges.put("pendingInquiries",
                milkInquiryRepository.findAll().stream()
                        .filter(i -> i.getStatus() == MilkInquiry.Status.PENDING).count());
        badges.put("submittedPOCount",
                purchaseOrderRepository.countByStatus(PurchaseOrder.Status.SUBMITTED));
        badges.put("awaitingInvoiceCount",
                inquiryService.getDeliveredInquiriesAwaitingInvoice().size());
        badges.put("unsentInvoiceCount",
                milkInquiryRepository.findAll().stream()
                        .filter(i -> (i.getStatus() == MilkInquiry.Status.INVOICED
                                || i.getStatus() == MilkInquiry.Status.PAID)
                                && !i.isInvoiceSent()).count());
        badges.put("awaitingPaymentCount",
                inquiryService.getInquiriesAwaitingPayment().size());
        return badges;
    }

    private Map<String, Object> ok(String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", message);
        r.put("badgeUpdates", buildBadgeUpdates());
        return r;
    }

    private Map<String, Object> err(String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", message);
        return r;
    }

    @PostMapping(value = "/inquiries/{id}/approve", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approveInquiryAjax(
            @PathVariable UUID id,
            @RequestParam BigDecimal pricePerLitre,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            User salesOfficer = userRepository.findByEmail(ud.getUsername()).orElse(null);
            UUID salesOfficerId = salesOfficer != null ? salesOfficer.getId() : null;
            MilkSale sale = inquiryService.approveInquiry(id, pricePerLitre, salesOfficerId);
            Map<String, Object> r = ok(String.format("Inquiry %s approved at ZMW %s/L. Company can now submit a Purchase Order.", sale.getReferenceNo(), pricePerLitre));
            r.put("newStatus", "APPROVED");
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.ok(err("Failed to approve: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/inquiries/{id}/reject", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectInquiryAjax(
            @PathVariable UUID id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            User salesOfficer = userRepository.findByEmail(ud.getUsername()).orElse(null);
            UUID salesOfficerId = salesOfficer != null ? salesOfficer.getId() : null;
            MilkInquiry inquiry = inquiryService.rejectInquiry(id, reason, salesOfficerId);
            Map<String, Object> r = ok(String.format("Inquiry %s rejected.", inquiry.getReferenceNo()));
            r.put("newStatus", "REJECTED");
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.ok(err("Failed to reject: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/purchase-orders/{id}/approve", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> approvePOAjax(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            User user = userRepository.findByEmail(ud.getUsername()).orElse(null);
            purchaseOrderService.approvePo(id, user);
            Map<String, Object> r = ok("Purchase Order approved successfully!");
            r.put("newStatus", "APPROVED");
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.ok(err("Error: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/purchase-orders/{id}/reject", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> rejectPOAjax(
            @PathVariable UUID id,
            @RequestParam String reason) {
        try {
            purchaseOrderService.rejectPo(id, reason);
            Map<String, Object> r = ok("Purchase Order rejected.");
            r.put("newStatus", "REJECTED");
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.ok(err("Error: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/delivered/{id}/generate-invoice", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateInvoiceAjax(@PathVariable UUID id) {
        try {
            MilkInquiry inquiry = inquiryService.generateInvoice(id);
            return ResponseEntity.ok(ok(String.format("Invoice %s generated for %s! Ready to sign and send.", inquiry.getInvoiceNo(), inquiry.getReferenceNo())));
        } catch (Exception e) {
            return ResponseEntity.ok(err("Failed to generate invoice: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/invoices/{id}/sign", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> signInvoiceAjax(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails ud) {
        try {
            var user = userRepository.findByEmail(ud.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (user.getSignatureData() == null || user.getSignatureData().isBlank()) {
                return ResponseEntity.ok(err("No signature saved. Draw and save your signature first."));
            }
            milkInquiryRepository.findById(id).ifPresent(inq -> {
                inq.setInvoiceSigned(true);
                inq.setInvoiceSignedAt(java.time.LocalDateTime.now());
                inq.setInvoiceSignedBy(user);
                milkInquiryRepository.save(inq);
            });
            return ResponseEntity.ok(ok("Invoice signed successfully."));
        } catch (Exception e) {
            return ResponseEntity.ok(err("Failed to sign: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/invoices/{id}/send", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendInvoiceAjax(@PathVariable UUID id) {
        try {
            MilkInquiry inq = milkInquiryRepository.findById(id).orElse(null);
            if (inq == null) return ResponseEntity.ok(err("Invoice not found."));
            if (!inq.isInvoiceSigned()) return ResponseEntity.ok(err("Sign the invoice first before sending."));
            if (inq.isInvoiceSent()) return ResponseEntity.ok(err("Invoice already sent."));
            inq.setInvoiceSent(true);
            inq.setInvoiceSentAt(java.time.LocalDateTime.now());
            inq.setInvoicedAt(java.time.LocalDateTime.now());
            if (inq.getInvoiceNo() == null || inq.getInvoiceNo().isBlank()) {
                inq.setInvoiceNo(inquiryService.generateInvoiceNo());
            }
            inq.setStatus(MilkInquiry.Status.INVOICED);
            milkInquiryRepository.save(inq);
            return ResponseEntity.ok(ok(String.format("Invoice sent to %s. Company can now view and pay.", inq.getCompanyName())));
        } catch (Exception e) {
            return ResponseEntity.ok(err("Failed to send: " + e.getMessage()));
        }
    }
}