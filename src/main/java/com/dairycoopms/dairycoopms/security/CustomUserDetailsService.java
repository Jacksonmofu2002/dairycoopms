package com.dairycoopms.dairycoopms.security;

import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Explicit constructor – no Lombok, works in every environment
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No account found for email: " + email));

        // user.active is a plain boolean field.
        // We read it directly to avoid any Lombok dependency.
        if (!user.isActive()) {
            throw new UsernameNotFoundException(
                    "Account is pending approval: " + email);
        }

        // user.getRole() is a hand-written method on User – not Lombok generated
        String roleName = user.getRole();
        if (roleName == null) {
            throw new UsernameNotFoundException(
                    "User has no role assigned: " + email);
        }

        // "ROLE_ADMIN", "ROLE_COLLECTION_OFFICER" etc.
        String authority = "ROLE_" + roleName;

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}