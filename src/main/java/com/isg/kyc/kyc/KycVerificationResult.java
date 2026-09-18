package com.isg.kyc.kyc;

import com.isg.kyc.model.KycStatus;

public record KycVerificationResult(KycStatus status, String reason) {

    public static KycVerificationResult success() {
        return new KycVerificationResult(KycStatus.VERIFIED, null);
    }

    public static KycVerificationResult failure(String reason) {
        return new KycVerificationResult(KycStatus.FAILED, reason);
    }

    public static KycVerificationResult pending() {
        return new KycVerificationResult(KycStatus.PENDING, null);
    }
}