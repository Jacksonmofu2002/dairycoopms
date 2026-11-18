package com.dairycoopms.dairycoopms.repository;

import com.dairycoopms.dairycoopms.model.CowAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CowAssignment entity.
 *
 * existsByCowIdAndActiveTrue() is the double-assignment guard in AdminService.
 * existsByMemberIdAndActiveTrue() is checked before deactivating a member.
 *
 * getCowCountPerMember() powers the ManagerService cow allocation dashboard.
 */
@Repository
public interface CowAssignmentRepository extends JpaRepository<CowAssignment, UUID> {

    // ── Active assignment guards ─────────────────────────────────────────────

    /**
     * True if this cow already has an open (active) assignment.
     * Used in AdminService.assignCowToMember() to prevent double-assignment.
     * Maps to: WHERE cow_id = :cowId AND is_active = 1
     */
    boolean existsByCowIdAndActiveTrue(UUID cowId);

    /**
     * True if this member has at least one active cow.
     * Used in AdminService.deactivateMember() – blocks deactivation if true.
     */
    boolean existsByMemberIdAndActiveTrue(UUID memberId);

    // ── Fetch queries ────────────────────────────────────────────────────────

    List<CowAssignment> findByActiveTrue();

    List<CowAssignment> findByMemberId(UUID memberId);

    /**
     * Fetch all cow assignments for a member with cow details eagerly loaded.
     * Used by member portal to display cow information without lazy-load issues.
     * JOIN FETCH ensures all cow attributes are loaded in a single query.
     */
    @Query("""
            SELECT ca FROM CowAssignment ca
            JOIN FETCH ca.cow
            WHERE ca.member.id = :memberId
            ORDER BY ca.assignedAt DESC
            """)
    List<CowAssignment> findByMemberIdWithCowDetails(@Param("memberId") UUID memberId);

    List<CowAssignment> findByCowId(UUID cowId);

    Optional<CowAssignment> findByCowIdAndActiveTrue(UUID cowId);

    long countByActiveTrue();

    // ── Dashboard / Manager queries ──────────────────────────────────────────

    /**
     * Returns [Member, cowCount] pairs for all members with active assignments.
     * Used in ManagerService.getCowCountPerMember().
     */
    @Query("""
            SELECT ca.member, COUNT(ca) AS cowCount
            FROM CowAssignment ca
            WHERE ca.active = true
            GROUP BY ca.member
            ORDER BY cowCount DESC
            """)
    List<Object[]> getCowCountPerMember();

    /**
     * Total active assignments for a given member.
     */
    @Query("""
            SELECT COUNT(ca) FROM CowAssignment ca
            WHERE ca.member.id = :memberId
              AND ca.active = true
            """)
    long countActiveCowsByMemberId(@Param("memberId") UUID memberId);
}