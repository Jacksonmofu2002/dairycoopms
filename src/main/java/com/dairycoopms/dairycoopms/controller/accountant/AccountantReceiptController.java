package com.dairycoopms.dairycoopms.controller.accountant;

import com.dairycoopms.dairycoopms.model.MilkInquiry;
import com.dairycoopms.dairycoopms.repository.MilkInquiryRepository;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/accountant/receipts")
public class AccountantReceiptController {

    private final MilkInquiryRepository inquiryRepository;
    private final UserRepository        userRepository;

    public AccountantReceiptController(MilkInquiryRepository inquiryRepository,
                                       UserRepository userRepository) {
        this.inquiryRepository = inquiryRepository;
        this.userRepository    = userRepository;
    }

    // ── List paid orders ──────────────────────────────────────────────────────
    @GetMapping
    public String receiptList(@AuthenticationPrincipal UserDetails ud, Model model) {
        userRepository.findByEmail(ud.getUsername())
                .ifPresent(u -> model.addAttribute("user", u));
        List<MilkInquiry> paid = inquiryRepository.findAll().stream()
                .filter(i -> i.getStatus() == MilkInquiry.Status.PAID)
                .sorted((a, b) -> {
                    boolean aSent = a.isReceiptSent(), bSent = b.isReceiptSent();
                    if (aSent != bSent) return aSent ? 1 : -1;
                    if (a.getPaidAt() != null && b.getPaidAt() != null)
                        return b.getPaidAt().compareTo(a.getPaidAt());
                    return 0;
                }).toList();
        model.addAttribute("paidInquiries", paid);
        model.addAttribute("unsentCount",   paid.stream().filter(i -> !i.isReceiptSent()).count());
        model.addAttribute("unsentReceipts", paid.stream().filter(i -> !i.isReceiptSent()).count());
        return "accountant/receipts";
    }

    // ── Preview receipt ───────────────────────────────────────────────────────
    @GetMapping("/{id}/preview")
    public String previewReceipt(@PathVariable UUID id, Model model, RedirectAttributes flash) {
        MilkInquiry inq = inquiryRepository.findById(id).orElse(null);
        if (inq == null || inq.getStatus() != MilkInquiry.Status.PAID) {
            flash.addFlashAttribute("errorMessage", "Payment record not found.");
            return "redirect:/accountant/receipts";
        }
        model.addAttribute("inquiry", inq);
        return "accountant/receipt-preview";
    }

    // ── Sign receipt ──────────────────────────────────────────────────────────
    @PostMapping("/{id}/sign")
    public String signReceipt(@PathVariable UUID id,
                              @AuthenticationPrincipal UserDetails ud,
                              RedirectAttributes flash) {
        var user = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getSignatureData() == null || user.getSignatureData().isBlank()) {
            flash.addFlashAttribute("errorMessage",
                    "No signature saved. Draw and save your signature first.");
            return "redirect:/accountant/receipts/" + id + "/preview";
        }
        inquiryRepository.findById(id).ifPresent(inq -> {
            inq.setSigned(true);
            inq.setSignedAt(LocalDateTime.now());
            inq.setSignedBy(user);
            inquiryRepository.save(inq);
        });
        flash.addFlashAttribute("successMessage", "Receipt signed successfully.");
        return "redirect:/accountant/receipts/" + id + "/preview";
    }

    // ── Send receipt ──────────────────────────────────────────────────────────
    @PostMapping("/{id}/send")
    public String sendReceipt(@PathVariable UUID id, RedirectAttributes flash) {
        MilkInquiry inq = inquiryRepository.findById(id).orElse(null);
        if (inq == null || inq.getStatus() != MilkInquiry.Status.PAID) {
            flash.addFlashAttribute("errorMessage", "Payment record not found.");
            return "redirect:/accountant/receipts";
        }
        if (!inq.isSigned()) {
            flash.addFlashAttribute("errorMessage", "Sign the receipt first before sending.");
            return "redirect:/accountant/receipts/" + id + "/preview";
        }
        if (inq.isReceiptSent()) {
            flash.addFlashAttribute("errorMessage", "Receipt already sent.");
            return "redirect:/accountant/receipts";
        }
        inq.setReceiptSent(true);
        inq.setReceiptSentAt(LocalDateTime.now());
        inquiryRepository.save(inq);
        flash.addFlashAttribute("successMessage",
                "Receipt sent to " + inq.getCompanyName()
                        + " (" + inq.getReferenceNo() + "). "
                        + "Company can now view it from their portal.");
        return "redirect:/accountant/receipts";
    }

    // ── Save signature (AJAX) ─────────────────────────────────────────────────
    @PostMapping("/signature/save")
    @ResponseBody
    public String saveSignature(@RequestParam String signatureData,
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

    // ── Get signature (AJAX) ──────────────────────────────────────────────────
    @GetMapping("/signature/mine")
    @ResponseBody
    public String getMySignature(@AuthenticationPrincipal UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .filter(u -> u.getSignatureData() != null && !u.getSignatureData().isBlank())
                .map(u -> "{\"data\":\"" + u.getSignatureData() + "\"}")
                .orElse("{}");
    }
}