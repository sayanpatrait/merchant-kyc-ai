package com.example.kyc.ai;

import com.example.kyc.model.Merchant;
import com.example.kyc.repository.MerchantRepository;
import com.example.kyc.service.KycService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MerchantTools {

    private final KycService kycService;
    private final MerchantRepository merchantRepository;

    public MerchantTools(KycService kycService, MerchantRepository merchantRepository) {
        this.kycService = kycService;
        this.merchantRepository = merchantRepository;
    }

    @Tool(description = "Get an overall KYC summary for MERCHANTS: total count and breakdown of verified, pending, and failed KYC checks. Use when the user asks about merchant KYC stats.")
    public String getMerchantSummary() {
        return kycService.summary();
    }

    @Tool(description = "Find MERCHANTS by name, PAN, email, or GSTIN. All fields use case-insensitive partial match. Returns a list with ID, name, PAN, GSTIN and per-check statuses. Call this FIRST when the user refers to a merchant by any identifier.")
    public String findMerchants(String query) {
        if (query == null || query.isBlank()) return "No merchant search query provided.";

        String q = query.toLowerCase().trim();
        List<Merchant> matches = merchantRepository.findAll().stream()
                .filter(m ->
                        (m.getMerchantName() != null && m.getMerchantName().toLowerCase().contains(q))
                     || (m.getPan()          != null && m.getPan().toLowerCase().contains(q))
                     || (m.getEmail()        != null && m.getEmail().toLowerCase().contains(q))
                     || (m.getGstin()        != null && m.getGstin().toLowerCase().contains(q))
                )
                .limit(10)
                .collect(Collectors.toList());

        if (matches.isEmpty()) return "No merchants matched: " + query;

        StringBuilder sb = new StringBuilder("Found " + matches.size() + " merchant(s):\n");
        for (Merchant m : matches) {
            sb.append("• ID=").append(m.getId())
              .append(" | ").append(m.getMerchantName())
              .append(" | PAN=").append(m.getPanStatus())
              .append(", AADHAAR=").append(m.getAadhaarStatus())
              .append(", GST=").append(m.getGstStatus())
              .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Get the full KYC details of a MERCHANT by numeric ID. Returns name, contact, masked identifiers, and each KYC status.")
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

    @Tool(description = "Start verification for every remaining pending PAN, Aadhaar, and GST check across ALL merchants. Use only when the user explicitly asks to verify all remaining merchant KYC.")
    public String verifyAllRemainingMerchants() {
        kycService.verifyRemaining();
        return "Verification of all remaining MERCHANT KYC checks has been started asynchronously.";
    }

    private String mask(String value) {
        if (value == null || value.length() < 4) return value;
        return "****" + value.substring(value.length() - 4);
    }
}