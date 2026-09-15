package com.example.kyc.ai;

import com.example.kyc.model.Customer;
import com.example.kyc.model.Merchant;
import com.example.kyc.repository.CustomerRepository;
import com.example.kyc.repository.MerchantRepository;
import com.example.kyc.service.CustomerService;
import com.example.kyc.service.KycService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class KycAiTools {

    private final KycService kycService;
    private final CustomerService customerService;
    private final MerchantRepository merchantRepository;
    private final CustomerRepository customerRepository;

    public KycAiTools(KycService kycService,
                      CustomerService customerService,
                      MerchantRepository merchantRepository,
                      CustomerRepository customerRepository) {
        this.kycService = kycService;
        this.customerService = customerService;
        this.merchantRepository = merchantRepository;
        this.customerRepository = customerRepository;
    }

    /* ============================================================
       MERCHANT TOOLS
       ============================================================ */

    @Tool(description = "Get an overall KYC summary: total merchants and counts of verified, pending, and failed KYC checks across all merchants.")
    public String getKycSummary() {
        return kycService.summary();
    }

    @Tool(description = "Find merchants by name, PAN, or email. Name is a partial case-insensitive match. Returns a list of matches with ID, name, PAN, and statuses. Call this FIRST when the user refers to a merchant by name, PAN, or email.")
    public String findMerchants(String query) {
        if (query == null || query.isBlank()) return "No search query provided.";

        String q = query.toLowerCase().trim();
        List<Merchant> matches = merchantRepository.findAll().stream()
                .filter(m ->
                        (m.getMerchantName() != null && m.getMerchantName().toLowerCase().contains(q))
                     || (m.getPan()          != null && m.getPan().toLowerCase().equals(q))
                     || (m.getEmail()        != null && m.getEmail().toLowerCase().contains(q))
                )
                .limit(10)
                .collect(Collectors.toList());

        if (matches.isEmpty()) return "No merchants matched: " + query;

        StringBuilder sb = new StringBuilder("Found " + matches.size() + " merchant(s):\n");
        for (Merchant m : matches) {
            sb.append("• ID=").append(m.getId())
              .append(" | ").append(m.getMerchantName())
              .append(" | PAN=").append(m.getPan())
              .append(" | PAN=").append(m.getPanStatus())
              .append(", AADHAAR=").append(m.getAadhaarStatus())
              .append(", GST=").append(m.getGstStatus())
              .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Get the full KYC details of a merchant by numeric ID. Returns name, contact, masked identifiers, and each KYC status.")
    public String getMerchantDetails(long merchantId) {
        Merchant m = merchantRepository.findById(merchantId).orElse(null);
        if (m == null) return "Merchant not found: " + merchantId;
        return "Merchant ID " + m.getId()
                + "\nName: " + m.getMerchantName()
                + "\nEmail: " + m.getEmail()
                + "\nPhone: " + m.getPhone()
                + "\nPAN: " + mask(m.getPan()) + " (" + m.getPanStatus() + ")"
                + "\nAadhaar: " + mask(m.getAadhaar()) + " (" + m.getAadhaarStatus() + ")"
                + "\nGSTIN: " + m.getGstin() + " (" + m.getGstStatus() + ")";
    }

    @Tool(description = "Verify all pending KYC checks for ONE merchant by numeric ID. Use only after resolving the merchant's ID via findMerchants.")
    public String verifyMerchant(long merchantId) {
        kycService.verify(merchantId);
        return "KYC verification completed for merchant " + merchantId + ".";
    }

    @Tool(description = "Start verification for every remaining pending PAN, Aadhaar, and GST KYC record across all merchants. Use only when the user explicitly asks to verify all remaining KYC.")
    public String verifyAllRemainingKyc() {
        kycService.verifyRemaining();
        return "Verification of all remaining KYC checks has been started asynchronously.";
    }

    /* ============================================================
       CUSTOMER TOOLS
       ============================================================ */

    @Tool(description = "Get an overall summary of customers: total count and breakdown by KYC status.")
    public String getCustomerSummary() {
        List<Customer> all = customerRepository.findAll();
        long total    = all.size();
        long verified = all.stream().filter(c -> c.getKycStatus().name().equals("VERIFIED")).count();
        long pending  = all.stream().filter(c -> c.getKycStatus().name().equals("PENDING")).count();
        long failed   = all.stream().filter(c -> c.getKycStatus().name().equals("FAILED")).count();
        return "Customers: " + total + ". Verified: " + verified
                + ", pending: " + pending + ", failed: " + failed + ".";
    }

    @Tool(description = "Find customers by name, email, ID number, city, or state. All fields use case-insensitive partial match. Returns a list of matches with ID, name, city, state, and KYC status. Call this FIRST when the user refers to a customer by name, email, ID, city, or state.")
    public String findCustomers(String query) {
        if (query == null || query.isBlank()) return "No search query provided.";

        String q = query.toLowerCase().trim();
        List<Customer> matches = customerRepository.findAll().stream()
                .filter(c ->
                        (c.getFullName() != null && c.getFullName().toLowerCase().contains(q))
                     || (c.getEmail()    != null && c.getEmail().toLowerCase().contains(q))
                     || (c.getIdNumber() != null && c.getIdNumber().toLowerCase().equals(q))
                     || (c.getCity()     != null && c.getCity().toLowerCase().contains(q))
                     || (c.getState()    != null && c.getState().toLowerCase().contains(q))
                )
                .limit(10)
                .collect(Collectors.toList());

        if (matches.isEmpty()) return "No customers matched: " + query;

        StringBuilder sb = new StringBuilder("Found " + matches.size() + " customer(s):\n");
        for (Customer c : matches) {
            sb.append("• ID=").append(c.getId())
              .append(" | ").append(c.getFullName())
              .append(" | ").append(c.getCity())
              .append(", ").append(c.getState())
              .append(" | ").append(c.getKycStatus())
              .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Get the full details of a customer by numeric ID.")
    public String getCustomerDetails(long customerId) {
        Customer c = customerRepository.findById(customerId).orElse(null);
        if (c == null) return "Customer not found: " + customerId;
        return "Customer ID " + c.getId()
                + "\nName: " + c.getFullName()
                + "\nEmail: " + c.getEmail()
                + "\nPhone: " + c.getPhone()
                + "\nID Type: " + c.getIdType()
                + "\nID Number: " + mask(c.getIdNumber())
                + "\nStatus: " + c.getKycStatus()
                + "\nCity/State: " + c.getCity() + ", " + c.getState();
    }

    @Tool(description = "Verify a single customer's pending KYC by numeric ID. Use only after resolving the customer's ID via findCustomers.")
    public String verifyCustomer(long customerId) {
        customerService.verify(customerId);
        return "KYC verification completed for customer " + customerId + ".";
    }

    /* ============================================================
       Utility
       ============================================================ */

    private String mask(String value) {
        if (value == null || value.length() < 4) return value;
        return "****" + value.substring(value.length() - 4);
    }
}