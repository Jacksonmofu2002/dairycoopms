package com.dairycoopms.dairycoopms.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * PortalAuthSuccessHandler
 *
 * After successful login, checks that the user used the correct portal:
 *   /login          → ADMIN only
 *   /member/login   → MEMBER only
 *   /staff/login    → COLLECTION_OFFICER, ACCOUNTS_OFFICER, SALES only
 *
 * If a user logs in via the wrong portal they are immediately signed out
 * and redirected back to that portal's login page with ?wrongportal=true
 */
@Component
public class PortalAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication auth) throws IOException {

        String referer = request.getHeader("Referer");
        if (referer == null) referer = "";

        boolean isAdmin   = has(auth, "ROLE_ADMIN");
        boolean isMember  = has(auth, "ROLE_MEMBER");
        boolean isStaff   = has(auth, "ROLE_COLLECTION_OFFICER")
                || has(auth, "ROLE_ACCOUNTS_OFFICER")
                || has(auth, "ROLE_SALES");

        // Determine which portal was used — supports both old (/login, /staff/login)
        // and new clean URLs (/admin, /staff, /member)
        boolean usedAdminPortal  = referer.contains("/admin")
                || (referer.contains("/login")
                && !referer.contains("/member/login")
                && !referer.contains("/staff/login"));
        boolean usedMemberPortal = referer.contains("/member");
        boolean usedStaffPortal  = referer.contains("/staff");

        // Admin used admin portal - OK
        if (isAdmin && usedAdminPortal) {
            response.sendRedirect("/dashboard");
            return;
        }

        // Member used member portal - OK
        if (isMember && usedMemberPortal) {
            response.sendRedirect("/dashboard");
            return;
        }

        // Staff used staff portal - OK
        if (isStaff && usedStaffPortal) {
            response.sendRedirect("/dashboard");
            return;
        }

        // Wrong portal - invalidate session and redirect back with error
        request.getSession().invalidate();

        if (usedAdminPortal) {
            response.sendRedirect("/admin?wrongportal=true");
        } else if (usedMemberPortal) {
            response.sendRedirect("/member?wrongportal=true");
        } else if (usedStaffPortal) {
            response.sendRedirect("/staff?wrongportal=true");
        } else {
            // Fallback - send to correct portal
            if (isAdmin)       response.sendRedirect("/admin");
            else if (isMember) response.sendRedirect("/member");
            else               response.sendRedirect("/staff");
        }
    }

    private boolean has(Authentication auth, String role) {
        return auth.getAuthorities().contains(new SimpleGrantedAuthority(role));
    }
}