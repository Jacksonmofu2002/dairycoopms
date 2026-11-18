package com.dairycoopms.dairycoopms.controller.admin;

import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import com.dairycoopms.dairycoopms.service.AdminService;
import com.dairycoopms.dairycoopms.service.LoanService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * AdminLoanController
 *
 * URLs handled:
 *   GET  /admin/loans           – list pending loans (LOAN_OFFICER role required)
 *   GET  /admin/loans/{id}      – loan detail
 *   POST /admin/loans/{id}/approve – approve loan (LOAN_OFFICER only)
 *   POST /admin/loans/{id}/reject  – reject loan  (LOAN_OFFICER only)
 *
 * NOTE: approveLoan() and rejectLoan() are @PreAuthorize("hasRole('LOAN_OFFICER')")
 * in AdminService. The controller maps them under /admin/** so the ADMIN role
 * can VIEW the loans page, but the actual approve/reject actions will fail
 * at the service layer unless the logged-in user also has LOAN_OFFICER role.
 * In a real deployment, a separate LOAN_OFFICER user handles approvals.
 */
@Controller
@RequestMapping("/admin/loans")
@PreAuthorize("hasRole('ADMIN') or hasRole('LOAN_OFFICER')")
public class AdminLoanController {

    private final AdminService   adminService;
    private final LoanService    loanService;
    private final UserRepository userRepository;

    public AdminLoanController(AdminService adminService,
                               LoanService loanService,
                               UserRepository userRepository) {
        this.adminService   = adminService;
        this.loanService    = loanService;
        this.userRepository = userRepository;
    }

    private User currentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    // ── GET /admin/loans ─────────────────────────────────────────────────────
    @GetMapping
    public String listLoans(@RequestParam(defaultValue = "PENDING") String status,
                            Model model) {
        model.addAttribute("loans",
                "ALL".equals(status)
                        ? loanService.getAllLoans()
                        : loanService.getPendingLoans());
        model.addAttribute("filterStatus",  status);
        model.addAttribute("pendingCount",  loanService.countByStatus("PENDING"));
        model.addAttribute("approvedCount", loanService.countByStatus("APPROVED"));
        model.addAttribute("rejectedCount", loanService.countByStatus("REJECTED"));
        return "admin/loans/list";
    }

    // ── GET /admin/loans/{id} ────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String loanDetail(@PathVariable UUID id, Model model) {
        try {
            model.addAttribute("loan",     adminService.getLoanById(id));
            model.addAttribute("progress", loanService.getRepaymentProgress(id));
            return "admin/loans/detail";

        } catch (IllegalArgumentException e) {
            return "redirect:/admin/loans?error=Loan+not+found";
        }
    }

    // ── POST /admin/loans/{id}/approve ───────────────────────────────────────
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    public String approveLoan(@PathVariable UUID id,
                              @AuthenticationPrincipal UserDetails ud,
                              RedirectAttributes flash) {
        try {
            adminService.approveLoan(id, currentUser(ud));
            flash.addFlashAttribute("successMessage", "Loan approved successfully.");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/loans/" + id;
    }

    // ── POST /admin/loans/{id}/reject ────────────────────────────────────────
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    public String rejectLoan(@PathVariable UUID id,
                             @RequestParam String reason,
                             @AuthenticationPrincipal UserDetails ud,
                             RedirectAttributes flash) {
        try {
            adminService.rejectLoan(id, reason, currentUser(ud));
            flash.addFlashAttribute("successMessage", "Loan rejected.");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/loans/" + id;
    }
}