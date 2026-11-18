package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.MilkDelivery;
import com.dairycoopms.dairycoopms.model.MilkSale;
import com.dairycoopms.dairycoopms.model.ProcessingCompany;
import com.dairycoopms.dairycoopms.repository.MilkDeliveryRepository;
import com.dairycoopms.dairycoopms.repository.MilkSaleRepository;
import com.dairycoopms.dairycoopms.repository.ProcessingCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * SalesService
 *
 * Business logic for the Sales Officer module:
 *   - View milk inventory (collected milk available for sale)
 *   - Record sales to processing companies
 *   - Track sales history and payment status
 *   - Manage processing companies
 *   - Dashboard statistics
 */
@Service
@Transactional(readOnly = true)
public class SalesService {

    private final MilkDeliveryRepository    milkDeliveryRepository;
    private final MilkSaleRepository        milkSaleRepository;
    private final ProcessingCompanyRepository processingCompanyRepository;

    public SalesService(MilkDeliveryRepository milkDeliveryRepository,
                        MilkSaleRepository milkSaleRepository,
                        ProcessingCompanyRepository processingCompanyRepository) {
        this.milkDeliveryRepository    = milkDeliveryRepository;
        this.milkSaleRepository        = milkSaleRepository;
        this.processingCompanyRepository = processingCompanyRepository;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD STATISTICS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get dashboard summary statistics for Sales Officer.
     */
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        int year  = today.getYear();
        int month = today.getMonthValue();

        // Today's collected milk
        BigDecimal todayCollected = milkDeliveryRepository
                .sumQuantityByDeliveryDate(today)
                .orElse(BigDecimal.ZERO);
        summary.put("todayCollected", todayCollected);

        // This month's total collection
        BigDecimal monthlyCollected = getMonthlyCollectedMilk(year, month);
        summary.put("monthlyCollected", monthlyCollected);

        // This month's total sold
        BigDecimal monthlySold = getMonthlySoldMilk(year, month);
        summary.put("monthlySold", monthlySold);

        // Available inventory (collected - sold this month)
        BigDecimal availableInventory = monthlyCollected.subtract(monthlySold);
        if (availableInventory.compareTo(BigDecimal.ZERO) < 0) {
            availableInventory = BigDecimal.ZERO;
        }
        summary.put("availableInventory", availableInventory);

        // This month's revenue
        BigDecimal monthlyRevenue = milkSaleRepository
                .sumRevenueByYearAndMonth(year, month)
                .orElse(BigDecimal.ZERO);
        summary.put("monthlyRevenue", monthlyRevenue);

        // Pending payments count
        long pendingPayments = milkSaleRepository.countByPaymentStatus("PENDING");
        summary.put("pendingPayments", pendingPayments);

        // Pending sales list (for quick action)
        List<MilkSale> pendingSales = milkSaleRepository.findByPaymentStatus("PENDING");
        summary.put("pendingSales", pendingSales);

        // Active processing companies
        long activeCompanies = processingCompanyRepository.findByStatus("ACTIVE").size();
        summary.put("activeCompanies", activeCompanies);

        // Recent sales (last 5)
        List<MilkSale> allSales = milkSaleRepository.findAll();
        allSales.sort((a, b) -> b.getSaleDate().compareTo(a.getSaleDate()));
        List<MilkSale> recentSales = allSales.size() > 5
                ? allSales.subList(0, 5)
                : allSales;
        summary.put("recentSales", recentSales);

        return summary;
    }

    /**
     * Get total milk collected in a given month.
     */
    public BigDecimal getMonthlyCollectedMilk(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();

        List<Object[]> dailyTotals = milkDeliveryRepository.getDailyTotals(from, to);
        return dailyTotals.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total milk sold in a given month.
     */
    public BigDecimal getMonthlySoldMilk(int year, int month) {
        List<MilkSale> allSales = milkSaleRepository.findAll();
        return allSales.stream()
                .filter(s -> s.getSaleDate().getYear() == year
                        && s.getSaleDate().getMonthValue() == month)
                .map(MilkSale::getQuantityLitres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MILK INVENTORY
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get daily milk collection summary for a date range.
     * Returns list of [date, totalLitres, deliveryCount] for inventory view.
     */
    public List<Map<String, Object>> getDailyInventory(LocalDate from, LocalDate to) {
        List<Map<String, Object>> inventory = new ArrayList<>();

        List<Object[]> dailyTotals = milkDeliveryRepository.getDailyTotals(from, to);

        for (Object[] row : dailyTotals) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal litres = (BigDecimal) row[1];

            // Count deliveries for this date
            List<MilkDelivery> deliveries = milkDeliveryRepository.findByDeliveryDate(date);

            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", date);
            dayData.put("totalLitres", litres);
            dayData.put("deliveryCount", deliveries.size());
            dayData.put("uniqueMembers", deliveries.stream()
                    .map(d -> d.getMember().getId())
                    .distinct()
                    .count());

            inventory.add(dayData);
        }

        // Sort by date descending (most recent first)
        inventory.sort((a, b) -> ((LocalDate) b.get("date")).compareTo((LocalDate) a.get("date")));

        return inventory;
    }

    /**
     * Get detailed deliveries for a specific date.
     */
    public List<MilkDelivery> getDeliveriesByDate(LocalDate date) {
        return milkDeliveryRepository.findByDeliveryDate(date);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROCESSING COMPANIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get all processing companies.
     */
    public List<ProcessingCompany> getAllCompanies() {
        List<ProcessingCompany> companies = processingCompanyRepository.findAll();
        companies.sort(Comparator.comparing(ProcessingCompany::getName));
        return companies;
    }

    /**
     * Get active processing companies.
     */
    public List<ProcessingCompany> getActiveCompanies() {
        return processingCompanyRepository.findByStatusOrderByNameAsc("ACTIVE");
    }

    /**
     * Get a company by ID.
     */
    public Optional<ProcessingCompany> getCompanyById(UUID companyId) {
        return processingCompanyRepository.findById(companyId);
    }

    /**
     * Add a new processing company.
     */
    @Transactional
    public ProcessingCompany addCompany(String name, String contactPerson,
                                        String phone, String email, String address) {

        if (processingCompanyRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A company with this name already exists");
        }

        ProcessingCompany company = ProcessingCompany.builder()
                .name(name)
                .contactPerson(contactPerson)
                .phone(phone)
                .email(email)
                .address(address)
                .status("ACTIVE")
                .build();

        return processingCompanyRepository.save(company);
    }

    /**
     * Update a processing company.
     */
    @Transactional
    public ProcessingCompany updateCompany(UUID companyId, String name, String contactPerson,
                                           String phone, String email, String address) {

        ProcessingCompany company = processingCompanyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        // Check name uniqueness if changed
        if (!company.getName().equalsIgnoreCase(name)
                && processingCompanyRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("A company with this name already exists");
        }

        company.setName(name);
        company.setContactPerson(contactPerson);
        company.setPhone(phone);
        company.setEmail(email);
        company.setAddress(address);

        return processingCompanyRepository.save(company);
    }

    /**
     * Deactivate a processing company.
     */
    @Transactional
    public ProcessingCompany deactivateCompany(UUID companyId) {
        ProcessingCompany company = processingCompanyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        company.setStatus("INACTIVE");
        return processingCompanyRepository.save(company);
    }

    /**
     * Reactivate a processing company.
     */
    @Transactional
    public ProcessingCompany activateCompany(UUID companyId) {
        ProcessingCompany company = processingCompanyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        company.setStatus("ACTIVE");
        return processingCompanyRepository.save(company);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CHART DATA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get monthly sales data for chart (current year).
     */
    public BigDecimal[] getMonthlySalesData(int year) {
        BigDecimal[] data = new BigDecimal[12];
        Arrays.fill(data, BigDecimal.ZERO);

        List<MilkSale> allSales = milkSaleRepository.findAll();

        for (MilkSale sale : allSales) {
            if (sale.getSaleDate().getYear() == year) {
                int monthIndex = sale.getSaleDate().getMonthValue() - 1;
                data[monthIndex] = data[monthIndex].add(sale.getQuantityLitres());
            }
        }

        return data;
    }

    /**
     * Get monthly revenue data for chart (current year).
     */
    public BigDecimal[] getMonthlyRevenueData(int year) {
        BigDecimal[] data = new BigDecimal[12];
        Arrays.fill(data, BigDecimal.ZERO);

        List<MilkSale> allSales = milkSaleRepository.findAll();

        for (MilkSale sale : allSales) {
            if (sale.getSaleDate().getYear() == year) {
                int monthIndex = sale.getSaleDate().getMonthValue() - 1;
                data[monthIndex] = data[monthIndex].add(sale.getTotalRevenue());
            }
        }

        return data;
    }

    /**
     * Get sales breakdown by company for a given month.
     */
    public List<Object[]> getSalesByCompanyForMonth(int year, int month) {
        return milkSaleRepository.getRevenueByCompany(year, month);
    }
}