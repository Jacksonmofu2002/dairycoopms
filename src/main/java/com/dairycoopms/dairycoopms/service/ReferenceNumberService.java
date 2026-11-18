package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.repository.PaymentRepository;
import com.dairycoopms.dairycoopms.repository.StaffSalaryRepository;
import com.dairycoopms.dairycoopms.repository.LoanRepository;
import com.dairycoopms.dairycoopms.repository.CooperativeFundTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Generates sequential, human-readable reference numbers for all payment types.
 *
 * Format: PREFIX-YYYY-NNNN
 *   PREFIX  = type code (SAL, MKP, DIS, LRE, TXN)
 *   YYYY    = current year
 *   NNNN    = zero-padded sequence within the year (resets each January)
 *
 * Examples:
 *   SAL-2026-0001  → 1st staff salary payment of 2026
 *   MKP-2026-0003  → 3rd member milk payment of 2026
 *   DIS-2026-0001  → 1st loan disbursement of 2026
 */
@Service
public class ReferenceNumberService {

    private final StaffSalaryRepository       salaryRepository;
    private final PaymentRepository            paymentRepository;
    private final LoanRepository               loanRepository;
    private final CooperativeFundTransactionRepository txnRepository;

    public ReferenceNumberService(StaffSalaryRepository salaryRepository,
                                  PaymentRepository paymentRepository,
                                  LoanRepository loanRepository,
                                  CooperativeFundTransactionRepository txnRepository) {
        this.salaryRepository  = salaryRepository;
        this.paymentRepository = paymentRepository;
        this.loanRepository    = loanRepository;
        this.txnRepository     = txnRepository;
    }

    // ── Staff salary payment  e.g. SAL-2026-0001 ─────────────────────────
    public String nextSalaryRef() {
        int year = LocalDate.now().getYear();
        long count = salaryRepository.findAll().stream()
                .filter(s -> "PAID".equals(s.getStatus()) && s.getYear() == year)
                .count();
        return String.format("SAL-%d-%04d", year, count + 1);
    }

    // ── Member milk payment  e.g. MKP-2026-0001 ──────────────────────────
    public String nextMilkPaymentRef() {
        int year = LocalDate.now().getYear();
        long count = paymentRepository.findAll().stream()
                .filter(p -> "PAID".equals(p.getStatus()) && p.getYear() == year)
                .count();
        return String.format("MKP-%d-%04d", year, count + 1);
    }

    // ── Loan disbursement  e.g. DIS-2026-0001 ────────────────────────────
    public String nextDisbursementRef() {
        int year = LocalDate.now().getYear();
        long count = loanRepository.findAll().stream()
                .filter(l -> "DISBURSED".equals(l.getDisbursementStatus())
                        && l.getDisbursedAt() != null
                        && l.getDisbursedAt().getYear() == year)
                .count();
        return String.format("DIS-%d-%04d", year, count + 1);
    }

    // ── COFA transaction  e.g. TXN-2026-0001 ─────────────────────────────
    public String nextTransactionRef() {
        int year = LocalDate.now().getYear();
        long count = txnRepository.findAll().stream()
                .filter(t -> t.getCreatedAt() != null
                        && t.getCreatedAt().getYear() == year)
                .count();
        return String.format("TXN-%d-%04d", year, count + 1);
    }
}