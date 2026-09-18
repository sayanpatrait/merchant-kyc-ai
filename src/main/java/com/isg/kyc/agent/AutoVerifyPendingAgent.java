package com.isg.kyc.agent;

import com.isg.kyc.model.KycStatus;
import com.isg.kyc.repository.CustomerRepository;
import com.isg.kyc.repository.MerchantRepository;
import com.isg.kyc.service.CustomerService;
import com.isg.kyc.service.KycService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoVerifyPendingAgent {

    private static final Logger log = LoggerFactory.getLogger(AutoVerifyPendingAgent.class);

    private final KycService kycService;
    private final CustomerService customerService;
    private final MerchantRepository merchantRepository;
    private final CustomerRepository customerRepository;
    private final AgentExecutor executor;              // ← CHANGED: use executor, not publisher

    @Value("${agent.enabled:true}")
    private boolean enabled;

    public AutoVerifyPendingAgent(KycService kycService,
                                  CustomerService customerService,
                                  MerchantRepository merchantRepository,
                                  CustomerRepository customerRepository,
                                  AgentExecutor executor) {   // ← CHANGED
        this.kycService = kycService;
        this.customerService = customerService;
        this.merchantRepository = merchantRepository;
        this.customerRepository = customerRepository;
        this.executor = executor;
    }

    @Scheduled(cron = "${agent.auto-verify-cron:0 * * * * *}")
    public void run() {
        if (!enabled) return;

        long pendingMerchants = merchantRepository.countByPanStatus(KycStatus.PENDING)
                              + merchantRepository.countByAadhaarStatus(KycStatus.PENDING)
                              + merchantRepository.countByGstStatus(KycStatus.PENDING);

        long pendingCustomers = customerRepository.findAll().stream()
                              .filter(c -> c.getKycStatus() == KycStatus.PENDING)
                              .count();

        if (pendingMerchants == 0 && pendingCustomers == 0) {
            log.debug("🤖 auto-verify: nothing pending — skipping");
            return;
        }

        log.info("🤖 auto-verify: {} pending merchant checks, {} pending customers",
                pendingMerchants, pendingCustomers);

        long t0 = System.currentTimeMillis();

        try {
            kycService.verifyRemaining();
            log.info("✅ Merchants queued for verification");

            for (var customer : customerRepository.findAll()) {
                if (customer.getKycStatus() == KycStatus.PENDING) {
                    try {
                        customerService.verify(customer.getId());
                    } catch (Exception e) {
                        log.warn("Customer #{} failed: {}", customer.getId(), e.getMessage());
                    }
                }
            }
            log.info("✅ Customers verified");

            long ms = System.currentTimeMillis() - t0;

            // Save to AGENT_RUNS + publish event to live feed
            executor.recordRun(
                    "auto-verify-pending",
                    String.format("Verified %d items in %dms",
                            pendingMerchants + pendingCustomers, ms),
                    true,
                    ms);

            log.info("🤖 auto-verify: completed in {}ms", ms);

        } catch (Exception e) {
            long ms = System.currentTimeMillis() - t0;

            executor.recordRun(
                    "auto-verify-pending",
                    "Failed: " + e.getMessage(),
                    false,
                    ms);

            log.error("❌ auto-verify: failed after {}ms", ms, e);
        }
    }
}