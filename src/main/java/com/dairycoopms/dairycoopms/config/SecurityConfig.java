package com.dairycoopms.dairycoopms.config;

import com.dairycoopms.dairycoopms.security.CustomUserDetailsService;
import com.dairycoopms.dairycoopms.security.PortalAuthSuccessHandler;
import com.dairycoopms.dairycoopms.security.PortalAwareAuthenticationEntryPoint;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * SecurityConfig
 *
 * URL access rules per role:
 *   /admin/**       → ADMIN only
 *   /collection/**  → COLLECTION_OFFICER only
 *   /accountant/**  → ACCOUNTS_OFFICER only
 *   /sales/**       → SALES only
 *   /register, /login → public
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService            userDetailsService;
    private final PortalAuthSuccessHandler            portalAuthSuccessHandler;
    private final PortalAwareAuthenticationEntryPoint portalAwareEntryPoint;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          PortalAuthSuccessHandler portalAuthSuccessHandler,
                          PortalAwareAuthenticationEntryPoint portalAwareEntryPoint) {
        this.userDetailsService       = userDetailsService;
        this.portalAuthSuccessHandler = portalAuthSuccessHandler;
        this.portalAwareEntryPoint    = portalAwareEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<>(new HttpSessionEventPublisher());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())

                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/portal/**")
                )

                .addFilterAfter((request, response, chain) -> {
                    jakarta.servlet.http.HttpServletRequest httpRequest =
                            (jakarta.servlet.http.HttpServletRequest) request;
                    String path = httpRequest.getRequestURI();
                    String contextPath = httpRequest.getContextPath();
                    if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
                        path = path.substring(contextPath.length());
                    }
                    if (!path.startsWith("/portal/")) {
                        Object token = request.getAttribute(CsrfToken.class.getName());
                        if (token instanceof CsrfToken csrfToken) {
                            csrfToken.getToken();
                        }
                    }
                    chain.doFilter(request, response);
                }, CsrfFilter.class)

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/webjars/**", "/css/**", "/js/**",
                                "/images/**", "/favicon.ico"
                        ).permitAll()

                        .requestMatchers(
                                "/", "/admin", "/staff", "/member", "/company",
                                "/accountant", "/sales", "/collection",
                                "/login", "/register", "/register/success",
                                "/member/register", "/member/register/success", "/error", "/member/login", "/staff/login"
                        ).permitAll()

                        .requestMatchers("/portal/**").permitAll()

                        .requestMatchers("/api/live/**").permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/collection/**").hasRole("COLLECTION_OFFICER")
                        .requestMatchers("/accountant/**").hasRole("ACCOUNTS_OFFICER")
                        .requestMatchers("/sales/**").hasRole("SALES")
                        .requestMatchers("/member/**").hasRole("MEMBER")
                        .requestMatchers("/account/**").authenticated()
                        .requestMatchers("/dashboard").authenticated()
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(portalAuthSuccessHandler)
                        .failureHandler((req, res, ex) -> {
                            String referer = req.getHeader("Referer");
                            String fail = "/admin?error=true";
                            if (referer != null) {
                                if      (referer.contains("/accountant")) fail = "/accountant?error=true";
                                else if (referer.contains("/sales"))      fail = "/sales?error=true";
                                else if (referer.contains("/collection")) fail = "/collection?error=true";
                                else if (referer.contains("/member"))     fail = "/member?error=true";
                                else if (referer.contains("/staff"))      fail = "/staff?error=true";
                            }
                            res.sendRedirect(req.getContextPath() + fail);
                        })
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((req, res, auth) -> {
                            String target = "/admin?logout=true";

                            System.out.println("=== LOGOUT DEBUG START ===");
                            System.out.println("auth is null? " + (auth == null));
                            if (auth != null) {
                                System.out.println("auth.getName() = " + auth.getName());
                                auth.getAuthorities().forEach(a ->
                                        System.out.println("Authority: [" + a.getAuthority() + "]"));

                                boolean isMember     = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_MEMBER"));
                                boolean isAccountant = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_ACCOUNTS_OFFICER"));
                                boolean isSales      = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_SALES"));
                                boolean isCollection = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_COLLECTION_OFFICER"));

                                System.out.println("isMember=" + isMember + " isAccountant=" + isAccountant
                                        + " isSales=" + isSales + " isCollection=" + isCollection);

                                if (isMember)          target = "/member?logout=true";
                                else if (isAccountant) target = "/accountant?logout=true";
                                else if (isSales)      target = "/sales?logout=true";
                                else if (isCollection) target = "/collection?logout=true";
                            }
                            System.out.println("Final redirect target: " + target);
                            System.out.println("=== LOGOUT DEBUG END ===");

                            res.sendRedirect(req.getContextPath() + target);
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .expiredUrl("/login?expired=true")
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(portalAwareEntryPoint)
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendRedirect("/dashboard"))
                );

        return http.build();
    }
}