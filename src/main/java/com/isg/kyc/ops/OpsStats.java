package com.isg.kyc.ops;

import org.springframework.stereotype.Component;

import com.isg.kyc.model.KycStatus;
import com.isg.kyc.repository.CustomerRepository;
import com.isg.kyc.repository.FeedbackRepository;
import com.isg.kyc.repository.MerchantRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpsStats {

    private final MerchantRepository merchantRepo;
    private final CustomerRepository customerRepo;
    private final FeedbackRepository feedbackRepo;

    public OpsStats(MerchantRepository merchantRepo,
                    CustomerRepository customerRepo,
                    FeedbackRepository feedbackRepo) {
        this.merchantRepo = merchantRepo;
        this.customerRepo = customerRepo;
        this.feedbackRepo = feedbackRepo;
    }

    public Map<String, Object> snapshot() {
        long merchantTotal = merchantRepo.count();

        long merVerified = merchantRepo.countByPanStatus(KycStatus.VERIFIED)
                         + merchantRepo.countByAadhaarStatus(KycStatus.VERIFIED)
                         + merchantRepo.countByGstStatus(KycStatus.VERIFIED);
        long merPending  = merchantRepo.countByPanStatus(KycStatus.PENDING)
                         + merchantRepo.countByAadhaarStatus(KycStatus.PENDING)
                         + merchantRepo.countByGstStatus(KycStatus.PENDING);
        long merFailed   = merchantRepo.countByPanStatus(KycStatus.FAILED)
                         + merchantRepo.countByAadhaarStatus(KycStatus.FAILED)
                         + merchantRepo.countByGstStatus(KycStatus.FAILED);

        long customerTotal = customerRepo.count();
        long custVerified = customerRepo.findAll().stream()
                .filter(c -> c.getKycStatus() == KycStatus.VERIFIED).count();
        long custPending  = customerRepo.findAll().stream()
                .filter(c -> c.getKycStatus() == KycStatus.PENDING).count();
        long custFailed   = customerRepo.findAll().stream()
                .filter(c -> c.getKycStatus() == KycStatus.FAILED).count();

        long feedbackCount = feedbackRepo.count();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", Instant.now().toString());
        m.put("merchants", Map.of(
                "total", merchantTotal,
                "verified", merVerified,
                "pending", merPending,
                "failed", merFailed
        ));
        m.put("customers", Map.of(
                "total", customerTotal,
                "verified", custVerified,
                "pending", custPending,
                "failed", custFailed
        ));
        m.put("feedback", feedbackCount);
        m.put("sparkline", buildSparkline(merchantTotal, customerTotal, merVerified + custVerified));
        return m;
    }

    /** Simple deterministic curve for the sparkline (would be replaced by historical data). */
    private List<Integer> buildSparkline(long a, long b, long c) {
        int base = (int) Math.min(50, (a + b + c) % 40 + 5);
        return List.of(
                base,
                Math.min(99, base + 4),
                Math.min(99, base + 2),
                Math.min(99, base + 8),
                Math.min(99, base + 6),
                Math.min(99, base + 12),
                Math.min(99, base + 10),
                Math.min(99, base + 15),
                Math.min(99, base + 13),
                Math.min(99, base + 18),
                Math.min(99, base + 20),
                Math.min(99, base + 22)
        );
    }
}