package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Role entity.
 * Uses Integer PK (AUTO_INCREMENT, not UUID).
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}