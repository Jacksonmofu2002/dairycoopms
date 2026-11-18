package com.dairycoopms.dairycoopms.controller.collection;

import com.dairycoopms.dairycoopms.model.MilkDelivery;
import com.dairycoopms.dairycoopms.repository.*;
import com.dairycoopms.dairycoopms.model.Member;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.dairycoopms.dairycoopms.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/collection")
@PreAuthorize("hasRole('COLLECTION_OFFICER')")
public class CollectionOfficerController {

    private final MilkDeliveryRepository milkDeliveryRepository;
    private final MemberRepository       memberRepository;
    private final MilkPriceRepository    milkPriceRepository;
    private final UserRepository         userRepository;

    public CollectionOfficerController(MilkDeliveryRepository milkDeliveryRepository,
                                       MemberRepository memberRepository,
                                       MilkPriceRepository milkPriceRepository,
                                       UserRepository userRepository) {
        this.milkDeliveryRepository = milkDeliveryRepository;
        this.memberRepository       = memberRepository;
        this.milkPriceRepository    = milkPriceRepository;
        this.userRepository         = userRepository;
    }

    private User currentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    //  Dashboard
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        User user = currentUser(ud);
        model.addAttribute("user", user);
        model.addAttribute("mustChangePassword", user.isMustChangePassword());

        LocalDate today = LocalDate.now();

        // Today's deliveries
        var todayDeliveries = milkDeliveryRepository.findByDeliveryDate(today);
        model.addAttribute("todayDeliveries", todayDeliveries);

        // Today's total litres
        BigDecimal todayLitres = todayDeliveries.stream()
                .map(MilkDelivery::getQuantityLitres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("todayLitres", todayLitres);
        model.addAttribute("todayCount", todayDeliveries.size());

        // Current milk price
        milkPriceRepository.findByActiveTrue()
                .ifPresent(p -> model.addAttribute("currentPrice", p.getPricePerLitre()));

        // Approved members count - exclude staff/admin
        model.addAttribute("approvedMembersCount", getApprovedCoopMembers().size());

        // This month deliveries
        var monthDeliveries = milkDeliveryRepository
                .findByDeliveryDateBetween(today.withDayOfMonth(1), today);
        BigDecimal monthLitres = monthDeliveries.stream()
                .map(MilkDelivery::getQuantityLitres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("monthLitres", monthLitres);
        model.addAttribute("monthCount", monthDeliveries.size());

        // Count today's confirmed deliveries
        long confirmedToday = todayDeliveries.stream()
                .filter(d -> d.isConfirmedByMember()).count();
        long unconfirmedToday = todayDeliveries.size() - confirmedToday;
        model.addAttribute("confirmedToday", confirmedToday);
        model.addAttribute("unconfirmedToday", unconfirmedToday);

        return "collection/dashboard";
    }

    //  Record delivery form
    @GetMapping("/record")
    public String recordForm(@AuthenticationPrincipal UserDetails ud,
                             @RequestParam(required = false) String date,
                             Model model) {
        User user = currentUser(ud);
        model.addAttribute("user", user);
        model.addAttribute("mustChangePassword", user.isMustChangePassword());

        // Approved members for dropdown
        model.addAttribute("members", getApprovedCoopMembers());

        // Current price - auto-filleds
        milkPriceRepository.findByActiveTrue()
                .ifPresent(p -> model.addAttribute("currentPrice", p.getPricePerLitre()));

        // Use provided date or default to today (for demo: any past date allowed)
        String selectedDate = (date != null && !date.isBlank()) ? date : LocalDate.now().toString();
        model.addAttribute("today", selectedDate);
        model.addAttribute("maxDate", LocalDate.now().toString());

        return "collection/record";
    }

    //  Submit delivery
    @PostMapping("/record")
    public String saveDelivery(@AuthenticationPrincipal UserDetails ud,
                               @RequestParam UUID memberId,
                               @RequestParam BigDecimal quantityLitres,
                               @RequestParam(required = false) String deliveryDateStr,
                               @RequestParam(required = false) String notes,
                               RedirectAttributes flash) {
        User officer = currentUser(ud);

        try {
            // Parse selected date (defaults to today)
            LocalDate deliveryDate = (deliveryDateStr != null && !deliveryDateStr.isBlank())
                    ? LocalDate.parse(deliveryDateStr)
                    : LocalDate.now();

            // Cannot record future dates
            if (deliveryDate.isAfter(LocalDate.now()))
                throw new IllegalArgumentException("Cannot record a delivery for a future date.");

            // Get current active price
            var price = milkPriceRepository.findByActiveTrue()
                    .orElseThrow(() -> new IllegalStateException(
                            "No active milk price set. Ask the admin to set a price first."));

            var member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("Member not found"));

            if (quantityLitres == null || quantityLitres.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Quantity must be greater than zero.");

            // Enforce max 2 deliveries per member per day
            long count = milkDeliveryRepository.countByMemberIdAndDeliveryDate(
                    memberId, deliveryDate);
            if (count >= 2)
                throw new IllegalStateException(
                        member.getName() + " already has 2 deliveries recorded on " +
                                deliveryDate + ". Maximum is 2 per day. " +
                                "Select a different date.");

            MilkDelivery delivery = MilkDelivery.builder()
                    .member(member)
                    .quantityLitres(quantityLitres)
                    .pricePerLitreAtDelivery(price.getPricePerLitre())
                    .deliveryDate(deliveryDate)
                    .deliveryDateTime(LocalDateTime.now())
                    .recordedBy(officer)
                    .deliveryStatus("RECORDED")
                    .notes(notes)
                    .build();

            milkDeliveryRepository.save(delivery);

            flash.addFlashAttribute("successMessage",
                    "Delivery " + (count + 1) + "/2 recorded for " + member.getName() +
                            " on " + deliveryDate + " — " + quantityLitres + " L at ZMW " +
                            price.getPricePerLitre() + "/L" +
                            (count + 1 >= 2 ? " (daily limit reached)" : ""));

        } catch (Exception e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/collection/record?date=" +
                    (deliveryDateStr != null ? deliveryDateStr : "");
        }
        return "redirect:/collection/dashboard";
    }

    //  History
    @GetMapping("/history")
    public String history(@AuthenticationPrincipal UserDetails ud,
                          @RequestParam(required = false) String from,
                          @RequestParam(required = false) String to,
                          Model model) {
        User user = currentUser(ud);
        model.addAttribute("user", user);
        model.addAttribute("mustChangePassword", user.isMustChangePassword());

        LocalDate today   = LocalDate.now();
        LocalDate fromDate = (from != null && !from.isBlank())
                ? LocalDate.parse(from) : today.minusDays(29);
        LocalDate toDate   = (to   != null && !to.isBlank())
                ? LocalDate.parse(to)   : today;

        var deliveries = milkDeliveryRepository.findByDeliveryDateBetween(fromDate, toDate);
        model.addAttribute("deliveries",   deliveries);
        model.addAttribute("fromDate",     fromDate.toString());
        model.addAttribute("toDate",       toDate.toString());
        model.addAttribute("totalCount",   deliveries.size());

        BigDecimal totalLitres = deliveries.stream()
                .map(MilkDelivery::getQuantityLitres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalLitres", totalLitres);

        long confirmed   = deliveries.stream().filter(d -> d.isConfirmedByMember()).count();
        long unconfirmed = deliveries.size() - confirmed;
        model.addAttribute("confirmedCount",   confirmed);
        model.addAttribute("unconfirmedCount", unconfirmed);

        return "collection/history";
    }
    // Returns only genuine cooperative farmer members (excludes staff/admin accounts)
    private List<Member> getApprovedCoopMembers() {
        Set<String> staffEmails = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && !"MEMBER".equals(u.getRole()))
                .filter(u -> u.getEmail() != null)
                .map(u -> u.getEmail().toLowerCase())
                .collect(Collectors.toSet());

        return memberRepository.findByStatusAndActiveTrue("APPROVED").stream()
                .filter(m -> m.getEmail() == null ||
                        !staffEmails.contains(m.getEmail().toLowerCase()))
                .toList();
    }
}