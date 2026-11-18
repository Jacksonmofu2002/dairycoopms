package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Check if a member already has a portal account
    boolean existsByMemberId(UUID memberId);

    // Active staff/member accounts
    List<User> findByActiveTrue();

    // Pending registrations (is_active = false) – awaiting admin approval
    List<User> findByActiveFalse();

    List<User> findByRoleEntityName(String roleName);

    long countByActiveTrue();
    long countByActiveFalse();

    // Members who registered but have not submitted profile yet (member_id is null)
    long countByRoleEntityNameAndMemberIsNull(String roleName);
}