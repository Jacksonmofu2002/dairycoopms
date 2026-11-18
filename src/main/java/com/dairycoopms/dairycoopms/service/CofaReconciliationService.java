package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.*;
import com.dairycoopms.dairycoopms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CofaReconciliationService
 *
 * Utility service to reconcile COFA balance with all transactions.
 * Used to fix data inconsistencies and missing transactions.
 */
@Service
public class CofaReconciliationService {

    private final CooperativeFundRepository cofaRepository;
    private final CooperativeFundTransactionRepository transactionRepository;
    private final MilkInquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    public CofaReconciliationService(
            CooperativeFundRepository cofaRepository,
            CooperativeFundTransactionRepository transactionRepository,
            MilkInquiryRepository inquiryRepository,
            UserRepository userRepository) {
        this.cofaRepository = cofaRepository;
        this.transactionRepository = transactionRepository;
        this.inquiryRepository = inquiryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get reconciliation report showing current state and discrepancies.
     */
    public Map<String, Object> getReconciliationReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        // Current COFA balance
        CooperativeFund cofa = cofaRepository.findById(1).orElse(null);
        BigDecimal currentBalance = cofa != null ? cofa.getBalance() : BigDecimal.ZERO;
        report.put("currentBalance", currentBalance);

        // All transactions
        List<CooperativeFundTransaction> allTransactions = transactionRepository.findAll();
        allTransactions.sort(Comparator.comparing(CooperativeFundTransaction::getCreatedAt));
        report.put("totalTransactions", allTransactions.size());

        // Calculate expected balance from transactions
        BigDecimal calculatedBalance = BigDecimal.ZERO;
        for (CooperativeFundTransaction txn : allTransactions) {
            calculatedBalance = calculatedBalance.add(txn.getAmount());
        }
        report.put("calculatedBalance", calculatedBalance);

        // Discrepancy
        BigDecimal discrepancy = currentBalance.subtract(calculatedBalance);
        report.put("discrepancy", discrepancy);
        report.put("hasDiscrepancy", discrepancy.compareTo(BigDecimal.ZERO) != 0);

        // Transaction breakdown
        Map<String, Object> breakdown = new LinkedHashMap<>();
        for (CooperativeFundTransaction.TransactionType type : CooperativeFundTransaction.TransactionType.values()) {
            BigDecimal sum = allTransactions.stream()
                    .filter(t -> t.getTransactionType() == type)
                    .map(CooperativeFundTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long count = allTransactions.stream()
                    .filter(t -> t.getTransactionType() == type)
                    .count();

            Map<String, Object> typeInfo = new LinkedHashMap<>();
            typeInfo.put("count", count);
            typeInfo.put("total", sum);
            breakdown.put(type.name(), typeInfo);
        }
        report.put("transactionBreakdown", breakdown);

        // Find paid inquiries
        List<MilkInquiry> paidInquiries = inquiryRepository.findByStatusOrderBySubmittedAtDesc(MilkInquiry.Status.PAID);
        report.put("totalPaidInquiries", paidInquiries.size());

        // Find paid inquiries with no matching transaction
        Set<String> transactionRefs = allTransactions.stream()
                .filter(t -> t.getTransactionType() == CooperativeFundTransaction.TransactionType.MILK_SALE)
                .map(CooperativeFundTransaction::getReferenceNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<MilkInquiry> missingInquiries = paidInquiries.stream()
                .filter(inq -> !transactionRefs.contains(inq.getReferenceNo()))
                .collect(Collectors.toList());

        report.put("paidInquiriesWithoutTransaction", missingInquiries.size());
        report.put("missingInquiries", missingInquiries);

        // Calculate total missing amount
        BigDecimal missingAmount = missingInquiries.stream()
                .map(MilkInquiry::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.put("missingAmount", missingAmount);

        return report;
    }

    /**
     * Create transactions for paid inquiries that are missing from transaction history.
     * This will add the missing revenue to COFA.
     */
    @Transactional
    public Map<String, Object> addMissingInquiryTransactions() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Get all transactions
        List<CooperativeFundTransaction> allTransactions = transactionRepository.findAll();
        Set<String> transactionRefs = allTransactions.stream()
                .filter(t -> t.getTransactionType() == CooperativeFundTransaction.TransactionType.MILK_SALE)
                .map(CooperativeFundTransaction::getReferenceNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Find paid inquiries without transactions
        List<MilkInquiry> paidInquiries = inquiryRepository.findByStatusOrderBySubmittedAtDesc(MilkInquiry.Status.PAID);
        List<MilkInquiry> missingInquiries = paidInquiries.stream()
                .filter(inq -> !transactionRefs.contains(inq.getReferenceNo()))
                .filter(inq -> inq.getTotalAmount() != null && inq.getTotalAmount().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(MilkInquiry::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        if (missingInquiries.isEmpty()) {
            result.put("success", true);
            result.put("message", "No missing transactions found. COFA is up to date.");
            result.put("added", 0);
            return result;
        }

        // Get current COFA
        CooperativeFund cofa = cofaRepository.findById(1)
                .orElseGet(() -> {
                    CooperativeFund newCofa = new CooperativeFund();
                    newCofa.setBalance(BigDecimal.ZERO);
                    return cofaRepository.save(newCofa);
                });

        BigDecimal currentBalance = cofa.getBalance();
        List<String> addedRefs = new ArrayList<>();

        // Add missing transactions
        for (MilkInquiry inquiry : missingInquiries) {
            currentBalance = currentBalance.add(inquiry.getTotalAmount());

            // Get the accountant who confirmed payment (or any accountant)
            User accountant = inquiry.getPaymentConfirmedBy();
            if (accountant == null) {
                accountant = userRepository.findAll().stream()
                        .filter(u -> "ACCOUNTS_OFFICER".equals(u.getRole()))
                        .findFirst()
                        .orElse(null);
            }

            // Create transaction
            CooperativeFundTransaction transaction = CooperativeFundTransaction.builder()
                    .transactionType(CooperativeFundTransaction.TransactionType.MILK_SALE)
                    .amount(inquiry.getTotalAmount())
                    .balanceAfter(currentBalance)
                    .referenceNo(inquiry.getReferenceNo())
                    .description("Milk sale payment from " + inquiry.getCompanyName() + " (Reconciliation)")
                    .performedBy(accountant)
                    .createdAt(inquiry.getPaidAt() != null ? inquiry.getPaidAt() : LocalDateTime.now())
                    .build();

            transactionRepository.save(transaction);
            addedRefs.add(inquiry.getReferenceNo());
        }

        // Update COFA balance
        cofa.setBalance(currentBalance);
        cofa.setUpdatedAt(LocalDateTime.now());
        cofaRepository.save(cofa);

        result.put("success", true);
        result.put("message", "Successfully added " + missingInquiries.size() + " missing transactions to COFA");
        result.put("added", missingInquiries.size());
        result.put("addedReferences", addedRefs);
        result.put("previousBalance", currentBalance.subtract(
                missingInquiries.stream()
                        .map(MilkInquiry::getTotalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        ));
        result.put("newBalance", currentBalance);

        return result;
    }

    /**
     * Recalculate COFA balance from all transactions.
     * WARNING: This will overwrite the current balance!
     */
    @Transactional
    public Map<String, Object> recalculateBalance() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Get current COFA
        CooperativeFund cofa = cofaRepository.findById(1)
                .orElseGet(() -> {
                    CooperativeFund newCofa = new CooperativeFund();
                    newCofa.setBalance(BigDecimal.ZERO);
                    return cofaRepository.save(newCofa);
                });

        BigDecimal oldBalance = cofa.getBalance();

        // Calculate from all transactions
        List<CooperativeFundTransaction> allTransactions = transactionRepository.findAll();
        allTransactions.sort(Comparator.comparing(CooperativeFundTransaction::getCreatedAt));

        BigDecimal calculatedBalance = BigDecimal.ZERO;
        for (CooperativeFundTransaction txn : allTransactions) {
            calculatedBalance = calculatedBalance.add(txn.getAmount());
        }

        // Update COFA
        cofa.setBalance(calculatedBalance);
        cofa.setUpdatedAt(LocalDateTime.now());
        cofaRepository.save(cofa);

        result.put("success", true);
        result.put("message", "COFA balance recalculated from " + allTransactions.size() + " transactions");
        result.put("oldBalance", oldBalance);
        result.put("newBalance", calculatedBalance);
        result.put("difference", calculatedBalance.subtract(oldBalance));

        return result;
    }
}
