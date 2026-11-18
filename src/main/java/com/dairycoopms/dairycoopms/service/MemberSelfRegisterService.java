package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.Member;
import com.dairycoopms.dairycoopms.model.Role;
import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.MemberRepository;
import com.dairycoopms.dairycoopms.repository.RoleRepository;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class MemberSelfRegisterService {

    private final UserRepository   userRepository;
    private final RoleRepository   roleRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder  passwordEncoder;

    public MemberSelfRegisterService(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     MemberRepository memberRepository,
                                     PasswordEncoder passwordEncoder) {
        this.userRepository   = userRepository;
        this.roleRepository   = roleRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder  = passwordEncoder;
    }

    // ── STEP 1: Create login account ─────────────────────────────────────

    public User createAccount(String fullName, String email,
                              String password, String confirmPassword) {

        if (!StringUtils.hasText(fullName))
            throw new IllegalArgumentException("Full name is required.");
        if (!StringUtils.hasText(email) || !email.contains("@"))
            throw new IllegalArgumentException("A valid email address is required.");
        if (!StringUtils.hasText(password))
            throw new IllegalArgumentException("Password is required.");
        if (!password.equals(confirmPassword))
            throw new IllegalArgumentException("Passwords do not match.");
        if (password.length() < 8)
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        if (!password.chars().anyMatch(Character::isUpperCase))
            throw new IllegalArgumentException("Password must contain at least one uppercase letter.");
        if (!password.chars().anyMatch(Character::isDigit))
            throw new IllegalArgumentException("Password must contain at least one number.");

        if (userRepository.existsByEmail(email.trim().toLowerCase()))
            throw new IllegalStateException(
                    "Email '" + email + "' is already registered. Please sign in instead.");

        Role memberRole = roleRepository.findByName("MEMBER")
                .orElseThrow(() -> new IllegalStateException(
                        "MEMBER role not found. Run fix_roles.sql in MySQL Workbench."));

        User user = User.builder()
                .fullName(fullName.trim())
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .roleEntity(memberRole)
                .member(null)
                .active(true)
                .build();

        return userRepository.save(user);
    }

    // ── STEP 2: Submit profile (first time or resubmit after rejection) ───

    public Member submitProfile(User user, String nrc,
                                String phone, String address) {

        if (!StringUtils.hasText(nrc))
            throw new IllegalArgumentException("NRC number is required.");
        if (!StringUtils.hasText(phone))
            throw new IllegalArgumentException("Phone number is required.");
        if (!StringUtils.hasText(address))
            throw new IllegalArgumentException("Home address is required.");

        // If member already has a profile
        if (user.getMember() != null) {
            Member existing = user.getMember();

            // Only allow resubmission if REJECTED
            if (!"REJECTED".equals(existing.getStatus())) {
                throw new IllegalStateException(
                        "Profile already submitted. Status: " + existing.getStatus());
            }

            // Check NRC uniqueness - allow same NRC as before
            if (!existing.getNrc().equalsIgnoreCase(nrc.trim()) &&
                    memberRepository.existsByNrc(nrc.trim())) {
                throw new IllegalStateException(
                        "NRC '" + nrc + "' is already registered by another member.");
            }

            // Update existing rejected profile and resubmit
            existing.setNrc(nrc.trim());
            existing.setPhoneNumber(phone.trim());
            existing.setAddress(address.trim());
            existing.setStatus("PENDING");
            existing.setRejectionReason(null);
            existing.setNotificationRead(true);
            return memberRepository.save(existing);
        }

        // First time submission
        if (memberRepository.existsByNrc(nrc.trim()))
            throw new IllegalStateException(
                    "NRC '" + nrc + "' is already registered by another member.");

        long count = memberRepository.count() + 1;
        String memberNumber = String.format("MEM-%05d", count);

        Member member = Member.builder()
                .name(user.getFullName())
                .memberNumber(memberNumber)
                .nrc(nrc.trim())
                .phoneNumber(phone.trim())
                .address(address.trim())
                .email(user.getEmail())
                .status("PENDING")
                .notificationRead(true)
                .active(true)
                .build();

        Member saved = memberRepository.save(member);
        user.setMember(saved);
        userRepository.save(user);
        return saved;
    }

    // ── Mark notification as read ─────────────────────────────────────────

    public void markNotificationRead(User user) {
        if (user.getMember() != null && !user.getMember().isNotificationRead()) {
            user.getMember().setNotificationRead(true);
            memberRepository.save(user.getMember());
        }
    }

    public void saveBankDetails(com.dairycoopms.dairycoopms.model.Member member) {
        memberRepository.save(member);
    }
}