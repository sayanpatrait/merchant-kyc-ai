package com.example.kyc.ai;

import com.example.kyc.model.Customer;
import com.example.kyc.model.KycStatus;
import com.example.kyc.repository.CustomerRepository;
import com.example.kyc.service.CustomerService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomerTools {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;

    public CustomerTools(CustomerService customerService,
                         CustomerRepository customerRepository) {
        this.customerService = customerService;
        this.customerRepository = customerRepository;
    }

    @Tool(description = "Get an overall KYC summary for CUSTOMERS: total count and breakdown of verified, pending, and failed. Use when the user asks about customer KYC stats.")
    public String getCustomerSummary() {
        List<Customer> all = customerRepository.findAll();
        long total    = all.size();
        long verified = all.stream().filter(c -> c.getKycStatus() == KycStatus.VERIFIED).count();
        long pending  = all.stream().filter(c -> c.getKycStatus() == KycStatus.PENDING).count();
        long failed   = all.stream().filter(c -> c.getKycStatus() == KycStatus.FAILED).count();
        return "Customers: " + total + ". Verified: " + verified
                + ", pending: " + pending + ", failed: " + failed + ".";
    }

    @Tool(description = "Find CUSTOMERS by name, email, ID number, city, or state. All fields use case-insensitive partial match. Returns a list with ID, name, city, state, and KYC status. Call this FIRST when the user refers to a customer by any identifier.")
    public String findCustomers(String query) {
        if (query == null || query.isBlank()) return "No customer search query provided.";

        String q = query.toLowerCase().trim();
        List<Customer> matches = customerRepository.findAll().stream()
                .filter(c ->
                        (c.getFullName() != null && c.getFullName().toLowerCase().contains(q))
                     || (c.getEmail()    != null && c.getEmail().toLowerCase().contains(q))
                     || (c.getIdNumber() != null && c.getIdNumber().toLowerCase().contains(q))
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
              .append(" | ").append(c.getCity()).append(", ").append(c.getState())
              .append(" | ").append(c.getKycStatus())
              .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Get the full details of a CUSTOMER by numeric ID. Returns name, contact, masked ID, status, and location.")
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

    @Tool(description = "Verify a single CUSTOMER's pending KYC by numeric ID. Use only after resolving the customer's ID via findCustomers.")
    public String verifyCustomer(long customerId) {
        customerService.verify(customerId);
        return "KYC verification completed for customer " + customerId + ".";
    }

    @Tool(description = "Start verification for every remaining pending CUSTOMER KYC. Use only when the user explicitly asks to verify all remaining customer KYC.")
    public String verifyAllRemainingCustomers() {
        List<Customer> pending = customerRepository.findAll().stream()
                .filter(c -> c.getKycStatus() == KycStatus.PENDING)
                .collect(Collectors.toList());

        int count = 0;
        for (Customer c : pending) {
            try {
                customerService.verify(c.getId());
                count++;
            } catch (Exception ignored) { }
        }
        return "Verification completed for " + count + " pending customer(s).";
    }

    private String mask(String value) {
        if (value == null || value.length() < 4) return value;
        return "****" + value.substring(value.length() - 4);
    }
}