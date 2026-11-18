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

/**
 * UserRegistrationService
 *
 * Handles self-registration of new system users.
 *
 * SECURITY RULES ENFORCED:
 *   1. ADMIN role cannot be self-registered — only the existing admin
 *      can create other admins (via the admin portal user management).
 *   2. Duplicate email is rejected immediately.
 *   3. Password must be at least 8 characters and contain at least
 *      one uppercase letter, one digit, and one special character.
 *   4. New accounts are created with is_active = FALSE — they cannot
 *      log in until an ADMIN approves them.
 *   5. The roles available for self-registration are strictly limited
 *      to: COLLECTION_OFFICER, ACCOUNTANT, MANAGER, LOAN_OFFICER.
 */
@Service
@Transactional
public class UserRegistrationService {

    // Roles that are BLOCKED from self-registration
    private static final List<String> BLOCKED_ROLES = List.of("ADMIN");

    // Roles allowed for self-registration
    private static final List<String> ALLOWED_ROLES = List.of(
            "COLLECTION_OFFICER", "ACCOUNTANT", "MANAGER", "LOAN_OFFICER"
    );

    private final UserRepository    userRepository;
    private final RoleRepository    roleRepository;
    private final PasswordEncoder   passwordEncoder;

    public UserRegistrationService(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.roleRepository  = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns the list of roles available for self-registration.
     * ADMIN is excluded — it cannot be self-registered.
     */
    @Transactional(readOnly = true)
    public List<Role> getSelectableRoles() {
        return ALLOWED_ROLES.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElse(null))
                .filter(r -> r != null)
                .toList();
    }

    /**
     * Register a new user account.
     *
     * The account is created with is_active = FALSE.
     * The user cannot log in until an admin activates the account.
     *
     * @param fullName  display name
     * @param email     login email – must be unique
     * @param password  plaintext password – will be BCrypt-hashed
     * @param roleName  role to assign – must be in ALLOWED_ROLES
     * @return the saved User (is_active = false)
     * @throws IllegalArgumentException if any validation fails
     * @throws IllegalStateException    if email already exists
     * @throws SecurityException        if ADMIN role is attempted
     */
    public User register(String fullName,
                         String email,
                         String password,
                         String roleName) {

        // ── 1. Input validation ───────────────────────────────────────────────
        if (!StringUtils.hasText(fullName))
            throw new IllegalArgumentException("Full name is required.");

        if (!StringUtils.hasText(email) || !email.contains("@"))
            throw new IllegalArgumentException("A valid email address is required.");

        if (!StringUtils.hasText(password))
            throw new IllegalArgumentException("Password is required.");

        if (!StringUtils.hasText(roleName))
            throw new IllegalArgumentException("Please select a role.");

        // ── 2. Password strength ──────────────────────────────────────────────
        validatePasswordStrength(password);

        // ── 3. SECURITY: block ADMIN self-registration ────────────────────────
        if (BLOCKED_ROLES.contains(roleName.toUpperCase())) {
            throw new SecurityException(
                    "Self-registration as ADMIN is not permitted. " +
                            "Contact your system administrator.");
        }

        // ── 4. Role must be in the allowed list ───────────────────────────────
        if (!ALLOWED_ROLES.contains(roleName.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Invalid role selected: " + roleName);
        }

        // ── 5. Duplicate email check ──────────────────────────────────────────
        if (userRepository.existsByEmail(email.trim().toLowerCase())) {
            throw new IllegalStateException(
                    "An account with email '" + email + "' already exists. " +
                            "If you have forgotten your password, contact the administrator.");
        }

        // ── 6. Resolve role entity ────────────────────────────────────────────
        Role role = roleRepository.findByName(roleName.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Role not found: " + roleName));

        // ── 7. Hash password and save ─────────────────────────────────────────
        User user = User.builder()
                .fullName(fullName.trim())
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .roleEntity(role)
                .active(false)   // MUST be false – requires admin approval to log in
                .build();

        return userRepository.save(user);
    }

    // ── Admin: list pending registrations ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<User> getPendingUsers() {
        return userRepository.findByActiveFalse();
    }

    /**
     * Admin approves a pending user — sets is_active = true.
     * The user can now log in.
     */
    public User approveUser(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + userId));
        if (user.isActive())
            throw new IllegalStateException("User is already active.");
        user.setActive(true);
        return userRepository.save(user);
    }

    /**
     * Admin rejects (deletes) a pending registration.
     * The email is freed up — the person may re-apply.
     */
    public void rejectUser(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + userId));
        if (user.isActive())
            throw new IllegalStateException(
                    "Cannot reject an already-active user account. Deactivate instead.");
        userRepository.delete(user);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validatePasswordStrength(String password) {
        if (password.length() < 8)
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters long.");

        boolean hasUpper   = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit   = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(c ->
                "!@#$%^&*()-_=+[]{}|;:',.<>?/`~".indexOf(c) >= 0);

        if (!hasUpper)
            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter.");
        if (!hasDigit)
            throw new IllegalArgumentException(
                    "Password must contain at least one number.");
        if (!hasSpecial)
            throw new IllegalArgumentException(
                    "Password must contain at least one special character (!@#$%^&* etc.).");
    }
}