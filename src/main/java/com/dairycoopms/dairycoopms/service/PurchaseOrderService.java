package com.dairycoopms.dairycoopms.service;

import com.dairycoopms.dairycoopms.model.MilkInquiry;
import com.dairycoopms.dairycoopms.model.MilkSale;
import com.dairycoopms.dairycoopms.model.PurchaseOrder;
import com.dairycoopms.dairycoopms.model.User;
import com.dairycoopms.dairycoopms.repository.MilkInquiryRepository;
import com.dairycoopms.dairycoopms.repository.MilkSaleRepository;
import com.dairycoopms.dairycoopms.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final MilkInquiryRepository milkInquiryRepository;
    private final MilkSaleRepository saleRepository;

    /**
     * Generate PO number based on company name
     * Format: PO-COMPANY_ABBREV-YYYY-NNNN
     * Example: PO-PMT-2026-0001 for Parmalat
     */
    public String generatePoNumber(String companyName) {
        // Get company abbreviation (first 3 letters, uppercase)
        String abbrev = companyName.length() >= 3
                ? companyName.substring(0, 3).toUpperCase()
                : companyName.toUpperCase();

        // Remove spaces and special characters
        abbrev = abbrev.replaceAll("[^A-Z]", "");

        // Get year
        int year = LocalDate.now().getYear();

        // Get latest PO number for this company
        List<String> existingPos = purchaseOrderRepository.findLatestPoNumberForCompany(companyName);

        int nextNumber = 1;
        if (!existingPos.isEmpty()) {
            String latestPo = existingPos.get(0);
            // Extract number from PO-XXX-YYYY-NNNN
            try {
                String[] parts = latestPo.split("-");
                if (parts.length == 4) {
                    nextNumber = Integer.parseInt(parts[3]) + 1;
                }
            } catch (Exception e) {
                log.warn("Could not parse PO number: " + latestPo, e);
            }
        }

        return String.format("PO-%s-%d-%04d", abbrev, year, nextNumber);
    }

    /**
     * Create draft PO from approved inquiry
     */
    @Transactional
    public PurchaseOrder createFromInquiry(UUID inquiryId, BigDecimal unitPrice,
                                           LocalDate deliveryDate, String deliveryAddress) {
        // Get inquiry
        MilkInquiry inquiry = milkInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));

        // Validate inquiry is approved
        if (inquiry.getStatus() != MilkInquiry.Status.APPROVED) {
            throw new IllegalStateException("Can only create PO from approved inquiry");
        }

        // Check if PO already exists
        if (purchaseOrderRepository.existsByInquiryId(inquiryId)) {
            throw new IllegalStateException("PO already exists for this inquiry");
        }

        // Generate PO number
        String poNumber = generatePoNumber(inquiry.getCompanyName());

        // Create PO
        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(poNumber)
                .inquiry(inquiry)
                .companyName(inquiry.getCompanyName())
                .contactPerson(inquiry.getContactPerson())
                .email(inquiry.getEmail())
                .phone(inquiry.getPhone())
                .poDate(LocalDate.now())
                .quantityLitres(inquiry.getRequestedLitres())
                .unitPrice(unitPrice)
                .deliveryDate(deliveryDate)
                .deliveryAddress(deliveryAddress)
                .paymentTerms("Net 30 days")
                .status(PurchaseOrder.Status.DRAFT)
                .build();

        // Save PO
        po = purchaseOrderRepository.save(po);

        // Update inquiry
        inquiry.setPurchaseOrderNo(poNumber);
        inquiry.setPoCreatedAt(LocalDateTime.now());
        inquiry.setStatus(MilkInquiry.Status.PO_CREATED);
        milkInquiryRepository.save(inquiry);

        log.info("Created PO {} from inquiry {}", poNumber, inquiry.getReferenceNo());

        return po;
    }

    /**
     * Submit PO to sales
     */
    @Transactional
    public PurchaseOrder submitPo(UUID poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found"));

        // Validate
        if (!po.canSubmit()) {
            throw new IllegalStateException("PO cannot be submitted - incomplete data");
        }

        // Submit
        po.submit();
        po = purchaseOrderRepository.save(po);

        // Update inquiry status
        if (po.getInquiry() != null) {
            MilkInquiry inquiry = po.getInquiry();
            inquiry.setStatus(MilkInquiry.Status.PO_SUBMITTED);
            inquiry.setPoSubmittedAt(LocalDateTime.now());
            milkInquiryRepository.save(inquiry);
        }

        log.info("Submitted PO {} to sales", po.getPoNumber());

        return po;
    }

    /**
     * Submit PO by inquiry ID (convenience method for portal)
     */
    @Transactional
    public PurchaseOrder submitPurchaseOrder(UUID inquiryId) {
        // Find PO by inquiry
        PurchaseOrder po = purchaseOrderRepository.findByInquiryId(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("No Purchase Order found for this inquiry"));

        // Submit it
        return submitPo(po.getId());
    }

    /**
     * Approve PO (Sales)
     */
    @Transactional
    public PurchaseOrder approvePo(UUID poId, User approvedBy) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found"));

        // Validate
        if (!po.canApprove()) {
            throw new IllegalStateException("PO cannot be approved in current state");
        }

        // Approve
        po.approve(approvedBy);
        po = purchaseOrderRepository.save(po);

        // Update inquiry status to PO_APPROVED
        // MilkSale will be created when company confirms delivery
        if (po.getInquiry() != null) {
            MilkInquiry inquiry = po.getInquiry();
            inquiry.setStatus(MilkInquiry.Status.PO_APPROVED);
            milkInquiryRepository.save(inquiry);
        }

        log.info("Approved PO {} by {} - awaiting delivery confirmation",
                po.getPoNumber(), approvedBy.getEmail());

        return po;
    }

    /**
     * Reject PO (Sales)
     */
    @Transactional
    public PurchaseOrder rejectPo(UUID poId, String reason) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found"));

        // Validate
        if (!po.canReject()) {
            throw new IllegalStateException("PO cannot be rejected in current state");
        }

        // Reject
        po.reject(reason);
        po = purchaseOrderRepository.save(po);

        // Update inquiry status
        if (po.getInquiry() != null) {
            MilkInquiry inquiry = po.getInquiry();
            inquiry.setStatus(MilkInquiry.Status.REJECTED);
            inquiry.setRejectionReason(reason);
            milkInquiryRepository.save(inquiry);
        }

        log.info("Rejected PO {}: {}", po.getPoNumber(), reason);

        return po;
    }

    /**
     * Confirm delivery (Company confirms they received the milk)
     * This creates the MilkSale record and updates inquiry to DELIVERED status
     */
    @Transactional
    public MilkSale confirmDelivery(UUID inquiryId) {
        MilkInquiry inquiry = milkInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("Inquiry not found"));

        // Validate status
        if (inquiry.getStatus() != MilkInquiry.Status.PO_APPROVED) {
            throw new IllegalStateException(
                    "Can only confirm delivery for PO_APPROVED status. Current: " + inquiry.getStatus());
        }

        // Guard against a repeat call: status intentionally stays PO_APPROVED
        // after the first confirmation (until Sales generates the invoice).
        if (inquiry.getDeliveredAt() != null) {
            throw new IllegalStateException("Delivery has already been confirmed for this order.");
        }

        // Get the approved PO
        PurchaseOrder po = purchaseOrderRepository.findByInquiryId(inquiryId)
                .stream()
                .filter(p -> p.getStatus() == PurchaseOrder.Status.APPROVED)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No approved PO found for this inquiry"));

        // approveInquiry() already created a placeholder MilkSale when the
        // inquiry was first approved (a required step before a PO can even
        // be created). Finalize that same record with the PO's actual
        // negotiated terms now that delivery is confirmed, rather than
        // creating a second one — every PO-routed inquiry would otherwise
        // end up with two MilkSale rows sharing the same referenceNo.
        MilkSale sale = saleRepository.findByReferenceNo(inquiry.getReferenceNo())
                .orElseGet(() -> MilkSale.builder()
                        .referenceNo(inquiry.getReferenceNo())
                        .paymentStatus("PENDING")
                        .build());

        sale.setProcessingCompany(null);  // No registered company link
        sale.setQuantityLitres(po.getQuantityLitres());
        sale.setPricePerLitre(po.getUnitPrice());
        sale.setTotalRevenue(po.getTotalAmount());
        sale.setSaleDate(LocalDate.now());
        sale.setNotes("Delivery confirmed for PO: " + po.getPoNumber() + " | Company: " + po.getCompanyName());

        sale = saleRepository.save(sale);

        // Mark delivery as confirmed
        inquiry.setDeliveredAt(LocalDateTime.now());
        milkInquiryRepository.save(inquiry);

        // Keep status as PO_APPROVED until Sales generates invoice
        // Status will change to INVOICED when Sales clicks "Generate Invoice"

        log.info("Delivery confirmed for PO {} - Created MilkSale: {} L @ ZMW {} = ZMW {} - Awaiting invoice generation",
                po.getPoNumber(),
                po.getQuantityLitres(),
                po.getUnitPrice(),
                po.getTotalAmount());

        return sale;
    }

    /**
     * Link PO to created order/invoice
     */
    @Transactional
    public void linkToOrder(UUID poId, MilkSale sale) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found"));

        po.linkToOrder(sale);
        purchaseOrderRepository.save(po);

        // Update inquiry
        if (po.getInquiry() != null) {
            MilkInquiry inquiry = po.getInquiry();
            inquiry.setStatus(MilkInquiry.Status.INVOICED);
            inquiry.setInvoiceNo(sale.getReferenceNo());
            inquiry.setInvoicedAt(LocalDateTime.now());
            milkInquiryRepository.save(inquiry);
        }

        log.info("Linked PO {} to order {}", po.getPoNumber(), sale.getReferenceNo());
    }

    /**
     * Get all POs (for sales)
     */
    public List<PurchaseOrder> getAllPos() {
        return purchaseOrderRepository.findAll();
    }

    /**
     * Get submitted POs (for sales to review)
     */
    public List<PurchaseOrder> getSubmittedPos() {
        return purchaseOrderRepository.findAllSubmitted();
    }

    /**
     * Get approved POs without invoice
     */
    public List<PurchaseOrder> getApprovedPosWithoutInvoice() {
        return purchaseOrderRepository.findApprovedWithoutInvoice();
    }

    /**
     * Get POs by company email (for company portal)
     */
    public List<PurchaseOrder> getPosByEmail(String email) {
        return purchaseOrderRepository.findByEmailOrderByCreatedAtDesc(email);
    }

    /**
     * Get PO by ID
     */
    public PurchaseOrder getPoById(UUID id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PO not found"));
    }

    /**
     * Get PO by PO number
     */
    public PurchaseOrder getPoByNumber(String poNumber) {
        return purchaseOrderRepository.findByPoNumber(poNumber)
                .orElseThrow(() -> new IllegalArgumentException("PO not found"));
    }

    /**
     * Check if inquiry has PO
     */
    public boolean inquiryHasPo(UUID inquiryId) {
        return purchaseOrderRepository.existsByInquiryId(inquiryId);
    }

    /**
     * Get PO by inquiry ID
     */
    public PurchaseOrder getPoByInquiryId(UUID inquiryId) {
        return purchaseOrderRepository.findByInquiryId(inquiryId).orElse(null);
    }

    /**
     * Count POs by status
     */
    public long countByStatus(PurchaseOrder.Status status) {
        return purchaseOrderRepository.countByStatus(status);
    }

    /**
     * Update PO details (before submission)
     */
    @Transactional
    public PurchaseOrder updatePo(UUID poId, BigDecimal unitPrice, LocalDate deliveryDate,
                                  String deliveryAddress, String specialInstructions) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found"));

        // Can only update draft POs
        if (po.getStatus() != PurchaseOrder.Status.DRAFT) {
            throw new IllegalStateException("Can only update draft POs");
        }

        // Update fields
        if (unitPrice != null) {
            po.setUnitPrice(unitPrice);
        }
        if (deliveryDate != null) {
            po.setDeliveryDate(deliveryDate);
        }
        if (deliveryAddress != null) {
            po.setDeliveryAddress(deliveryAddress);
        }
        if (specialInstructions != null) {
            po.setSpecialInstructions(specialInstructions);
        }

        return purchaseOrderRepository.save(po);
    }
}