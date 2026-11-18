package com.dairycoopms.dairycoopms.controller.admin;

import com.dairycoopms.dairycoopms.model.Member;
import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import com.dairycoopms.dairycoopms.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * AdminMemberController
 *
 * URLs handled:
 *   GET  /admin/members                – list all members (optional ?status= filter)
 *   GET  /admin/members/register       – show registration form
 *   POST /admin/members/register       – save new member
 *   GET  /admin/members/{id}           – member detail page
 *   POST /admin/members/{id}/approve   – approve a PENDING member
 *   POST /admin/members/{id}/reject    – reject a PENDING member (reason required)
 *   POST /admin/members/{id}/deactivate– soft-deactivate an APPROVED member
 *   POST /admin/members/{id}/reactivate– reactivate a deactivated member
 */
@Controller
@RequestMapping("/admin/members")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

    private final AdminService   adminService;
    private final UserRepository userRepository;

    public AdminMemberController(AdminService adminService,
                                 UserRepository userRepository) {
        this.adminService   = adminService;
        this.userRepository = userRepository;
    }

    // ── Helper: resolve logged-in User entity ────────────────────────────────
    private User currentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LIST
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * GET /admin/members
     * Optional query param: ?status=PENDING | APPROVED | REJECTED
     * Without the param, all members are shown.
     */
    @GetMapping
    public String listMembers(@RequestParam(required = false) String status,
                              Model model) {

        List<Member> members = (status != null && !status.isBlank())
                ? adminService.getMembersByStatus(status.toUpperCase())
                : adminService.getAllMembers();

        model.addAttribute("members",       members);
        model.addAttribute("filterStatus",  status);
        model.addAttribute("totalCount",    adminService.getAllMembers().size());
        model.addAttribute("pendingCount",  adminService.getMembersByStatus("PENDING").size());
        model.addAttribute("approvedCount", adminService.getMembersByStatus("APPROVED").size());
        model.addAttribute("rejectedCount", adminService.getMembersByStatus("REJECTED").size());

        return "admin/members/list";
    }

    // ═════════════════════════════════════════════════════════════════════════
    // REGISTER
    // ═════════════════════════════════════════════════════════════════════════

    /** GET /admin/members/register – show blank form */
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("member", new Member());
        return "admin/members/register";
    }

    /**
     * POST /admin/members/register – save member.
     * On validation / duplicate errors, returns to form with error message.
     * On success, redirects to detail page with flash message.
     */
    @PostMapping("/register")
    public String registerMember(@ModelAttribute Member member,
                                 @AuthenticationPrincipal UserDetails ud,
                                 RedirectAttributes flash,
                                 Model model) {
        try {
            Member saved = adminService.registerMember(member, currentUser(ud));
            flash.addFlashAttribute("successMessage",
                    "Member " + saved.getName() + " registered successfully (MEM# "
                            + saved.getMemberNumber() + "). Awaiting approval.");
            return "redirect:/admin/members/" + saved.getId();

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("member", member);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/members/register";
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DETAIL
    // ═════════════════════════════════════════════════════════════════════════

    /** GET /admin/members/{id} – member detail + action buttons */
    @GetMapping("/{id}")
    public String memberDetail(@PathVariable UUID id, Model model) {
        try {
            Member member = adminService.getMemberById(id);
            model.addAttribute("member", member);
            // Loans for this member – shown in a tab
            model.addAttribute("memberLoans", adminService.getLoansByMember(id));
            // Cow assignments for this member
            model.addAttribute("assignments", adminService.getAssignmentsByMember(id));
            return "admin/members/detail";

        } catch (IllegalArgumentException e) {
            return "redirect:/admin/members?error=Member+not+found";
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // APPROVE
    // ═════════════════════════════════════════════════════════════════════════

    /** POST /admin/members/{id}/approve */
    @PostMapping("/{id}/approve")
    public String approveMember(@PathVariable UUID id,
                                @AuthenticationPrincipal UserDetails ud,
                                RedirectAttributes flash) {
        try {
            Member m = adminService.approveMember(id, currentUser(ud));
            flash.addFlashAttribute("successMessage",
                    m.getName() + " has been approved successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/members/" + id;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // REJECT
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * POST /admin/members/{id}/reject
     * Form param: reason (String) – required
     */
    @PostMapping("/{id}/reject")
    public String rejectMember(@PathVariable UUID id,
                               @RequestParam String reason,
                               @AuthenticationPrincipal UserDetails ud,
                               RedirectAttributes flash) {
        try {
            Member m = adminService.rejectMember(id, reason, currentUser(ud));
            flash.addFlashAttribute("successMessage",
                    m.getName() + " has been rejected.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/members/" + id;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DEACTIVATE / REACTIVATE
    // ═════════════════════════════════════════════════════════════════════════

    /** POST /admin/members/{id}/deactivate */
    @PostMapping("/{id}/deactivate")
    public String deactivateMember(@PathVariable UUID id,
                                   @AuthenticationPrincipal UserDetails ud,
                                   RedirectAttributes flash) {
        try {
            adminService.deactivateMember(id, currentUser(ud));
            flash.addFlashAttribute("successMessage", "Member account deactivated.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/members/" + id;
    }

    /** POST /admin/members/{id}/reactivate */
    @PostMapping("/{id}/reactivate")
    public String reactivateMember(@PathVariable UUID id,
                                   @AuthenticationPrincipal UserDetails ud,
                                   RedirectAttributes flash) {
        try {
            adminService.reactivateMember(id, currentUser(ud));
            flash.addFlashAttribute("successMessage", "Member account reactivated.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/members/" + id;
    }
}