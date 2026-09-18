package com.isg.kyc.kafka;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.isg.kyc.model.KycStatus;
import com.isg.kyc.repository.CustomerRepository;
import com.isg.kyc.repository.MerchantRepository;
import com.isg.kyc.service.CustomerService;
import com.isg.kyc.service.KycService;

@Component
public class UploadConsumer {

    private static final Logger log = LoggerFactory.getLogger(UploadConsumer.class);

    private final KycService kycService;
    private final CustomerService customerService;
    private final MerchantRepository merchantRepository;
    private final CustomerRepository customerRepository;

    public UploadConsumer(KycService kycService,
                          CustomerService customerService,
                          MerchantRepository merchantRepository,
                          CustomerRepository customerRepository) {
        this.kycService = kycService;
        this.customerService = customerService;
        this.merchantRepository = merchantRepository;
        this.customerRepository = customerRepository;
    }

    @KafkaListener(
            topics = "kyc.merchant.upload",
            groupId = "kyc-merchant-verifier",
            concurrency = "3",
            properties = {
                    "spring.json.value.default.type=com.isg.kyc.kafka.MerchantUploadEvent"
            }
    )
    public void onMerchantUpload(MerchantUploadEvent event) {
        log.info("📥 Received merchant upload event: #{}", event.merchantId());

        boolean stillPending = merchantRepository.findById(event.merchantId())
                .map(m -> m.getPanStatus() == KycStatus.PENDING
                        || m.getAadhaarStatus() == KycStatus.PENDING
                        || m.getGstStatus() == KycStatus.PENDING)
                .orElse(false);

        if (!stillPending) {
            log.info("⏭️ Skipping merchant #{} — not pending or not found", event.merchantId());
            return;
        }

        try {
            kycService.verify(event.merchantId());
            log.info("✅ Merchant #{} verified", event.merchantId());
        } catch (Exception e) {
            log.error("❌ Verification failed for merchant #{}", event.merchantId(), e);
            throw e;
        }
    }

    @KafkaListener(
            topics = "kyc.customer.upload",
            groupId = "kyc-customer-verifier",
            concurrency = "6",
            properties = {
                    "spring.json.value.default.type=com.isg.kyc.kafka.CustomerUploadEvent"
            }
    )
    public void onCustomerUpload(CustomerUploadEvent event) {
        log.info("📥 Received customer upload event: #{}", event.customerId());

        boolean stillPending = customerRepository.findById(event.customerId())
                .map(c -> c.getKycStatus() == KycStatus.PENDING)
                .orElse(false);

        if (!stillPending) {
            log.info("⏭️ Skipping customer #{} — not pending or not found", event.customerId());
            return;
        }

        try {
            customerService.verify(event.customerId());
            log.info("✅ Customer #{} verified", event.customerId());
        } catch (Exception e) {
            log.error("❌ Verification failed for customer #{}", event.customerId(), e);
            throw e;
        }
    }
}