package com.isg.kyc.kyc;

import com.isg.kyc.model.KycType;

public interface KycProvider {
    KycVerificationResult verify(KycType type, String identifier);
}
