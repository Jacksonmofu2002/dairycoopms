package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.*;
import com.dairycoopms.dairycoopms.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AdminService  full implementation of all Administrator responsibilities.
 *
 * Spec section 3  Administrator:
 *   1. Register and manage cooperative members
 *   2. Record cows owned by the cooperative
 *   3. Assign cows to members
 *   4. Set milk price per litre
 *   5. Approve / reject loan requests  (ACCOUNTS_OFFICER role)
 *   6. Generate administrative reports
 *   7. Manage system users
 *
 * Note: Deduction configuration has been moved to AccountantService for better
 * separation of duties (financial operations vs operational management).
 *
 * All write methods are @Transactional.
 * @PreAuthorize provides a second layer of role enforcement beyond SecurityConfig URLs.
 * logAction() writes every state change to audit_logs for traceability.
 */
@Slf4j
@Service
@Transactional
public class AdminService {

    //  Status constants
    private static final String STATUS_PENDING  = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final MemberRepository        memberRepository;
    private final CowRepository           cowRepository;
    private final CowAssignmentRepository cowAssignmentRepository;
    private final LoanRepository          loanRepository;
    private final MilkPriceRepository     milkPriceRepository;
    private final AuditLogRepository      auditLogRepository;
    private final UserRepository          userRepository;


    //  Explicit constructor (works in all IntelliJ editions)
    public AdminService(
            MemberRepository memberRepository,
            CowRepository cowRepository,
            CowAssignmentRepository cowAssignmentRepository,
            LoanRepository loanRepository,
            MilkPriceRepository milkPriceRepository,
            AuditLogRepository auditLogRepository,
            UserRepository userRepository) {
        this.memberRepository = memberRepository;
        this.cowRepository = cowRepository;
        this.cowAssignmentRepository = cowAssignmentRepository;
        this.loanRepository = loanRepository;
        this.milkPriceRepository = milkPriceRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }
    //
    //
    // AUDIT HELPER
    //

    private void logAction(String entityType, UUID entityId, String action, User changedBy) {
        AuditLog auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
        log.info("AUDIT | {} | {} | {} | by={}", entityType, entityId, action,
                changedBy != null ? changedBy.getEmail() : "system");
    }

    private void logAction(String entityType, Optional<UUID> entityId,
                           String action, User changedBy) {
        logAction(entityType, entityId.orElse(null), action, changedBy);
    }

    //
    // 1. MEMBER MANAGEMENT
    //

    /**
     * Register a new cooperative member.
     * Status is ALWAYS forced to PENDING regardless of what the caller supplies.
     * Duplicate NRC and duplicate email are both rejected.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Member registerMember(Member member, User admin) {
        validateNotNull(member, "Member");

        if (!StringUtils.hasText(member.getName()))
            throw new IllegalArgumentException("Full name is required");
        if (!StringUtils.hasText(member.getNrc()))
            throw new IllegalArgumentException("NRC is required");
        if (!StringUtils.hasText(member.getPhoneNumber()))
            throw new IllegalArgumentException("Phone number is required");
        if (!StringUtils.hasText(member.getAddress()))
            throw new IllegalArgumentException("Address is required");

        if (memberRepository.existsByNrc(member.getNrc()))
            throw new IllegalStateException(
                    "A member with NRC '" + member.getNrc() + "' already exists");

        if (StringUtils.hasText(member.getEmail()) &&
                memberRepository.existsByEmail(member.getEmail()))
            throw new IllegalStateException(
                    "A member with email '" + member.getEmail() + "' already exists");

        member.setMemberNumber(generateMemberNumber());
        member.setStatus(STATUS_PENDING);   // always forced  never trust caller
        member.setActive(true);

        Member saved = memberRepository.save(member);
        logAction("Member", saved.getId(), "REGISTERED", admin);
        return saved;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<Member> getAllMembers() {
        // Exclude members who belong to staff/admin users - by member_id link OR by email match
        java.util.List<com.dairycoopms.dairycoopms.model.User> nonMemberUsers =
                userRepository.findAll().stream()
                        .filter(u -> u.getRole() != null && !"MEMBER".equals(u.getRole()))
                        .toList();

        java.util.Set<java.util.UUID> staffMemberIds = nonMemberUsers.stream()
                .filter(u -> u.getMember() != null)
                .map(u -> u.getMember().getId())
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<String> staffEmails = nonMemberUsers.stream()
                .filter(u -> u.getEmail() != null)
                .map(u -> u.getEmail().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());

        return memberRepository.findAll().stream()
                .filter(m -> !staffMemberIds.contains(m.getId()))
                .filter(m -> m.getEmail() == null || !staffEmails.contains(m.getEmail().toLowerCase()))
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<Member> getActiveMembers() {
        return memberRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<Member> getMembersByStatus(String status) {
        java.util.List<com.dairycoopms.dairycoopms.model.User> nonMemberUsers =
                userRepository.findAll().stream()
                        .filter(u -> u.getRole() != null && !"MEMBER".equals(u.getRole()))
                        .toList();

        java.util.Set<java.util.UUID> staffMemberIds = nonMemberUsers.stream()
                .filter(u -> u.getMember() != null)
                .map(u -> u.getMember().getId())
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<String> staffEmails = nonMemberUsers.stream()
                .filter(u -> u.getEmail() != null)
                .map(u -> u.getEmail().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());

        return memberRepository.findByStatus(status).stream()
                .filter(m -> !staffMemberIds.contains(m.getId()))
                .filter(m -> m.getEmail() == null || !staffEmails.contains(m.getEmail().toLowerCase()))
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Member getMemberById(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
    }

    /**
     * Approve a PENDING member.
     * Only PENDING  APPROVED transitions are allowed.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Member approveMember(UUID memberId, User admin) {
        Member member = findMemberOrThrow(memberId);

        if (!STATUS_PENDING.equals(member.getStatus()))
            throw new IllegalStateException(
                    "Cannot approve member with status '" + member.getStatus() +
                            "'. Only PENDING members may be approved.");

        member.setStatus(STATUS_APPROVED);
        member.setNotificationRead(false);   // trigger notification on member portal
        Member updated = memberRepository.save(member);
        logAction("Member", memberId, "APPROVED", admin);
        return updated;
    }

    /**
     * Reject a PENDING member. Reason is mandatory.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Member rejectMember(UUID memberId, String reason, User admin) {
        if (!StringUtils.hasText(reason))
            throw new IllegalArgumentException("A rejection reason is required");

        Member member = findMemberOrThrow(memberId);

        if (!STATUS_PENDING.equals(member.getStatus()))
            throw new IllegalStateException(
                    "Cannot reject member with status '" + member.getStatus() +
                            "'. Only PENDING members may be rejected.");

        member.setStatus(STATUS_REJECTED);
        member.setRejectionReason(reason);
        member.setNotificationRead(false);   // trigger notification on member portal
        Member updated = memberRepository.save(member);
        logAction("Member", memberId, "REJECTED", admin);
        return updated;
    }

    /**
     * Soft-deactivate a member.
     * Blocked if the member still has active cow assignments.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Member deactivateMember(UUID memberId, User admin) {
        Member member = findMemberOrThrow(memberId);

        if (cowAssignmentRepository.existsByMemberIdAndActiveTrue(memberId))
            throw new IllegalStateException(
                    "Cannot deactivate member " + memberId +
                            ": they still have active cow assignments. Revoke assignments first.");

        member.setActive(false);
        Member updated = memberRepository.save(member);
        logAction("Member", memberId, "DEACTIVATED", admin);
        return updated;
    }

    /**
     * Reactivate a previously deactivated member.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Member reactivateMember(UUID memberId, User admin) {
        Member member = findMemberOrThrow(memberId);
        member.setActive(true);
        Member updated = memberRepository.save(member);
        logAction("Member", memberId, "REACTIVATED", admin);
        return updated;
    }

    //
    // 2. COW MANAGEMENT
    //

    /**
     * Record a new cooperative-owned cow.
     * Tag number must be unique across all cows.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Cow recordCow(Cow cow, User admin) {
        validateNotNull(cow, "Cow");

        // Auto-generate sequential tag number: TAG-2026-0001
        String year = String.valueOf(java.time.Year.now().getValue());
        long next = cowRepository.count() + 1;
        String tag = "TAG-" + year + "-" + String.format("%04d", next);
        // Ensure uniqueness in case of gaps
        while (cowRepository.existsByTagNumber(tag)) {
            next++;
            tag = "TAG-" + year + "-" + String.format("%04d", next);
        }
        cow.setTagNumber(tag);

        Cow saved = cowRepository.save(cow);
        logAction("Cow", saved.getId(), "RECORDED", admin);
        return saved;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<Cow> getAllCows() {
        return cowRepository.findAll();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<Cow> getUnassignedCows() {
        return cowRepository.findUnassignedCows();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Cow getCowById(UUID cowId) {
        return cowRepository.findById(cowId)
                .orElseThrow(() -> new IllegalArgumentException("Cow not found: " + cowId));
    }

    /**
     * Update cow status (e.g. ACTIVE  RETIRED or DECEASED).
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Cow updateCowStatus(UUID cowId, String newStatus, User admin) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new IllegalArgumentException("Cow not found: " + cowId));
        String oldStatus = cow.getStatus();
        cow.setStatus(newStatus);
        Cow updated = cowRepository.save(cow);
        logAction("Cow", cowId, "STATUS_CHANGED_" + oldStatus + "_TO_" + newStatus, admin);
        return updated;
    }

    /**
     * General update method for cow attributes (including portrait).
     */
    @PreAuthorize("hasRole('ADMIN')")
    public Cow updateCow(Cow cow, User admin) {
        Cow updated = cowRepository.save(cow);
        logAction("Cow", cow.getId(), "UPDATED", admin);
        return updated;
    }

    //
    // 3. COW ASSIGNMENT
    //

    /**
     * Assign a cooperative cow to an approved, active member.
     * Rules:
     *   - Member must be APPROVED and active
     *   - Cow must not have an existing active assignment
     */
    @PreAuthorize("hasRole('ADMIN')")
    public CowAssignment assignCowToMember(UUID cowId, UUID memberId, User admin) {
        Cow cow = cowRepository.findById(cowId)
                .orElseThrow(() -> new IllegalArgumentException("Cow not found: " + cowId));

        Member member = findMemberOrThrow(memberId);

        if (!STATUS_APPROVED.equals(member.getStatus()))
            throw new IllegalStateException(
                    "Member must be APPROVED before receiving a cow. Current status: "
                            + member.getStatus());

        if (!member.isActive())
            throw new IllegalStateException(
                    "Cannot assign a cow to an inactive member.");

        if (!"ACTIVE".equals(cow.getStatus()))
            throw new IllegalStateException(
                    "Cannot assign a cow with status '" + cow.getStatus() + "'.");

        if (cowAssignmentRepository.existsByCowIdAndActiveTrue(cowId))
            throw new IllegalStateException(
                    "Cow " + cow.getTagNumber() + " already has an active assignment. " +
                            "Revoke it before reassigning.");

        CowAssignment assignment = CowAssignment.builder()
                .id(UUID.randomUUID())
                .cow(cow)
                .member(member)
                .active(true)
                .assignedAt(LocalDateTime.now())
                .build();

        CowAssignment saved = cowAssignmentRepository.save(assignment);
        logAction("CowAssignment", saved.getId(), "ASSIGNED", admin);
        return saved;
    }

    /**
     * Revoke (close) an active cow assignment, freeing the cow for reassignment.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public CowAssignment revokeCowAssignment(UUID assignmentId, String reason, User admin) {
        CowAssignment assignment = cowAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Assignment not found: " + assignmentId));

        if (!assignment.isActive())
            throw new IllegalStateException("Assignment " + assignmentId + " is already closed.");

        assignment.setActive(false);
        assignment.setRevokedAt(LocalDateTime.now());
        assignment.setRevokeReason(reason);
        CowAssignment updated = cowAssignmentRepository.save(assignment);
        logAction("CowAssignment", assignmentId, "REVOKED", admin);
        return updated;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<CowAssignment> getAllActiveAssignments() {
        return cowAssignmentRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<CowAssignment> getAssignmentsByMember(UUID memberId) {
        return cowAssignmentRepository.findByMemberId(memberId);
    }

    //
    // 4. MILK PRICE
    //

    /**
     * Set the cooperative milk price per litre.
     * Deactivates any existing active price then saves the new one.
     * Only one active price exists at any time.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public MilkPrice setMilkPrice(BigDecimal price, String notes, User admin) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Price per litre must be positive");

        // Deactivate current active price
        milkPriceRepository.findByActiveTrue().ifPresent(existing -> {
            existing.setActive(false);
            milkPriceRepository.save(existing);
            log.info("Deactivated previous milk price: {}", existing.getPricePerLitre());
        });

        MilkPrice milkPrice = MilkPrice.builder()
                .id(UUID.randomUUID())
                .pricePerLitre(price)
                .effectiveDate(LocalDate.now())
                .active(true)
                .setBy(admin)
                .notes(notes)
                .build();

        MilkPrice saved = milkPriceRepository.save(milkPrice);
        logAction("MilkPrice", saved.getId(), "SET_PRICE", admin);
        return saved;
    }

    @Transactional(readOnly = true)
    public MilkPrice getCurrentMilkPrice() {
        return milkPriceRepository.findByActiveTrue().orElse(null);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<MilkPrice> getMilkPriceHistory() {
        return milkPriceRepository.findAllByOrderByEffectiveDateDesc();
    }

    //
    // 5. LOAN MANAGEMENT  (ACCOUNTS_OFFICER role)
    //

    /**
     * Approve a PENDING loan.
     * Rules:
     *   - Caller must have ACCOUNTS_OFFICER role
     *   - Loan must be PENDING
     *   - Loan's member must be APPROVED and active
     */
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public Loan approveLoan(UUID loanId, User officer) {
        Loan loan = findLoanOrThrow(loanId);

        if (!STATUS_PENDING.equals(loan.getStatus()))
            throw new IllegalStateException(
                    "Cannot approve loan with status '" + loan.getStatus() +
                            "'. Only PENDING loans may be approved.");

        if (loan.getMember() == null || !STATUS_APPROVED.equals(loan.getMember().getStatus()))
            throw new IllegalStateException(
                    "Cannot approve a loan for a member who is not APPROVED.");

        if (!loan.getMember().isActive())
            throw new IllegalStateException(
                    "Cannot approve a loan for an inactive member.");

        loan.setStatus(STATUS_APPROVED);
        loan.setApprovedBy(officer);
        loan.setApprovedDate(LocalDate.now());
        loan.setStartDate(LocalDate.now().withDayOfMonth(1).plusMonths(1));
        loan.setMaturityDate(loan.getStartDate().plusMonths(loan.getRepaymentMonths()));

        Loan updated = loanRepository.save(loan);
        logAction("Loan", loanId, "APPROVED", officer);
        return updated;
    }

    /**
     * Reject a PENDING loan. Reason is mandatory.
     */
    @PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
    public Loan rejectLoan(UUID loanId, String reason, User officer) {
        if (!StringUtils.hasText(reason))
            throw new IllegalArgumentException("A rejection reason is required");

        Loan loan = findLoanOrThrow(loanId);

        if (!STATUS_PENDING.equals(loan.getStatus()))
            throw new IllegalStateException(
                    "Cannot reject loan with status '" + loan.getStatus() +
                            "'. Only PENDING loans may be rejected.");

        loan.setStatus(STATUS_REJECTED);
        loan.setRejectionReason(reason);
        Loan updated = loanRepository.save(loan);
        logAction("Loan", loanId, "REJECTED", officer);
        return updated;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTS_OFFICER')")
    public List<Loan> getPendingLoans() {
        return loanRepository.findByStatus(STATUS_PENDING);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTS_OFFICER')")
    public List<Loan> getLoansByMember(UUID memberId) {
        return loanRepository.findByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('ACCOUNTS_OFFICER')")
    public Loan getLoanById(UUID loanId) {
        return findLoanOrThrow(loanId);
    }

    //
    // 6. DEDUCTION CONFIGURATION - MOVED TO AccountantService
    //
    // Deduction management is now handled by the Accountant role (ACCOUNTS_OFFICER)
    // in AccountantService.java. This provides better separation of duties:
    //   - Accountant: Configures and applies deductions (financial operations)
    //   - Admin: Manages members, cows, assignments (operational management)
    //

    //
    // 7. REPORT GENERATION
    //

    /**
     * Generate an administrative report.
     * Returns byte[]  callers stream to HTTP response or write to disk.
     * reportType must be uppercase alphanumeric/underscore (prevents log injection).
     * TODO: integrate JasperReports or iText for real PDF/JPEG output.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public byte[] generateReport(String reportType, User admin) {
        if (!StringUtils.hasText(reportType) || !reportType.matches("[A-Z0-9_]{1,50}"))
            throw new IllegalArgumentException(
                    "reportType must be 1-50 uppercase alphanumeric/underscore characters");

        byte[] reportBytes = new byte[0]; // TODO: JasperReports implementation

        logAction("Report", Optional.empty(),
                "GENERATED_" + reportType.toUpperCase(), admin);
        return reportBytes;
    }

    //
    // USER MANAGEMENT  (admin creates / manages system user accounts)
    //

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getActiveUsers() {
        return userRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    //
    // PRIVATE HELPERS
    //

    private Member findMemberOrThrow(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
    }

    private Loan findLoanOrThrow(UUID loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
    }

    private void validateNotNull(Object obj, String name) {
        if (obj == null)
            throw new IllegalArgumentException(name + " must not be null");
    }

    private String generateMemberNumber() {
        long count = memberRepository.count();
        return String.format("MEM-%05d", count + 1);
    }
}