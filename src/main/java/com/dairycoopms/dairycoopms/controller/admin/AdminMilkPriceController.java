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

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/milk-price")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMilkPriceController {

    private final AdminService   adminService;
    private final UserRepository userRepository;

    public AdminMilkPriceController(AdminService adminService,
                                    UserRepository userRepository) {
        this.adminService   = adminService;
        this.userRepository = userRepository;
    }

    private User currentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public String milkPricePage(@AuthenticationPrincipal UserDetails ud, Model model) {
        // getCurrentMilkPrice throws if no price set - handle gracefully
        try {
            model.addAttribute("currentPrice", adminService.getCurrentMilkPrice());
        } catch (Exception e) {
            model.addAttribute("currentPrice", null);
        }
        try {
            model.addAttribute("priceHistory", adminService.getMilkPriceHistory());
        } catch (Exception e) {
            model.addAttribute("priceHistory", java.util.Collections.emptyList());
        }
        return "admin/milk-price/index";
    }

    @PostMapping("/set")
    public String setPrice(@AuthenticationPrincipal UserDetails ud,
                           @RequestParam BigDecimal price,
                           @RequestParam(required = false) String notes,
                           RedirectAttributes flash) {
        try {
            adminService.setMilkPrice(price, notes, currentUser(ud));
            flash.addFlashAttribute("successMessage",
                    "Milk price set to ZMW " + price + " per litre successfully.");
        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/milk-price";
    }
}