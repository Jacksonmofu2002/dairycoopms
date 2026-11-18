package com.dairycoopms.dairycoopms.controller.admin;

import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import com.dairycoopms.dairycoopms.service.StaffAccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AdminStaffController
 *
 * GET  /admin/users           - list all staff accounts
 * GET  /admin/users/new       - form to create new staff account
 * POST /admin/users/new       - create the account
 * POST /admin/users/{id}/deactivate  - deactivate a staff account
 * POST /admin/users/{id}/activate    - reactivate a staff account
 * GET  /admin/users/{id}/edit        - edit staff account form
 * POST /admin/users/{id}/edit        - update staff account
 *
 * Staff self-service (any authenticated user):
 * GET  /account/password      - change password page
 * POST /account/password      - submit password change
 * POST /account/dismiss-banner - dismiss temp password banner
 */
@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/users")
public class AdminStaffController {

    private final StaffAccountService staffService;
    private final UserRepository userRepository;
    private final com.dairycoopms.dairycoopms.repository.RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AdminStaffController(StaffAccountService staffService,
                                UserRepository userRepository,
                                com.dairycoopms.dairycoopms.repository.RoleRepository roleRepository,
                                org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.staffService = staffService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listStaff(Model model) {
        model.addAttribute("staffList", staffService.getAllStaff());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newStaffForm(Model model) {
        // Find which roles are already taken by active staff
        java.util.Set<String> takenRoles = userRepository.findAll().stream()
                .filter(u -> u.isActive() && u.getRole() != null
                        && !u.getRole().equals("ADMIN")
                        && !u.getRole().equals("MEMBER"))
                .map(u -> u.getRole())
                .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("takenRoles", takenRoles);
        return "admin/users/create";
    }

    @PostMapping("/new")
    public String createStaff(@RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam String roleName,
                              @RequestParam String tempPassword,
                              Model model,
                              RedirectAttributes flash) {
        try {
            String emailLower = email.trim().toLowerCase();
            validateEmailFormat(emailLower, roleName);
            User created = staffService.createStaffAccount(
                    fullName, emailLower, roleName, tempPassword);
            flash.addFlashAttribute("successMessage",
                    "Account created for " + created.getFullName() +
                            ". Share the temporary password with them directly.");
            flash.addFlashAttribute("createdEmail", created.getEmail());
            flash.addFlashAttribute("createdPassword", tempPassword);
            return "redirect:/admin/users";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("roleName", roleName);
            java.util.Set<String> takenRoles = userRepository.findAll().stream()
                    .filter(u -> u.isActive() && u.getRole() != null
                            && !u.getRole().equals("ADMIN")
                            && !u.getRole().equals("MEMBER"))
                    .map(u -> u.getRole())
                    .collect(java.util.stream.Collectors.toSet());
            model.addAttribute("takenRoles", takenRoles);
            return "admin/users/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editStaffForm(@PathVariable java.util.UUID id, Model model) {
        var staff = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        model.addAttribute("staff", staff);

        java.util.Set<String> takenRoles = userRepository.findAll().stream()
                .filter(u -> u.isActive() && u.getRole() != null
                        && !u.getRole().equals("ADMIN")
                        && !u.getRole().equals("MEMBER")
                        && !u.getId().equals(id))   // exclude self
                .map(u -> u.getRole())
                .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("takenRoles", takenRoles);
        return "admin/users/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateStaff(@PathVariable java.util.UUID id,
                              @RequestParam String fullName,
                              @RequestParam String email,
                              @RequestParam String roleName,
                              @RequestParam(required = false) String newPassword,
                              Model model,
                              RedirectAttributes flash) {
        var staff = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        try {
            // Full name
            if (!org.springframework.util.StringUtils.hasText(fullName))
                throw new IllegalArgumentException("Full name is required.");
            staff.setFullName(fullName.trim());

            // Email — enforce firstname.role@gmail.com format
            if (!org.springframework.util.StringUtils.hasText(email) || !email.contains("@"))
                throw new IllegalArgumentException("A valid email is required.");
            String emailLower = email.trim().toLowerCase();
            validateEmailFormat(emailLower, roleName);

            // If email changed, check it's not already taken
            if (!emailLower.equals(staff.getEmail()) &&
                    userRepository.existsByEmail(emailLower))
                throw new IllegalStateException("Email '" + email + "' is already in use.");
            staff.setEmail(emailLower);

            // Role — check not taken by someone else
            if (!roleName.equals(staff.getRole())) {
                boolean taken = userRepository.findAll().stream()
                        .anyMatch(u -> roleName.equals(u.getRole())
                                && u.isActive()
                                && !u.getId().equals(id));
                if (taken)
                    throw new IllegalStateException("Role '" + roleName.replace('_', ' ')
                            + "' is already assigned to another active staff member.");
                var role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalStateException("Role not found."));
                staff.setRoleEntity(role);
            }

            // Optional password reset
            if (org.springframework.util.StringUtils.hasText(newPassword)) {
                if (newPassword.length() < 5)
                    throw new IllegalArgumentException("New password must be at least 5 characters.");
                staff.setPasswordHash(passwordEncoder.encode(newPassword));
                staff.setMustChangePassword(true);
            }

            userRepository.save(staff);
            flash.addFlashAttribute("successMessage",
                    "Account updated for " + staff.getFullName() + ".");
            if (org.springframework.util.StringUtils.hasText(newPassword))
                flash.addFlashAttribute("resetPassword", newPassword);
            return "redirect:/admin/users";

        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("staff", staff);
            java.util.Set<String> takenRoles = userRepository.findAll().stream()
                    .filter(u -> u.isActive() && u.getRole() != null
                            && !u.getRole().equals("ADMIN") && !u.getRole().equals("MEMBER")
                            && !u.getId().equals(id))
                    .map(u -> u.getRole())
                    .collect(java.util.stream.Collectors.toSet());
            model.addAttribute("takenRoles", takenRoles);
            return "admin/users/edit";
        }
    }

    /**
     * Enforce firstname.role@gmail.com format
     * Examples:
     *   - jackson.collection@gmail.com (COLLECTION_OFFICER)
     *   - chanda.accounts@gmail.com (ACCOUNTS_OFFICER)
     *   - jane.sales@gmail.com (SALES)
     */
    private void validateEmailFormat(String email, String roleName) {
        // Extract first word of role: "COLLECTION_OFFICER" → "collection"
        String roleShort = roleName.split("_")[0].toLowerCase();

        // Pattern: firstname.role@gmail.com
        String pattern = "^[a-z0-9.]+\\." + roleShort + "@gmail\\.com$";

        if (!email.matches(pattern)) {
            throw new IllegalArgumentException(
                    "Email must follow format: firstname." + roleShort + "@gmail.com " +
                            "(e.g., jackson." + roleShort + "@gmail.com)"
            );
        }
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable java.util.UUID id,
                             RedirectAttributes flash) {
        userRepository.findById(id).ifPresent(u -> {
            u.setActive(false);
            userRepository.save(u);
        });
        flash.addFlashAttribute("successMessage", "Staff account deactivated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable java.util.UUID id,
                           RedirectAttributes flash) {
        userRepository.findById(id).ifPresent(u -> {
            u.setActive(true);
            userRepository.save(u);
        });
        flash.addFlashAttribute("successMessage", "Staff account reactivated.");
        return "redirect:/admin/users";
    }
}