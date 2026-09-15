package com.example.kyc.kyc;

import com.example.kyc.model.KycStatus;

public record KycVerificationResult(KycStatus status, String providerReference, String message) {}
