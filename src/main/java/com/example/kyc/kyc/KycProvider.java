package com.example.kyc.kyc;

import com.example.kyc.model.KycType;

public interface KycProvider {
    KycVerificationResult verify(KycType type, String identifier);
}
