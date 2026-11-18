package com.dairycoopms.dairycoopms.controller;

import com.dairycoopms.dairycoopms.model.MilkInquiry;
import com.dairycoopms.dairycoopms.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Universal live-poll endpoint — one endpoint for all roles.
 * The JS client (dairycoop-live.js) calls this every 5 seconds.
 * Returns a lightweight JSON payload describing what the current user
 * needs to know about right now.  The client decides what to toast,
 * what to reload, and what badge counts to update.
 */
@RestController
@RequestMapping("/api/live")
public class LivePollController {

    private final UserRepository         userRepository;
    private final LoanRepository         loanRepository;
    private final MilkInquiryRepository  inquiryRepository;
    private final PaymentRepository      paymentRepository;
    private final ReportSubmissionRepository reportSubmissionRepository;

    public LivePollController(UserRepository userRepository,
                              LoanRepository loanRepository,
                              MilkInquiryRepository inquiryRepository,
                              PaymentRepository paymentRepository,
                              ReportSubmissionRepository reportSubmissionRepository) {
        this.userRepository   = userRepository;
        this.loanRepository   = loanRepository;
        this.inquiryRepository = inquiryRepository;
        this.paymentRepository = paymentRepository;
        this.reportSubmissionRepository = reportSubmissionRepository;
    }

    @GetMapping("/poll")
    public ResponseEntity<Map<String, Object>> poll(
            @AuthenticationPrincipal UserDetails ud) {

        if (ud == null) return ResponseEntity.status(401).build();

        var user = userRepository.findByEmail(ud.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        String role = user.getRole() != null ? user.getRole() : "";
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("role", role);
        response.put("ts",   System.currentTimeMillis());

        switch (role) {
            case "ACCOUNTS_OFFICER" -> buildAccountantPayload(response);
            case "SALES"            -> buildSalesPayload(response);
            case "ADMIN"            -> buildAdminPayload(response);
            case "MEMBER"           -> buildMemberPayload(response, user.getId());
            case "COLLECTION_OFFICER" -> buildCollectionPayload(response);
            default -> {
                // Company portal users are identified by email on the inquiry
                buildCompanyPayload(response, ud.getUsername());
            }
        }

        return ResponseEntity.ok(response);
    }

    // ── Accountant ────────────────────────────────────────────────────────────
    private void buildAccountantPayload(Map<String, Object> r) {
        long pendingLoans = loanRepository.countByStatus("PENDING");
        long unsentReceipts = inquiryRepository
                .findByStatusOrderBySubmittedAtDesc(MilkInquiry.Status.PAID)
                .stream().filter(i -> !i.isReceiptSent()).count();
        long pendingPayments = inquiryRepository
                .countByStatus(MilkInquiry.Status.INVOICED);

        r.put("badges", Map.of(
                "pendingLoans",    pendingLoans,
                "unsentReceipts",  unsentReceipts,
                "pendingPayments", pendingPayments
        ));

        // Recent inquiries with actionable status for the accountant
        var actionable = inquiryRepository.findByStatusInOrderBySubmittedAtDesc(
                List.of(MilkInquiry.Status.INVOICED,
                        MilkInquiry.Status.PAID,
                        MilkInquiry.Status.PO_SUBMITTED));
        r.put("inquiries", inquirySummaries(actionable));
    }

    // ── Sales ─────────────────────────────────────────────────────────────────
    private void buildSalesPayload(Map<String, Object> r) {
        long pending = inquiryRepository.countByStatus(MilkInquiry.Status.PENDING);
        long poSubmitted = inquiryRepository.countByStatus(MilkInquiry.Status.PO_SUBMITTED);
        r.put("badges", Map.of(
                "pendingInquiries", pending,
                "poSubmitted",      poSubmitted
        ));
        var actionable = inquiryRepository.findByStatusInOrderBySubmittedAtDesc(
                List.of(MilkInquiry.Status.PENDING,
                        MilkInquiry.Status.PO_SUBMITTED,
                        MilkInquiry.Status.DELIVERED));
        r.put("inquiries", inquirySummaries(actionable));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────
    private void buildAdminPayload(Map<String, Object> r) {
        long pendingLoans     = loanRepository.countByStatus("PENDING");
        long pendingInquiries = inquiryRepository.countByStatus(MilkInquiry.Status.PENDING);
        long unreadReports    = reportSubmissionRepository.countByReadByAdminFalseAndSentAtNotNull();
        r.put("badges", Map.of(
                "pendingLoans",     pendingLoans,
                "pendingInquiries", pendingInquiries,
                "unreadReports",    unreadReports
        ));
    }

    // ── Member ────────────────────────────────────────────────────────────────
    private void buildMemberPayload(Map<String, Object> r, java.util.UUID memberId) {
        if (memberId == null) return;
        var loans = loanRepository.findByMemberId(memberId);
        long pendingLoans = loans.stream().filter(l -> "PENDING".equals(l.getStatus())).count();
        long approvedLoans = loans.stream().filter(l -> "APPROVED".equals(l.getStatus())).count();
        var payments = paymentRepository.findByMemberId(memberId);
        long unpaidPayments = payments.stream().filter(p -> "UNPAID".equals(p.getStatus())).count();
        r.put("badges", Map.of(
                "pendingLoans",  pendingLoans,
                "approvedLoans", approvedLoans,
                "unpaidPayments",unpaidPayments
        ));
    }

    // ── Collection Officer ────────────────────────────────────────────────────
    private void buildCollectionPayload(Map<String, Object> r) {
        r.put("badges", Map.of());
    }

    // ── Company portal ────────────────────────────────────────────────────────
    private void buildCompanyPayload(Map<String, Object> r, String email) {
        var inquiries = inquiryRepository.findByEmailIgnoreCaseOrderBySubmittedAtDesc(email);
        r.put("inquiries", inquirySummaries(inquiries));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private List<Map<String, Object>> inquirySummaries(List<MilkInquiry> list) {
        return list.stream()
                .limit(30)
                .map(i -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("id",     i.getId() != null ? i.getId().toString() : "");
                    m.put("ref",    i.getReferenceNo() != null ? i.getReferenceNo() : "");
                    m.put("status", i.getStatus() != null ? i.getStatus().name() : "");
                    m.put("company",i.getCompanyName() != null ? i.getCompanyName() : "");
                    return m;
                }).collect(java.util.stream.Collectors.toList());
    }
}