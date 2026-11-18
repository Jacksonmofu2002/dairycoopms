package com.dairycoopms.dairycoopms.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * PortalAwareAuthenticationEntryPoint
 *
 * Spring Security's default behavior sends every unauthenticated request
 * to a single shared loginPage(), regardless of which role's area was
 * being accessed. This overrides that so a session that expires (or a
 * server restart that wipes in-memory sessions) while on /accountant/**,
 * /sales/**, /collection/**, or /member/** sends the user back to THAT
 * portal's login page instead of always landing on the admin login.
 */
@Component
public class PortalAwareAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        String target;
        if (path.startsWith("/accountant") || path.startsWith("/sales") || path.startsWith("/collection")) {
            target = "/staff";
        } else if (path.startsWith("/member")) {
            target = "/member";
        } else {
            target = "/admin";
        }

        response.sendRedirect(request.getContextPath() + target + "?sessionExpired=true");
    }
}