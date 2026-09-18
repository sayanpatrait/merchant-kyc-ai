package com.isg.kyc.kyc;

import com.isg.kyc.model.KycType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MockKycProvider implements KycProvider {

    private static final Logger log = LoggerFactory.getLogger(MockKycProvider.class);
    private static final Random RND = new Random();

    @Override
    public KycVerificationResult verify(KycType type, String identifier) {
        log.info("🔌 Verifying {} — {}", type, identifier);

        // Simulate latency
        try { Thread.sleep(50 + RND.nextInt(150)); } catch (InterruptedException ignored) {}

        if (identifier == null || identifier.isBlank()) {
            return KycVerificationResult.failure("NOT_FOUND");
        }

        // 90% success, 10% various failures
        int roll = RND.nextInt(100);

        if (roll < 90) {
            return KycVerificationResult.success();
        } else if (roll < 94) {
            return KycVerificationResult.failure("PROVIDER_TIMEOUT");
        } else if (roll < 97) {
            return KycVerificationResult.failure("INVALID_FORMAT");
        } else if (roll < 99) {
            return KycVerificationResult.failure("NAME_MISMATCH");
        } else {
            return KycVerificationResult.failure("RATE_LIMITED");
        }
    }
}