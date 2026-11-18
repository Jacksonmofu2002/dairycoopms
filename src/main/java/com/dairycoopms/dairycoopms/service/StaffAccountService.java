package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.Role;
import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.RoleRepository;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * StaffAccountService
 *
 * Admin creates staff accounts directly.
 * - Admin sets name, email, role, and a temporary password (min 5 chars)
 * - mustChangePassword = true is set so dashboard shows a banner
 * - Staff logs in normally, sees banner, changes password when ready
 * - Staff can dismiss banner and continue working with temp password
 */
@Service
@Transactional
public class StaffAccountService {

    private static final List<String> ALLOWED_STAFF_ROLES =
            List.of("ADMIN", "COLLECTION_OFFICER", "ACCOUNTS_OFFICER", "SALES");

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffAccountService(UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.roleRepository  = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Create staff account (admin only) ────────────────────────────────

    public User createStaffAccount(String fullName, String email,
                                   String roleName, String tempPassword) {

        if (!StringUtils.hasText(fullName))
            throw new IllegalArgumentException("Full name is required.");

        if (!StringUtils.hasText(email) || !email.contains("@"))
            throw new IllegalArgumentException("A valid email address is required.");

        if (!ALLOWED_STAFF_ROLES.contains(roleName))
            throw new IllegalArgumentException("Invalid role selected.");

        // One active staff member per role
        boolean roleAlreadyFilled = userRepository.findAll().stream()
                .anyMatch(u -> roleName.equals(u.getRole()) && u.isActive());
        if (roleAlreadyFilled)
            throw new IllegalStateException(
                    "Role '" + roleName.replace('_', ' ') + "' is already assigned to an active staff member. " +
                            "Deactivate the existing account first.");

        // Minimum 5 characters for staff temp password
        if (!StringUtils.hasText(tempPassword) || tempPassword.length() < 5)
            throw new IllegalArgumentException("Temporary password must be at least 5 characters.");

        if (userRepository.existsByEmail(email.trim().toLowerCase()))
            throw new IllegalStateException(
                    "Email '" + email + "' is already registered.");

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(
                        "Role '" + roleName + "' not found."));

        User user = User.builder()
                .fullName(fullName.trim())
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .roleEntity(role)
                .member(null)
                .active(true)
                .mustChangePassword(true)   // triggers banner on dashboard
                .build();

        return userRepository.save(user);
    }

    // ── Staff changes their password ──────────────────────────────────────

    public void changePassword(User user, String currentPassword,
                               String newPassword, String confirmPassword) {

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash()))
            throw new IllegalArgumentException("Current password is incorrect.");

        if (!StringUtils.hasText(newPassword) || newPassword.length() < 5)
            throw new IllegalArgumentException("New password must be at least 5 characters.");

        if (!newPassword.equals(confirmPassword))
            throw new IllegalArgumentException("Passwords do not match.");

        if (currentPassword.equals(newPassword))
            throw new IllegalArgumentException("New password must be different from current password.");

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);   // dismiss banner permanently
        userRepository.save(user);
    }

    // ── Dismiss banner without changing password ──────────────────────────

    public void dismissBanner(User user) {
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    // ── List all staff (non-member, non-admin) ────────────────────────────

    @Transactional(readOnly = true)
    public List<User> getAllStaff() {
        return userRepository.findAll().stream()
                .filter(u -> {
                    String role = u.getRole();
                    return role != null && ALLOWED_STAFF_ROLES.contains(role);
                })
                .toList();
    }
}