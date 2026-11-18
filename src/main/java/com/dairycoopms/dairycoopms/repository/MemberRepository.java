package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Member entity.
 *
 * Every method name here maps directly to a service call:
 *
 *   AdminService:
 *     existsByNrc()                       → duplicate NRC guard
 *     existsByEmail()                     → duplicate email guard
 *     findByActiveTrue()                  → getActiveMembers()
 *     countByActiveTrue()                 → dashboard
 *     countByStatus()                     → dashboard pending approvals
 *     findMembersWithNoActiveCowAssignment() → ManagerService unproductive members
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {

    // ── Duplicate guards (AdminService.registerMember) ──────────────────────
    boolean existsByNrc(String nrc);
    boolean existsByEmail(String email);

    // ── Status queries ───────────────────────────────────────────────────────
    List<Member> findByStatus(String status);
    long countByStatus(String status);

    // ── Active flag queries ──────────────────────────────────────────────────
    List<Member> findByActiveTrue();
    long countByActiveTrue();

    // ── Combined queries ─────────────────────────────────────────────────────
    List<Member> findByStatusAndActiveTrue(String status);

    Optional<Member> findByMemberNumber(String memberNumber);

    // ── Dashboard / Manager queries ──────────────────────────────────────────

    /**
     * Members who have NO active cow assignment.
     * Used by ManagerService.getMembersWithNoCows().
     */
    @Query("""
            SELECT m FROM Member m
            WHERE m.active = true
              AND m.status = 'APPROVED'
              AND NOT EXISTS (
                  SELECT 1 FROM CowAssignment ca
                  WHERE ca.member = m
                    AND ca.active = true
              )
            """)
    List<Member> findMembersWithNoActiveCowAssignment();
}