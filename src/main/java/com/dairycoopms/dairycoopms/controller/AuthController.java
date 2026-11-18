package com.dairycoopms.dairycoopms.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * AuthController
 *
 * Redirects each user to their role-specific dashboard after login.
 *
 * ROLE → DASHBOARD:
 *   ADMIN              → /admin/dashboard
 *   COLLECTION_OFFICER → /collection/dashboard
 *   ACCOUNTS_OFFICER   → /accountant/dashboard
 *   SALES              → /sales/dashboard
 */
@Controller
public class AuthController {

    /**
     * Root — redirect to admin login by default.
     * Each portal has its own dedicated URL:
     *   /admin   → Admin login
     *   /staff   → Staff login  (Accountant, Sales, Collection Officer)
     *   /member  → Member login
     *   /company → Company portal (no login required)
     */
    @GetMapping("/")
    public String root(Authentication auth) {
        if (auth != null && auth.isAuthenticated())
            return "redirect:/dashboard";
        return "redirect:/admin";
    }

    /** Admin portal — dedicated URL */
    @GetMapping("/admin")
    public String adminLogin(Authentication auth) {
        if (auth != null && auth.isAuthenticated())
            return "redirect:/dashboard";
        return "auth/login";
    }

    /** Staff portal — dedicated URL (accountant, sales, collection officer) */
    @GetMapping("/staff")
    public String staffLogin(Authentication auth) {
        if (auth != null && auth.isAuthenticated())
            return "redirect:/dashboard";
        return "auth/staff-login";
    }

    /** Member portal — dedicated URL */
    @GetMapping("/member")
    public String memberLogin(Authentication auth) {
        if (auth != null && auth.isAuthenticated())
            return "redirect:/dashboard";
        return "auth/member-login";
    }

    // Keep the old login routes working so Spring Security's
    // form login processing still functions correctly
    @GetMapping("/login")
    public String loginPage(Authentication auth) {
        if (auth != null && auth.isAuthenticated())
            return "redirect:/dashboard";
        return "auth/login";
    }

    @GetMapping("/member/login")
    public String memberLoginPage(Authentication auth) {
        if (auth != null && auth.isAuthenticated())
            return "redirect:/dashboard";
        return "auth/member-login";
    }

    /** Collection Officer portal — shares the staff login page */
    @GetMapping("/collection")
    public String collectionLogin(Authentication auth) {
        if (auth != null && auth.isAuthenticated())
            return "redirect:/dashboard";
        return "auth/staff-login";
    }

    /** Accounts Officer portal — shares the staff login page */
    @GetMapping("/accountant")
    public String accountantLogin(Authentication auth) {
        if (auth != null && auth.isAuthenticated())
            return "redirect:/dashboard";
        return "auth/staff-login";
    }

    /** Sales portal — shares the staff login page */
    @GetMapping("/sales")
    public String salesLogin(Authentication auth) {
        if (auth != null && auth.isAuthenticated())
            return "redirect:/dashboard";
        return "auth/staff-login";
    }

    @GetMapping("/staff/login")
    public String staffLoginPage(Authentication auth) {
        if (auth != null && auth.isAuthenticated())
            return "redirect:/dashboard";
        return "auth/staff-login";
    }

    @GetMapping("/dashboard")
    public String dashboardRedirect(Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return "redirect:/login";

        if (has(auth, "ROLE_ADMIN"))              return "redirect:/admin/dashboard";
        if (has(auth, "ROLE_COLLECTION_OFFICER")) return "redirect:/collection/dashboard";
        if (has(auth, "ROLE_ACCOUNTS_OFFICER"))   return "redirect:/accountant/dashboard";
        if (has(auth, "ROLE_SALES"))              return "redirect:/sales/dashboard";
        if (has(auth, "ROLE_MEMBER"))             return "redirect:/member/dashboard";

        return "redirect:/login?error=true";
    }

    private boolean has(Authentication auth, String role) {
        return auth.getAuthorities().contains(new SimpleGrantedAuthority(role));
    }
}