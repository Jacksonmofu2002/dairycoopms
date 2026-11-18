package com.dairycoopms.dairycoopms.controller.accountant;

import com.dairycoopms.dairycoopms.model.Deduction;
import com.dairycoopms.dairycoopms.model.DeductionType;
import com.dairycoopms.dairycoopms.model.CooperativeFund;
import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.DeductionRepository;
import com.dairycoopms.dairycoopms.repository.CooperativeFundRepository;
import com.dairycoopms.dairycoopms.repository.UserRepository;
import com.dairycoopms.dairycoopms.service.AccountantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AccountantDeductionController - Fund Allocation from COFA Balance
 *
 * Manages fund allocation based on total cooperative funds.
 * Example: 10% of COFA balance goes to Cow Maintenance Fund
 *
 * Calculation: COFA Balance × Deduction Rate = Monthly Allocation
 */
@Controller
@RequestMapping("/accountant/deductions")
@PreAuthorize("hasRole('ACCOUNTS_OFFICER')")
public class AccountantDeductionController {

    private final AccountantService accountantService;
    private final UserRepository    userRepository;
    private final DeductionRepository deductionRepository;
    private final CooperativeFundRepository cofaRepository;

    public AccountantDeductionController(AccountantService accountantService,
                                         UserRepository userRepository,
                                         DeductionRepository deductionRepository,
                                         CooperativeFundRepository cofaRepository) {
        this.accountantService = accountantService;
        this.userRepository    = userRepository;
        this.deductionRepository = deductionRepository;
        this.cofaRepository = cofaRepository;
    }

    private User currentUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    /**
     * List all deduction types with fund allocation calculations
     * Shows: COFA balance, allocation per type, accumulated totals
     */
    @GetMapping
    public String listDeductions(Model model, @AuthenticationPrincipal UserDetails ud) {
        model.addAttribute("user", currentUser(ud));

        // Get COFA balance (total cooperative funds)
        CooperativeFund cofa = cofaRepository.findById(1)
                .orElseGet(() -> {
                    CooperativeFund newFund = new CooperativeFund();
                    newFund.setBalance(BigDecimal.ZERO);
                    return cofaRepository.save(newFund);
                });

        BigDecimal cofaBalance = cofa.getBalance();
        model.addAttribute("cofaBalance", cofaBalance);

        // Get all deduction types
        List<DeductionType> deductionTypes = accountantService.getAllDeductionTypes();
        model.addAttribute("deductionTypes", deductionTypes);

        // Calculate current month allocation for each deduction type
        // Formula: COFA Balance × Rate = Allocation Amount
        Map<Integer, BigDecimal> currentAllocations = new HashMap<>();
        BigDecimal totalCurrentAllocations = BigDecimal.ZERO;

        for (DeductionType dt : deductionTypes) {
            if (dt.isActive()) {
                // Calculate percentage: COFA Balance × (Rate / 100)
                BigDecimal allocation = cofaBalance
                        .multiply(dt.getRate())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                currentAllocations.put(dt.getId(), allocation);
                totalCurrentAllocations = totalCurrentAllocations.add(allocation);
            } else {
                currentAllocations.put(dt.getId(), BigDecimal.ZERO);
            }
        }
        model.addAttribute("currentAllocations", currentAllocations);
        model.addAttribute("totalCurrentAllocations", totalCurrentAllocations);

        // Calculate accumulated totals (historical deductions already made)
        Map<Integer, BigDecimal> totalAccumulated = calculateAccumulatedTotals();
        model.addAttribute("totalAccumulated", totalAccumulated);

        // Calculate this month's actual deductions (if any were recorded)
        Map<Integer, BigDecimal> thisMonthActual = calculateThisMonthTotals();
        model.addAttribute("thisMonthActual", thisMonthActual);

        // Grand total accumulated (all time)
        BigDecimal grandTotal = totalAccumulated.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("grandTotal", grandTotal);

        // Remaining COFA after allocations
        BigDecimal remainingCofa = cofaBalance.subtract(totalCurrentAllocations);
        model.addAttribute("remainingCofa", remainingCofa);

        // Current month info
        YearMonth currentMonth = YearMonth.now();
        model.addAttribute("currentYear", currentMonth.getYear());
        model.addAttribute("currentMonth", currentMonth.getMonthValue());
        model.addAttribute("currentMonthName", currentMonth.getMonth().toString());

        return "accountant/deductions/list";
    }

    /**
     * Calculate total accumulated deductions (all time)
     */
    private Map<Integer, BigDecimal> calculateAccumulatedTotals() {
        Map<Integer, BigDecimal> totals = new HashMap<>();

        List<Deduction> allDeductions = deductionRepository.findAll();

        for (Deduction deduction : allDeductions) {
            if (deduction.getDeductionType() != null) {
                Integer typeId = deduction.getDeductionType().getId();
                BigDecimal currentTotal = totals.getOrDefault(typeId, BigDecimal.ZERO);
                totals.put(typeId, currentTotal.add(deduction.getAmount()));
            }
        }

        return totals;
    }

    /**
     * Calculate deductions actually recorded for current month
     */
    private Map<Integer, BigDecimal> calculateThisMonthTotals() {
        Map<Integer, BigDecimal> totals = new HashMap<>();

        YearMonth currentMonth = YearMonth.now();
        int year = currentMonth.getYear();
        int month = currentMonth.getMonthValue();

        List<Deduction> monthDeductions = deductionRepository.findAll().stream()
                .filter(d -> d.getYear() == year && d.getMonth() == month)
                .collect(Collectors.toList());

        for (Deduction deduction : monthDeductions) {
            if (deduction.getDeductionType() != null) {
                Integer typeId = deduction.getDeductionType().getId();
                BigDecimal currentTotal = totals.getOrDefault(typeId, BigDecimal.ZERO);
                totals.put(typeId, currentTotal.add(deduction.getAmount()));
            }
        }

        return totals;
    }

    /**
     * View deduction history for a specific type
     */
    @GetMapping("/{id}/history")
    public String viewHistory(@PathVariable Integer id,
                              @AuthenticationPrincipal UserDetails ud,
                              Model model,
                              RedirectAttributes flash) {
        model.addAttribute("user", currentUser(ud));

        // Get deduction type
        Optional<DeductionType> deductionTypeOpt = accountantService.getAllDeductionTypes()
                .stream()
                .filter(dt -> dt.getId().equals(id))
                .findFirst();

        if (deductionTypeOpt.isEmpty()) {
            flash.addFlashAttribute("errorMessage", "Deduction type not found");
            return "redirect:/accountant/deductions";
        }

        DeductionType deductionType = deductionTypeOpt.get();
        model.addAttribute("deductionType", deductionType);

        // Get all deductions for this type
        List<Deduction> deductions = deductionRepository.findAll().stream()
                .filter(d -> d.getDeductionType() != null
                        && d.getDeductionType().getId().equals(id))
                .sorted((a, b) -> {
                    int yearComp = Integer.compare(b.getYear(), a.getYear());
                    if (yearComp != 0) return yearComp;
                    return Integer.compare(b.getMonth(), a.getMonth());
                })
                .collect(Collectors.toList());

        // Group by year-month
        Map<String, BigDecimal> monthlyTotals = new LinkedHashMap<>();
        Map<String, Integer> monthlyCount = new LinkedHashMap<>();

        for (Deduction d : deductions) {
            String key = d.getYear() + "-" + String.format("%02d", d.getMonth());
            BigDecimal current = monthlyTotals.getOrDefault(key, BigDecimal.ZERO);
            monthlyTotals.put(key, current.add(d.getAmount()));
            monthlyCount.put(key, monthlyCount.getOrDefault(key, 0) + 1);
        }

        model.addAttribute("monthlyTotals", monthlyTotals);
        model.addAttribute("monthlyCount", monthlyCount);

        // Total accumulated
        BigDecimal totalAccumulated = deductions.stream()
                .map(Deduction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalAccumulated", totalAccumulated);

        return "accountant/deductions/history";
    }

    /**
     * Show form to add new deduction type
     */
    @GetMapping("/add")
    public String showAddForm(Model model, @AuthenticationPrincipal UserDetails ud) {
        model.addAttribute("user", currentUser(ud));
        model.addAttribute("deductionType", new DeductionType());

        // Show current COFA balance for reference
        CooperativeFund cofa = cofaRepository.findById(1).orElse(null);
        if (cofa != null) {
            model.addAttribute("cofaBalance", cofa.getBalance());
        }

        return "accountant/deductions/add";
    }

    /**
     * Create a new deduction type
     */
    @PostMapping("/add")
    public String addDeduction(@ModelAttribute DeductionType deductionType,
                               @AuthenticationPrincipal UserDetails ud,
                               RedirectAttributes flash,
                               Model model) {
        try {
            accountantService.configureDeduction(deductionType, currentUser(ud));

            // Calculate what this rate means based on current COFA
            CooperativeFund cofa = cofaRepository.findById(1).orElse(null);
            if (cofa != null) {
                BigDecimal allocation = cofa.getBalance()
                        .multiply(deductionType.getRate())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                flash.addFlashAttribute("successMessage",
                        String.format("Deduction type '%s' configured at %s%%. " +
                                        "Current monthly allocation: ZMW %s (from COFA balance of ZMW %s)",
                                deductionType.getName(),
                                deductionType.getRate(),
                                allocation,
                                cofa.getBalance()));
            } else {
                flash.addFlashAttribute("successMessage",
                        "Deduction type '" + deductionType.getName() + "' configured successfully at "
                                + deductionType.getRate() + "% rate.");
            }

            return "redirect:/accountant/deductions";

        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("user", currentUser(ud));
            model.addAttribute("deductionType", deductionType);
            model.addAttribute("errorMessage", e.getMessage());
            return "accountant/deductions/add";
        }
    }

    /**
     * Update deduction rate
     */
    @PostMapping("/{id}/rate")
    public String updateRate(@PathVariable Integer id,
                             @RequestParam BigDecimal rate,
                             @AuthenticationPrincipal UserDetails ud,
                             RedirectAttributes flash) {
        try {
            DeductionType updated = accountantService.updateDeductionRate(id, rate, currentUser(ud));

            // Show impact of rate change
            CooperativeFund cofa = cofaRepository.findById(1).orElse(null);
            if (cofa != null) {
                BigDecimal newAllocation = cofa.getBalance()
                        .multiply(rate)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                flash.addFlashAttribute("successMessage",
                        String.format("Rate updated to %s%%. New monthly allocation: ZMW %s",
                                rate, newAllocation));
            } else {
                flash.addFlashAttribute("successMessage",
                        "Deduction rate updated to " + rate + "%.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/accountant/deductions";
    }

    /**
     * Toggle deduction type active/inactive
     */
    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable Integer id,
                               @RequestParam boolean active,
                               @AuthenticationPrincipal UserDetails ud,
                               RedirectAttributes flash) {
        try {
            accountantService.toggleDeductionActive(id, active, currentUser(ud));
            flash.addFlashAttribute("successMessage",
                    "Deduction type " + (active ? "activated" : "deactivated") + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/accountant/deductions";
    }
}