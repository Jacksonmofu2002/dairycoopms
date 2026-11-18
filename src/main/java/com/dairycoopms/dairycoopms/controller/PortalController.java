package com.dairycoopms.dairycoopms.controller;

import com.dairycoopms.dairycoopms.model.MilkInquiry;
import com.dairycoopms.dairycoopms.model.ProcessingCompany;
import com.dairycoopms.dairycoopms.model.PurchaseOrder;
import com.dairycoopms.dairycoopms.model.CooperativeFund;
import com.dairycoopms.dairycoopms.model.CooperativeFundTransaction;
import com.dairycoopms.dairycoopms.repository.MilkInquiryRepository;
import com.dairycoopms.dairycoopms.repository.ProcessingCompanyRepository;
import com.dairycoopms.dairycoopms.repository.CooperativeFundRepository;
import com.dairycoopms.dairycoopms.repository.CooperativeFundTransactionRepository;
import com.dairycoopms.dairycoopms.service.MilkInquiryService;
import com.dairycoopms.dairycoopms.service.PurchaseOrderService;
import com.dairycoopms.dairycoopms.repository.PurchaseOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/portal")
public class PortalController {

    private final MilkInquiryService inquiryService;
    private final PurchaseOrderService purchaseOrderService;
    private final ProcessingCompanyRepository companyRepository;
    private final MilkInquiryRepository inquiryRepository;
    private final CooperativeFundRepository fundRepository;
    private final CooperativeFundTransactionRepository fundTransactionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public PortalController(MilkInquiryService inquiryService,
                            PurchaseOrderService purchaseOrderService,
                            ProcessingCompanyRepository companyRepository,
                            MilkInquiryRepository inquiryRepository,
                            CooperativeFundRepository fundRepository,
                            CooperativeFundTransactionRepository fundTransactionRepository,
                            PurchaseOrderRepository purchaseOrderRepository) {
        this.inquiryService = inquiryService;
        this.purchaseOrderService = purchaseOrderService;
        this.companyRepository = companyRepository;
        this.inquiryRepository = inquiryRepository;
        this.fundRepository = fundRepository;
        this.fundTransactionRepository = fundTransactionRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    // ── Self-registration ─────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerForm() {
        return "portal/register";
    }

    @PostMapping("/register")
    public String registerCompany(@RequestParam String name,
                                  @RequestParam String contactPerson,
                                  @RequestParam String phone,
                                  @RequestParam String email,
                                  @RequestParam(required = false) String address,
                                  RedirectAttributes ra) {
        try {
            boolean exists = companyRepository.findByStatusOrderByNameAsc("ACTIVE").stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(name.trim()));
            if (exists) {
                ra.addFlashAttribute("errorMessage",
                        "A company named '" + name.trim() + "' is already registered. Select it below.");
                return "redirect:/portal";
            }
            ProcessingCompany saved = companyRepository.save(
                    ProcessingCompany.builder()
                            .name(name.trim()).contactPerson(contactPerson.trim())
                            .phone(phone.trim()).email(email.trim().toLowerCase())
                            .address(address != null ? address.trim() : null)
                            .status("ACTIVE").build());
            ra.addFlashAttribute("successMessage", "Welcome! " + saved.getName() + " is now registered.");
            return "redirect:/portal/company/" + saved.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "redirect:/portal/register";
        }
    }

    // ── Home ──────────────────────────────────────────────────────────────────

    @GetMapping({"", "/"})
    public String home(Model model) {
        try {
            model.addAttribute("companies",
                    companyRepository.findByStatusOrderByNameAsc("ACTIVE"));
        } catch (Exception e) {
            model.addAttribute("companies", new ArrayList<>());
        }
        return "portal/index";
    }

    // ── Company dashboard ─────────────────────────────────────────────────────

    @GetMapping("/company/{id}")
    public String companyDashboard(@PathVariable UUID id,
                                   @RequestParam(defaultValue = "0") int page,
                                   Model model,
                                   RedirectAttributes ra) {
        ProcessingCompany company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            ra.addFlashAttribute("errorMessage", "Company not found");
            return "redirect:/portal";
        }

        // Fetch paginated inquiries (5 per page, latest first)
        Pageable pageable = PageRequest.of(page, 5, Sort.by("submittedAt").descending());
        Page<MilkInquiry> inquiryPage =
                inquiryRepository.findByCompanyNameIgnoreCaseOrderBySubmittedAtDesc(company.getName(), pageable);

        // Get all inquiries for stats (unpaginated)
        List<MilkInquiry> allInquiries =
                inquiryRepository.findByCompanyNameIgnoreCaseOrderBySubmittedAtDesc(company.getName());

        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", allInquiries.stream().filter(i -> i.getStatus() == MilkInquiry.Status.PENDING).count());
        stats.put("approved", allInquiries.stream().filter(MilkInquiry::isApproved).count());
        stats.put("total", allInquiries.size());
        stats.put("totalAmount", allInquiries.stream()
                .filter(i -> i.getTotalAmount() != null)
                .map(MilkInquiry::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        model.addAttribute("company", company);
        model.addAttribute("inquiries", inquiryPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", inquiryPage.getTotalPages());
        model.addAttribute("totalItems", inquiryPage.getTotalElements());
        model.addAttribute("stats", stats);
        return "portal/company/dashboard";
    }

    // ── Submit inquiry ────────────────────────────────────────────────────────

    @PostMapping("/company/{id}/inquiry")
    public String submitInquiry(@PathVariable UUID id,
                                @RequestParam BigDecimal requestedLitres,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate preferredDate,
                                @RequestParam(required = false) String frequency,
                                @RequestParam String email,
                                @RequestParam(required = false) String message,
                                RedirectAttributes ra) {
        ProcessingCompany company = companyRepository.findById(id).orElse(null);
        if (company == null) {
            ra.addFlashAttribute("errorMessage", "Company not found");
            return "redirect:/portal";
        }
        try {
            MilkInquiry.Frequency freq = MilkInquiry.Frequency.ONE_TIME;
            if (frequency != null && !frequency.isEmpty()) {
                try {
                    freq = MilkInquiry.Frequency.valueOf(frequency);
                } catch (Exception ignored) {
                }
            }
            MilkInquiry inq = inquiryService.submitInquiry(
                    company.getName(),
                    company.getContactPerson() != null ? company.getContactPerson() : "N/A",
                    null, email.trim().toLowerCase(),
                    company.getPhone() != null ? company.getPhone() : "N/A",
                    requestedLitres, preferredDate, freq, message);
            ra.addFlashAttribute("successMessage", "Inquiry submitted! Reference: " + inq.getReferenceNo());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/portal/company/" + id;
    }

    // ── Create Purchase Order ─────────────────────────────────────────────────

    @GetMapping("/company/{companyId}/purchase-order/create")
    public String showCreatePurchaseOrder(@PathVariable UUID companyId,
                                          @RequestParam UUID inquiryId,
                                          Model model,
                                          RedirectAttributes ra) {
        ProcessingCompany company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            ra.addFlashAttribute("errorMessage", "Company not found");
            return "redirect:/portal";
        }

        MilkInquiry inquiry = inquiryRepository.findById(inquiryId).orElse(null);
        if (inquiry == null) {
            ra.addFlashAttribute("errorMessage", "Inquiry not found");
            return "redirect:/portal/company/" + companyId;
        }

        if (inquiry.getStatus() != MilkInquiry.Status.APPROVED) {
            ra.addFlashAttribute("errorMessage", "Only approved inquiries can have purchase orders created");
            return "redirect:/portal/company/" + companyId;
        }

        model.addAttribute("company", company);
        model.addAttribute("inquiry", inquiry);
        return "portal/company/purchase-orders/create";
    }

    @PostMapping("/company/{companyId}/purchase-order/create")
    public String createPurchaseOrder(@PathVariable UUID companyId,
                                      @RequestParam UUID inquiryId,
                                      @RequestParam BigDecimal unitPrice,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                                      @RequestParam(required = false) String deliveryLocation,
                                      RedirectAttributes ra) {
        try {
            // Call the correct method from PurchaseOrderService
            PurchaseOrder po = purchaseOrderService.createFromInquiry(
                    inquiryId, unitPrice, deliveryDate, deliveryLocation);
            ra.addFlashAttribute("successMessage",
                    "Purchase Order " + po.getPoNumber() + " created successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage",
                    "Error creating purchase order: " + e.getMessage());
        }
        return "redirect:/portal/company/" + companyId;
    }

    // ── Submit Purchase Order ─────────────────────────────────────────────────

    @PostMapping("/company/{companyId}/purchase-order/{inquiryId}/submit")
    public String submitPurchaseOrder(@PathVariable UUID companyId,
                                      @PathVariable UUID inquiryId,
                                      RedirectAttributes ra) {
        try {
            purchaseOrderService.submitPurchaseOrder(inquiryId);
            ra.addFlashAttribute("successMessage", "Purchase Order submitted successfully! Awaiting sales approval.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to submit PO: " + e.getMessage());
        }
        return "redirect:/portal/company/" + companyId;
    }

    @PostMapping(value = "/company/{companyId}/purchase-order/{inquiryId}/submit",
            headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> submitPurchaseOrderAjax(
            @PathVariable UUID companyId, @PathVariable UUID inquiryId) {
        java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
        try {
            purchaseOrderService.submitPurchaseOrder(inquiryId);
            r.put("success", true);
            r.put("message", "Purchase Order submitted successfully! Awaiting sales approval.");
            r.put("newStatus", "PO_SUBMITTED");
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", "Failed to submit PO: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.ok(r);
    }

    // ── View Purchase Order (Company) ─────────────────────────────────────────

    @GetMapping("/company/{companyId}/purchase-order/{inquiryId}/view")
    public String viewPurchaseOrder(@PathVariable UUID companyId,
                                    @PathVariable UUID inquiryId,
                                    Model model) {
        MilkInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found"));

        model.addAttribute("inquiry", inquiry);
        model.addAttribute("companyId", companyId);
        return "portal/company/purchase-orders/view";
    }

    // ── Confirm delivery ──────────────────────────────────────────────────────

    @PostMapping("/company/{companyId}/inquiry/{inquiryId}/confirm-delivery")
    public String confirmDelivery(@PathVariable UUID companyId,
                                  @PathVariable UUID inquiryId,
                                  RedirectAttributes ra) {
        try {
            MilkInquiry inq = inquiryRepository.findById(inquiryId)
                    .orElseThrow(() -> new RuntimeException("Inquiry not found"));
            // Check if PO has been approved by sales
            if (inq.getStatus() != MilkInquiry.Status.PO_APPROVED) {
                ra.addFlashAttribute("errorMessage", "Delivery can only be confirmed after PO is approved by sales.");
                return "redirect:/portal/company/" + companyId;
            }
            // Use PO service to confirm delivery (creates MilkSale, sets deliveredAt)
            purchaseOrderService.confirmDelivery(inquiryId);
            ra.addFlashAttribute("successMessage",
                    "Delivery confirmed successfully! Invoice will be generated by sales.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to confirm delivery: " + e.getMessage());
        }
        return "redirect:/portal/company/" + companyId;
    }

    // ── View invoice ──────────────────────────────────────────────────────────

    @GetMapping("/company/{companyId}/invoice/{inquiryId}")
    public String viewInvoice(@PathVariable UUID companyId,
                              @PathVariable UUID inquiryId,
                              Model model, RedirectAttributes ra) {
        MilkInquiry inq = inquiryRepository.findById(inquiryId).orElse(null);
        if (inq == null) {
            ra.addFlashAttribute("errorMessage", "Inquiry not found");
            return "redirect:/portal/company/" + companyId;
        }
        if (inq.getStatus() != MilkInquiry.Status.INVOICED && inq.getStatus() != MilkInquiry.Status.PAID) {
            ra.addFlashAttribute("errorMessage", "Invoice not yet available for this order.");
            return "redirect:/portal/company/" + companyId;
        }
        if (!inq.isInvoiceSent()) {
            ra.addFlashAttribute("errorMessage",
                    "Your invoice is being prepared and will be available shortly.");
            return "redirect:/portal/company/" + companyId;
        }
        // Load signer details eagerly
        if (inq.isInvoiceSigned() && inq.getInvoiceSignedBy() != null) {
            model.addAttribute("invoiceSignerName", inq.getInvoiceSignedBy().getFullName());
            model.addAttribute("invoiceSignerSignature", inq.getInvoiceSignedBy().getSignatureData());
        }
        model.addAttribute("inquiry", inq);
        model.addAttribute("company", companyRepository.findById(companyId).orElse(null));
        return "portal/company/invoice";
    }

    // ── Submit payment → marks PAID + credits COFA immediately ───────────────

    @PostMapping("/company/{companyId}/inquiry/{inquiryId}/submit-payment")
    public String submitPayment(@PathVariable UUID companyId,
                                @PathVariable UUID inquiryId,
                                @RequestParam String paymentMethod,
                                @RequestParam String transactionRef,
                                @RequestParam(required = false) String paymentNotes,
                                RedirectAttributes ra) {
        try {
            MilkInquiry inquiry = inquiryRepository.findById(inquiryId)
                    .orElseThrow(() -> new RuntimeException("Inquiry not found"));

            // Guard — only INVOICED can be paid
            if (inquiry.getStatus() == MilkInquiry.Status.PAID) {
                ra.addFlashAttribute("errorMessage", "This order has already been paid.");
                return "redirect:/portal/company/" + companyId;
            }
            if (inquiry.getStatus() != MilkInquiry.Status.INVOICED) {
                ra.addFlashAttribute("errorMessage", "Payment can only be submitted for invoiced orders.");
                return "redirect:/portal/company/" + companyId;
            }

            BigDecimal amount = inquiry.getTotalAmount() != null
                    ? inquiry.getTotalAmount() : BigDecimal.ZERO;

            // ── Mark inquiry as PAID ──────────────────────────────────────
            inquiry.setStatus(MilkInquiry.Status.PAID);
            inquiry.setPaidAt(java.time.LocalDateTime.now());
            inquiry.setReceiptNo("RCP-" + LocalDate.now().getYear()
                    + "-" + String.format("%04d", (int) (Math.random() * 9000) + 1000));
            String notes = "Payment Method: " + paymentMethod
                    + " | Ref: " + transactionRef
                    + (paymentNotes != null && !paymentNotes.isBlank() ? " | Notes: " + paymentNotes : "");
            inquiry.setMessage((inquiry.getMessage() != null ? inquiry.getMessage() + "\n" : "") + notes);
            inquiryRepository.save(inquiry);

            // Keep the linked MilkSale (Sales-side Orders/Payments pages) in sync
            inquiryService.syncSalePaymentReceived(inquiry.getReferenceNo());

            // ── Credit COFA ───────────────────────────────────────────────
            CooperativeFund fund = fundRepository.findAll().stream()
                    .findFirst()
                    .orElseGet(() -> {
                        CooperativeFund f = new CooperativeFund();
                        f.setBalance(BigDecimal.ZERO);
                        return fundRepository.save(f);
                    });

            BigDecimal newBalance = fund.getBalance().add(amount);
            fund.setBalance(newBalance);
            fund.setUpdatedAt(java.time.LocalDateTime.now());
            fundRepository.save(fund);

            // ── Record transaction ────────────────────────────────────────
            fundTransactionRepository.save(
                    CooperativeFundTransaction.builder()
                            .transactionType(CooperativeFundTransaction.TransactionType.MILK_SALE)
                            .amount(amount)
                            .balanceAfter(newBalance)
                            .referenceNo(transactionRef)
                            .description("Milk sale — " + inquiry.getCompanyName()
                                    + " | " + inquiry.getReferenceNo() + " | " + paymentMethod)
                            .build());

            ra.addFlashAttribute("successMessage",
                    "Payment of ZMW " + String.format("%,.2f", amount)
                            + " received. Ref: " + transactionRef + ". Your receipt is now available.");

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Failed to process payment: " + e.getMessage());
        }
        return "redirect:/portal/company/" + companyId;
    }

    // ── View receipt ──────────────────────────────────────────────────────────

    @GetMapping("/company/{companyId}/receipt/{inquiryId}")
    public String viewReceipt(@PathVariable UUID companyId,
                              @PathVariable UUID inquiryId,
                              Model model, RedirectAttributes ra) {
        MilkInquiry inq = inquiryRepository.findById(inquiryId).orElse(null);
        if (inq == null) {
            ra.addFlashAttribute("errorMessage", "Inquiry not found");
            return "redirect:/portal/company/" + companyId;
        }
        if (!inq.isPaid()) {
            ra.addFlashAttribute("errorMessage", "Receipt not available — payment not yet confirmed.");
            return "redirect:/portal/company/" + companyId;
        }
        // Receipt only available after accountant explicitly sends it
        if (!inq.isReceiptSent()) {
            ra.addFlashAttribute("errorMessage",
                    "Your receipt is being prepared by our accounts team and will be available shortly.");
            return "redirect:/portal/company/" + companyId;
        }
        model.addAttribute("inquiry", inq);
        model.addAttribute("company", companyRepository.findById(companyId).orElse(null));
        // Force-load signedBy so signature is available in template
        if (inq.isSigned() && inq.getSignedBy() != null) {
            model.addAttribute("signerName", inq.getSignedBy().getFullName());
            model.addAttribute("signerSignature", inq.getSignedBy().getSignatureData());
        }
        return "portal/company/receipt";
    }

    // ── Check status by reference ─────────────────────────────────────────────

    @GetMapping("/status")
    public String checkStatus(@RequestParam(required = false) String ref, Model model) {
        if (ref != null && !ref.trim().isEmpty()) {
            inquiryService.findByReference(ref.trim()).ifPresentOrElse(
                    inq -> model.addAttribute("inquiry", inq),
                    () -> model.addAttribute("errorMessage",
                            "No inquiry found with reference: " + ref.trim().toUpperCase()));
            model.addAttribute("ref", ref.trim().toUpperCase());
        }
        return "portal/status";
    }

    /**
     * Lightweight polling endpoint for the company portal.
     * Called every few seconds by the portal JS to detect status changes
     * across devices (e.g. Sales approves a PO on PC → phone sees it update).
     * Returns only what the portal needs to refresh its UI.
     */
    @GetMapping("/company/{companyId}/status-poll")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>>
    statusPoll(@PathVariable UUID companyId) {

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();

        try {
            ProcessingCompany company = companyRepository.findById(companyId).orElse(null);
            if (company == null) {
                result.put("error", "Company not found");
                return org.springframework.http.ResponseEntity.ok(result);
            }

            // All inquiries for this company — the portal needs to track these
            List<MilkInquiry> inquiries = inquiryRepository.findAll().stream()
                    .filter(i -> company.getName().equalsIgnoreCase(i.getCompanyName()))
                    .sorted((a, b) -> {
                        if (a.getSubmittedAt() == null) return 1;
                        if (b.getSubmittedAt() == null) return -1;
                        return b.getSubmittedAt().compareTo(a.getSubmittedAt());
                    })
                    .collect(java.util.stream.Collectors.toList());

            // Build lightweight status list
            java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
            for (MilkInquiry inq : inquiries) {
                java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
                item.put("id", inq.getId().toString());
                item.put("ref", inq.getReferenceNo());
                item.put("status", inq.getStatus().name());
                item.put("invoiceNo", inq.getInvoiceNo());
                item.put("invoiceSent", inq.isInvoiceSent());
                item.put("receiptSent", inq.isReceiptSent());
                item.put("paidAt", inq.getPaidAt() != null ? inq.getPaidAt().toString() : null);
                item.put("receiptNo", inq.getReceiptNo());

                // PO status if one exists
                purchaseOrderRepository.findByInquiryId(inq.getId()).stream()
                        .findFirst()
                        .ifPresent(po -> {
                            item.put("poStatus", po.getStatus().name());
                            item.put("poNumber", po.getPoNumber());
                        });

                items.add(item);
            }

            result.put("inquiries", items);
            result.put("ts", System.currentTimeMillis());
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return org.springframework.http.ResponseEntity.ok(result);
    }

    @PostMapping(value = "/company/{companyId}/inquiry/{inquiryId}/confirm-delivery",
            headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> confirmDeliveryAjax(
            @PathVariable UUID companyId, @PathVariable UUID inquiryId) {
        java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
        try {
            MilkInquiry inq = inquiryRepository.findById(inquiryId)
                    .orElseThrow(() -> new RuntimeException("Inquiry not found"));
            if (inq.getStatus() != MilkInquiry.Status.PO_APPROVED) {
                r.put("success", false);
                r.put("message", "Delivery can only be confirmed after PO is approved by sales.");
                return org.springframework.http.ResponseEntity.ok(r);
            }
            purchaseOrderService.confirmDelivery(inquiryId);
            r.put("success", true);
            r.put("message", "Delivery confirmed! The Sales team will generate your invoice shortly.");
            r.put("newStatus", "DELIVERED");
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", "Failed to confirm delivery: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.ok(r);
    }

    @PostMapping(value = "/company/{companyId}/inquiry/{inquiryId}/submit-payment",
            headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> submitPaymentAjax(
            @PathVariable UUID companyId, @PathVariable UUID inquiryId,
            @RequestParam String paymentMethod,
            @RequestParam String transactionRef,
            @RequestParam(required = false) String paymentNotes) {
        java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
        try {
            MilkInquiry inquiry = inquiryRepository.findById(inquiryId)
                    .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));

            if (inquiry.getStatus() == MilkInquiry.Status.PAID) {
                r.put("success", false);
                r.put("message", "This order has already been paid.");
                return org.springframework.http.ResponseEntity.ok(r);
            }
            if (inquiry.getStatus() != MilkInquiry.Status.INVOICED) {
                r.put("success", false);
                r.put("message", "Payment can only be submitted for invoiced orders.");
                return org.springframework.http.ResponseEntity.ok(r);
            }

            BigDecimal amount = inquiry.getTotalAmount() != null
                    ? inquiry.getTotalAmount() : BigDecimal.ZERO;

            inquiry.setStatus(MilkInquiry.Status.PAID);
            inquiry.setPaidAt(java.time.LocalDateTime.now());
            inquiry.setReceiptNo("RCP-" + LocalDate.now().getYear()
                    + "-" + String.format("%04d", (int) (Math.random() * 9000) + 1000));
            String notes = "Payment Method: " + paymentMethod
                    + " | Ref: " + transactionRef
                    + (paymentNotes != null && !paymentNotes.isBlank() ? " | Notes: " + paymentNotes : "");
            inquiry.setMessage((inquiry.getMessage() != null ? inquiry.getMessage() + "\n" : "") + notes);
            inquiryRepository.save(inquiry);

            inquiryService.syncSalePaymentReceived(inquiry.getReferenceNo());

            CooperativeFund fund = fundRepository.findAll().stream()
                    .findFirst()
                    .orElseGet(() -> {
                        CooperativeFund f = new CooperativeFund();
                        f.setBalance(BigDecimal.ZERO);
                        return fundRepository.save(f);
                    });

            BigDecimal newBalance = fund.getBalance().add(amount);
            fund.setBalance(newBalance);
            fund.setUpdatedAt(java.time.LocalDateTime.now());
            fundRepository.save(fund);

            fundTransactionRepository.save(
                    CooperativeFundTransaction.builder()
                            .transactionType(CooperativeFundTransaction.TransactionType.MILK_SALE)
                            .amount(amount)
                            .balanceAfter(newBalance)
                            .referenceNo(transactionRef)
                            .description("Milk sale — " + inquiry.getCompanyName()
                                    + " | " + inquiry.getReferenceNo() + " | " + paymentMethod)
                            .build());

            r.put("success", true);
            r.put("message", "Payment of ZMW " + String.format("%,.2f", amount)
                    + " received. Ref: " + transactionRef + ". Your receipt is now available.");
            r.put("newStatus", "PAID");
        } catch (Exception e) {
            r.put("success", false);
            r.put("message", "Failed to submit payment: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.ok(r);
    }
}