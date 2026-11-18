package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.*;
import com.dairycoopms.dairycoopms.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CollectionOfficerService – full implementation of Collection Officer duties.
 *
 * Spec section 3 – Collection Officer:
 *   1. Verify member identity before accepting a delivery
 *   2. Record daily milk deliveries (quantity, date/time)
 *   3. Submit and view daily milk collection records
 *
 * CRITICAL DESIGN:
 *   pricePerLitreAtDelivery is snapshotted from the active MilkPrice at the
 *   moment of recording. AccountantService.calculateGrossEarnings() uses THIS
 *   snapshot — not the current price — so historical earnings are never altered
 *   by future price changes.
 *
 *   The UNIQUE constraint (member_id, delivery_date) prevents double-counting.
 */
@Slf4j
@Service
@Transactional
public class CollectionOfficerService {

    private final MilkDeliveryRepository milkDeliveryRepository;
    private final MemberRepository       memberRepository;
    private final MilkPriceRepository    milkPriceRepository;
    private final AuditLogRepository     auditLogRepository;
    private final CowRepository          cowRepository;


    // ── Explicit constructor (works in all IntelliJ editions) ────────────────
    public CollectionOfficerService(
            MilkDeliveryRepository milkDeliveryRepository,
            MemberRepository memberRepository,
            MilkPriceRepository milkPriceRepository,
            AuditLogRepository auditLogRepository,
            CowRepository cowRepository) {
        this.milkDeliveryRepository = milkDeliveryRepository;
        this.memberRepository = memberRepository;
        this.milkPriceRepository = milkPriceRepository;
        this.auditLogRepository = auditLogRepository;
        this.cowRepository = cowRepository;
    }
    // ─────────────────────────────────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════════════════
    // AUDIT HELPER
    // ═══════════════════════════════════════════════════════════════════════════

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
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. MEMBER IDENTITY VERIFICATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verify a member is APPROVED and active before accepting their delivery.
     * Call this first, then recordDelivery().
     *
     * @param memberNumber the MEM-XXXXX number printed on the member's card
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COLLECTION_OFFICER')")
    public Member verifyMemberByNumber(String memberNumber) {
        Member member = memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No member found with number: " + memberNumber));
        return validateMemberForDelivery(member);
    }

    /**
     * Verify by UUID – used by UI when selecting from a list.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COLLECTION_OFFICER')")
    public Member verifyMember(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        return validateMemberForDelivery(member);
    }

    private Member validateMemberForDelivery(Member member) {
        if (!"APPROVED".equals(member.getStatus()))
            throw new IllegalStateException(
                    "Member '" + member.getName() + "' is not APPROVED (status: "
                            + member.getStatus() + "). Cannot accept delivery.");
        if (!member.isActive())
            throw new IllegalStateException(
                    "Member '" + member.getName() + "' account is inactive. Cannot accept delivery.");
        return member;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. RECORD MILK DELIVERY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Record a milk delivery from a verified member.
     *
     * Business rules enforced:
     *   - Member must be APPROVED and active (re-verified here)
     *   - Quantity must be positive
     *   - Delivery datetime cannot be in the future
     *   - Only one delivery per member per calendar day
     *   - Price at delivery time is snapshotted from active MilkPrice
     *   - cow_id is optional – linked only if the officer knows which cow
     */
    @PreAuthorize("hasRole('COLLECTION_OFFICER')")
    public MilkDelivery recordDelivery(UUID memberId,
                                       BigDecimal quantityLitres,
                                       LocalDateTime deliveryDateTime,
                                       UUID cowId,
                                       String notes,
                                       User officer) {
        // ── Input validation ─────────────────────────────────────────────────
        if (quantityLitres == null || quantityLitres.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Quantity must be a positive value in litres");

        if (deliveryDateTime == null)
            throw new IllegalArgumentException("Delivery date and time are required");

        if (deliveryDateTime.isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Delivery date cannot be in the future");

        // ── Member verification ───────────────────────────────────────────────
        Member member = validateMemberForDelivery(
                memberRepository.findById(memberId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Member not found: " + memberId)));

        // ── Price snapshot ────────────────────────────────────────────────────
        MilkPrice activePrice = milkPriceRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No active milk price configured. Admin must set a price before deliveries can be recorded."));

        // ── Max 2 deliveries per member per day ──────────────────────────────────
        LocalDate deliveryDate = deliveryDateTime.toLocalDate();
        long deliveryCount = milkDeliveryRepository.countByMemberIdAndDeliveryDate(memberId, deliveryDate);
        if (deliveryCount >= 2)
            throw new IllegalStateException(
                    member.getName() + " already has 2 deliveries on "
                            + deliveryDate + ". Maximum is 2 per day.");

        // ── Optional cow lookup ───────────────────────────────────────────────
        Cow cow = null;
        if (cowId != null) {
            cow = cowRepository.findById(cowId)
                    .orElseThrow(() -> new IllegalArgumentException("Cow not found: " + cowId));
        }

        // ── Build and save ────────────────────────────────────────────────────
        MilkDelivery delivery = MilkDelivery.builder()
                .id(UUID.randomUUID())
                .member(member)
                .cow(cow)
                .quantityLitres(quantityLitres)
                .pricePerLitreAtDelivery(activePrice.getPricePerLitre())
                .deliveryDate(deliveryDate)
                .deliveryDateTime(deliveryDateTime)
                .recordedBy(officer)
                .deliveryStatus("RECORDED")
                .notes(notes)
                .build();

        MilkDelivery saved = milkDeliveryRepository.save(delivery);
        logAction("MilkDelivery", saved.getId(), "RECORDED", officer);

        log.info("Delivery recorded: member={} litres={} price={} date={}",
                member.getMemberNumber(), quantityLitres,
                activePrice.getPricePerLitre(), deliveryDate);
        return saved;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3. DAILY COLLECTION RECORDS
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COLLECTION_OFFICER')")
    public List<MilkDelivery> getTodaysDeliveries() {
        return milkDeliveryRepository.findByDeliveryDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COLLECTION_OFFICER')")
    public List<MilkDelivery> getDeliveriesByDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Date is required");
        return milkDeliveryRepository.findByDeliveryDate(date);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COLLECTION_OFFICER')")
    public List<MilkDelivery> getDeliveriesByMember(UUID memberId) {
        return milkDeliveryRepository.findByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COLLECTION_OFFICER')")
    public BigDecimal getTotalLitresOnDate(LocalDate date) {
        return milkDeliveryRepository.sumQuantityByDeliveryDate(date)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COLLECTION_OFFICER')")
    public BigDecimal getMemberMonthlyTotal(UUID memberId, int year, int month) {
        return milkDeliveryRepository
                .sumQuantityByMemberIdAndYearAndMonth(memberId, year, month)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Total value of today's collection:
     *   SUM(quantityLitres × pricePerLitreAtDelivery) for all today's deliveries.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COLLECTION_OFFICER')")
    public BigDecimal getTodaysCollectionValue() {
        return milkDeliveryRepository.findByDeliveryDate(LocalDate.now())
                .stream()
                .map(d -> d.getQuantityLitres().multiply(d.getPricePerLitreAtDelivery()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * List of all APPROVED active members – shown in the delivery entry dropdown.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COLLECTION_OFFICER')")
    public List<Member> getApprovedActiveMembers() {
        return memberRepository.findByStatusAndActiveTrue("APPROVED");
    }
}