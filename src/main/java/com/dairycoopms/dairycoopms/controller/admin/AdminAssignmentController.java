package com.dairycoopms.dairycoopms.controller.admin;

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

import java.util.UUID;

/**
 * AdminAssignmentController
 *
 * URLs handled:
 *   GET  /admin/assignments        – list all active assignments
 *   GET  /admin/assignments/assign – show assign form (dropdowns populated)
 *   POST /admin/assignments/assign – execute assignment
 *   POST /admin/assignments/{id}/revoke – revoke an active assignment
 */
@Controller
@RequestMapping("/admin/assignments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAssignmentController {

    private final AdminService   adminService;
    private final UserRepository userRepository;

    public AdminAssignmentController(AdminService adminService,
                                     UserRepository userRepository) {
        this.adminService   = adminService;
        this.userRepository = userRepository;
    }

    private User currentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    // ── GET /admin/assignments ───────────────────────────────────────────────
    @GetMapping
    public String listAssignments(Model model) {
        model.addAttribute("assignments", adminService.getAllActiveAssignments());
        return "admin/assignments/list";
    }

    // ── GET /admin/assignments/assign ────────────────────────────────────────
    @GetMapping("/assign")
    public String showAssignForm(Model model) {
        // Only APPROVED active members can receive a cow
        model.addAttribute("approvedMembers",
                adminService.getMembersByStatus("APPROVED"));
        // Only cows with no active assignment
        model.addAttribute("availableCows",
                adminService.getUnassignedCows());
        return "admin/assignments/assign";
    }

    // ── POST /admin/assignments/assign ───────────────────────────────────────
    @PostMapping("/assign")
    public String assignCow(@RequestParam UUID cowId,
                            @RequestParam UUID memberId,
                            @AuthenticationPrincipal UserDetails ud,
                            RedirectAttributes flash) {
        try {
            adminService.assignCowToMember(cowId, memberId, currentUser(ud));
            flash.addFlashAttribute("successMessage",
                    "Cow assigned to member successfully.");
            return "redirect:/admin/assignments";

        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/assignments/assign";
        }
    }

    // ── POST /admin/assignments/{id}/revoke ──────────────────────────────────
    @PostMapping("/{id}/revoke")
    public String revokeAssignment(@PathVariable UUID id,
                                   @RequestParam(defaultValue = "Revoked by admin") String reason,
                                   @AuthenticationPrincipal UserDetails ud,
                                   RedirectAttributes flash) {
        try {
            adminService.revokeCowAssignment(id, reason, currentUser(ud));
            flash.addFlashAttribute("successMessage", "Assignment revoked successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/assignments";
    }
}