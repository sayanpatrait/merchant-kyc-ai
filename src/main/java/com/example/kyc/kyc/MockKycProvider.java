package com.example.kyc.kyc;

import org.springframework.stereotype.Component;

import com.example.kyc.model.KycStatus;
import com.example.kyc.model.KycType;

import java.util.UUID;

@Component
public class MockKycProvider implements KycProvider {
    /*
     * Replace this class with adapters for your actual PAN/Aadhaar/GST vendors.
     * This demo provider marks non-blank identifiers as VERIFIED.
     */
    @Override
    public KycVerificationResult verify(KycType type, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return new KycVerificationResult(KycStatus.FAILED, null, type + " identifier is missing");
        }
        return new KycVerificationResult(
                KycStatus.VERIFIED,
                "DEMO-" + UUID.randomUUID(),
                "Demo verification successful");
    }
}
