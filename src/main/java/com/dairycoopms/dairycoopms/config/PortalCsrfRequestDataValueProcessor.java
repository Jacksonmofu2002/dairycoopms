package com.dairycoopms.dairycoopms.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PortalCsrfRequestDataValueProcessor
 *
 * The public company portal (/portal/**) is intentionally unauthenticated
 * and CSRF-exempt (see SecurityConfig). But Thymeleaf's Spring integration
 * auto-renders a hidden CSRF field on every <form th:action="..."> tag,
 * regardless of whether that path actually enforces CSRF — and resolving
 * that field still tries to create a session to back the token. For the
 * portal, that's both unnecessary and the root cause of "Cannot create a
 * session after the response has been committed" on long pages.
 *
 * This processor makes portal forms true zero-session statelessness:
 * for /portal/** paths it returns an empty hidden-field map without ever
 * touching the CsrfToken attribute, so no session gets created at all.
 * Every other form in the app (Sales, Accountant, Admin, Member) keeps
 * its CSRF hidden field exactly as before — this replicates Spring
 * Security's default CsrfRequestDataValueProcessor for those paths.
 */
@Component
public class PortalCsrfRequestDataValueProcessor implements RequestDataValueProcessor {

    @Override
    public String processAction(HttpServletRequest request, String action, String httpMethod) {
        return action;
    }
    @Override
    public String processFormFieldValue(HttpServletRequest request, String name, String value, String type) {
        return value;
    }

    @Override
    public String processUrl(HttpServletRequest request, String url) {
        return url;
    }

    @Override
    public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {
        if (isPortalRequest(request)) {
            // Never read the CsrfToken attribute here — touching it is what
            // triggers session creation. Skipping it entirely keeps portal
            // requests fully stateless.
            return Collections.emptyMap();
        }

        Object tokenAttr = request.getAttribute(CsrfToken.class.getName());
        if (tokenAttr instanceof CsrfToken token) {
            Map<String, String> hiddenFields = new LinkedHashMap<>(1);
            hiddenFields.put(token.getParameterName(), token.getToken());
            return hiddenFields;
        }
        return Collections.emptyMap();
    }

    private boolean isPortalRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path.startsWith("/portal/");
    }
}