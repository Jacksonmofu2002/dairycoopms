package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.MilkDelivery;
import com.dairycoopms.dairycoopms.repository.MilkDeliveryRepository;
import com.dairycoopms.dairycoopms.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Embedded Credit Scoring Engine
 *
 * PERIOD LOGIC:
 *   Always checks TWO months:
 *     1. The month the application is submitted (current month)
 *     2. The previous completed month
 *
 *   If EITHER month has >= 5 qualifying days (50+ litres) → AUTO_APPROVED
 *   The best score from the two months is used.
 *   Both months are reported in the reason so the member can see the breakdown.
 */

@Service
@Transactional(readOnly = true)
public class CreditScoringService {

    private static final BigDecimal QUALIFYING_LITRES   = new BigDecimal("50");
    private static final int        QUALIFYING_DAYS_MIN = 5;

    private final MilkDeliveryRepository milkDeliveryRepository;
    private final LoanRepository         loanRepository;

    public CreditScoringService(MilkDeliveryRepository milkDeliveryRepository,
                                LoanRepository loanRepository) {
        this.milkDeliveryRepository = milkDeliveryRepository;
        this.loanRepository         = loanRepository;
    }

    // ── Period summary ─────────────────────────────────────────────────────────

    private static class PeriodData {
        final int        year;
        final int        month;
        final String     label;
        final int        qualifyingDays;
        final int        totalDays;
        final BigDecimal totalLitres;
        final int        primaryScore;

        PeriodData(int year, int month, String label,
                   int qualifyingDays, int totalDays, BigDecimal totalLitres) {
            this.year           = year;
            this.month          = month;
            this.label          = label;
            this.qualifyingDays = qualifyingDays;
            this.totalDays      = totalDays;
            this.totalLitres    = totalLitres;
            this.primaryScore   = Math.min(qualifyingDays, 5);
        }
    }

    // ── Result DTO ─────────────────────────────────────────────────────────────

    public static class ScoreResult {
        private final int        score;
        private final boolean    approved;
        private final String     decision;
        private final String     reasonShort;
        private final String     reasonFull;
        private final int        qualifyingDays;   // from best month
        private final BigDecimal totalLitres;      // from best month
        private final int        periodYear;       // best month year
        private final int        periodMonth;      // best month mont
        private final String     periodLabel;      // best month label

        public ScoreResult(int score, boolean approved, String decision,
                           String reasonShort, String reasonFull,
                           int qualifyingDays, BigDecimal totalLitres,
                           int periodYear, int periodMonth, String periodLabel) {
            this.score          = score;
            this.approved       = approved;
            this.decision       = decision;
            this.reasonShort    = reasonShort;
            this.reasonFull     = reasonFull;
            this.qualifyingDays = qualifyingDays;
            this.totalLitres    = totalLitres;
            this.periodYear     = periodYear;
            this.periodMonth    = periodMonth;
            this.periodLabel    = periodLabel;
        }

        public int        getScore()          { return score; }
        public boolean    isApproved()        { return approved; }
        public String     getDecision()       { return decision; }
        public String     getReasonShort()    { return reasonShort; }
        public String     getReasonFull()     { return reasonFull; }
        public int        getQualifyingDays() { return qualifyingDays; }
        public BigDecimal getTotalLitres()    { return totalLitres; }
        public int        getPeriodYear()     { return periodYear; }
        public int        getPeriodMonth()    { return periodMonth; }
        public String     getPeriodLabel()    { return periodLabel; }
        public int        getScorePercent()   { return score * 10; }
    }

    // ── Main scoring method ────────────────────────────────────────────────────

    public ScoreResult score(UUID memberId, BigDecimal loanAmount) {

        LocalDate today = LocalDate.now();

        // ── Collect data for BOTH months ──────────────────────────────────────
        PeriodData current  = collect(memberId, today.getYear(), today.getMonthValue());
        LocalDate  prevDate = today.minusMonths(1);
        PeriodData previous = collect(memberId, prevDate.getYear(), prevDate.getMonthValue());

        // ── Choose the best period (most qualifying days) ─────────────────────
        PeriodData best = current.qualifyingDays >= previous.qualifyingDays
                ? current : previous;

        // ── Bonus points based on best period ────────────────────────────────
        int bonus = 0;

        if (best.totalLitres.compareTo(new BigDecimal("500")) >= 0)      bonus += 2;
        else if (best.totalLitres.compareTo(new BigDecimal("300")) >= 0) bonus += 1;

        if (best.totalDays >= 10) bonus += 1;

        // No active outstanding loan bonus
        boolean hasActiveLoan = loanRepository.findAll().stream().anyMatch(l ->
                l.getMember() != null
                        && memberId.equals(l.getMember().getId())
                        && "APPROVED".equals(l.getStatus())
                        && l.getRemainingBalance() != null
                        && l.getRemainingBalance().compareTo(BigDecimal.ZERO) > 0);
        if (!hasActiveLoan) bonus += 1;

        int finalScore = Math.min(best.primaryScore + bonus, 10);

        // ── Decision ─────────────────────────────────────────────────────────
        // Pass if EITHER month has >= 5 qualifying days
        boolean approved = current.qualifyingDays  >= QUALIFYING_DAYS_MIN
                || previous.qualifyingDays >= QUALIFYING_DAYS_MIN;
        String  decision = approved ? "AUTO_APPROVED" : "AUTO_REJECTED";

        // ── Build reasons ─────────────────────────────────────────────────────
        String reasonShort, reasonFull;

        // Format each month line
        String currentLine = String.format(
                "%s: %d qualifying day(s), %.2f L total (%s)",
                current.label, current.qualifyingDays,
                current.totalLitres,
                current.qualifyingDays >= QUALIFYING_DAYS_MIN ? "PASS" : "FAIL");

        String previousLine = String.format(
                "%s: %d qualifying day(s), %.2f L total (%s)",
                previous.label, previous.qualifyingDays,
                previous.totalLitres,
                previous.qualifyingDays >= QUALIFYING_DAYS_MIN ? "PASS" : "FAIL");

        if (approved) {
            // Which month caused the pass?
            String passingMonth = (current.qualifyingDays >= QUALIFYING_DAYS_MIN
                    && previous.qualifyingDays >= QUALIFYING_DAYS_MIN)
                    ? "both months qualify"
                    : (current.qualifyingDays >= QUALIFYING_DAYS_MIN
                    ? current.label : previous.label) + " qualifies";

            reasonShort = String.format(
                    "Auto-approved. Score %d/10 — %s. Best: %d qualifying days.",
                    finalScore, passingMonth, best.qualifyingDays);

            reasonFull = String.format(
                    "Approved based on your delivery record in %s.%n"
                            + "%d qualifying days with 50+ litres (required: %d).%n"
                            + "Score: %d/10.",
                    best.label, best.qualifyingDays, QUALIFYING_DAYS_MIN, finalScore);
        } else {
            reasonShort = String.format(
                    "Auto-rejected. Score %d/10 — neither month reached %d qualifying days. "
                            + "Best was %d day(s) in %s.",
                    finalScore, QUALIFYING_DAYS_MIN, best.qualifyingDays, best.label);

            reasonFull = String.format(
                    "Not approved. Best month (%s): only %d qualifying day(s) with 50+ litres.%n"
                            + "You need at least %d days in a month. Score: %d/10.%n"
                            + "Deliver more and reapply.",
                    best.label, best.qualifyingDays, QUALIFYING_DAYS_MIN, finalScore);
        }

        return new ScoreResult(finalScore, approved, decision,
                reasonShort, reasonFull,
                best.qualifyingDays, best.totalLitres,
                best.year, best.month, best.label);
    }

    // ── Helper: collect delivery data for one month ────────────────────────────

    private PeriodData collect(UUID memberId, int year, int month) {
        String label = Month.of(month)
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;

        List<MilkDelivery> deliveries =
                milkDeliveryRepository.findByMemberIdAndYearAndMonth(memberId, year, month);

        int        qualifyingDays = 0;
        BigDecimal totalLitres    = BigDecimal.ZERO;

        for (MilkDelivery d : deliveries) {
            totalLitres = totalLitres.add(d.getQuantityLitres());
            if (d.getQuantityLitres().compareTo(QUALIFYING_LITRES) >= 0) {
                qualifyingDays++;
            }
        }

        return new PeriodData(year, month, label,
                qualifyingDays, deliveries.size(), totalLitres);
    }
}