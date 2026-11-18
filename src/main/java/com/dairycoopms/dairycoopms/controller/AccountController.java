package com.dairycoopms.dairycoopms.controller;

import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import com.dairycoopms.dairycoopms.service.StaffAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AccountController - self-service password change for ALL roles
 *
 * GET  /account/password        - change password page
 * POST /account/password        - submit change
 * POST /account/dismiss-banner  - dismiss the temp password banner
 */
@Controller
@RequestMapping("/account")
public class AccountController {

    private final StaffAccountService staffService;
    private final UserRepository      userRepository;

    public AccountController(StaffAccountService staffService,
                             UserRepository userRepository) {
        this.staffService   = staffService;
        this.userRepository = userRepository;
    }

    @GetMapping("/password")
    public String showChangePassword() {
        return "account/change-password";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails ud,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model,
                                 RedirectAttributes flash) {
        User user = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            staffService.changePassword(user, currentPassword, newPassword, confirmPassword);
            flash.addFlashAttribute("successMessage",
                    "Password changed successfully.");
            // Redirect back to their role dashboard
            return "redirect:/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "account/change-password";
        }
    }

    @PostMapping("/dismiss-banner")
    public String dismissBanner(@AuthenticationPrincipal UserDetails ud,
                                RedirectAttributes flash) {
        User user = userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        staffService.dismissBanner(user);
        return "redirect:/dashboard";
    }
}