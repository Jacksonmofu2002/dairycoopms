package com.dairycoopms.dairycoopms.controller;

import com.dairycoopms.dairycoopms.service.UserRegistrationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * RegistrationController
 *
 * Handles ONLY public staff self-registration pages.
 * /admin/users is now handled by AdminStaffController.
 *
 * PUBLIC:
 *   GET  /register         - show registration form
 *   POST /register         - submit registration
 *   GET  /register/success - success page
 */
@Controller
public class RegistrationController {

    private final UserRegistrationService registrationService;

    public RegistrationController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("selectableRoles", registrationService.getSelectableRoles());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@RequestParam String fullName,
                                      @RequestParam String email,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      @RequestParam String roleName,
                                      Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match.");
            model.addAttribute("selectableRoles", registrationService.getSelectableRoles());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("roleName", roleName);
            return "auth/register";
        }
        try {
            registrationService.register(fullName, email, password, roleName);
            return "redirect:/register/success";
        } catch (SecurityException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("securityWarning", true);
            model.addAttribute("selectableRoles", registrationService.getSelectableRoles());
            return "auth/register";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("selectableRoles", registrationService.getSelectableRoles());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("roleName", roleName);
            return "auth/register";
        }
    }

    @GetMapping("/register/success")
    public String registrationSuccess() {
        return "auth/register-success";
    }
}