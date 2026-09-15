package com.example.kyc.service;

import com.example.kyc.kyc.KycProvider;
import com.example.kyc.kyc.KycVerificationResult;
import com.example.kyc.model.KycStatus;
import com.example.kyc.model.KycType;
import com.example.kyc.model.Merchant;
import com.example.kyc.ops.OpsEventPublisher;
import com.example.kyc.repository.MerchantRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class KycService {
    private final MerchantRepository merchantRepository;
    private final KycProvider kycProvider;
    private final OpsEventPublisher opsEvents;

    @Transactional(readOnly = true)
    public List<Merchant> all() {
        return merchantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Merchant get(long id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + id));
    }

    @Transactional(readOnly = true)
    public String summary() {
        long total = merchantRepository.count();
        long verified = merchantRepository.countByPanStatus(KycStatus.VERIFIED)
                + merchantRepository.countByAadhaarStatus(KycStatus.VERIFIED)
                + merchantRepository.countByGstStatus(KycStatus.VERIFIED);
        long pending = merchantRepository.countByPanStatus(KycStatus.PENDING)
                + merchantRepository.countByAadhaarStatus(KycStatus.PENDING)
                + merchantRepository.countByGstStatus(KycStatus.PENDING);
        long failed = merchantRepository.countByPanStatus(KycStatus.FAILED)
                + merchantRepository.countByAadhaarStatus(KycStatus.FAILED)
                + merchantRepository.countByGstStatus(KycStatus.FAILED);

        return "Merchants: " + total +
                ". KYC checks — verified: " + verified +
                ", pending: " + pending +
                ", failed: " + failed + ".";
    }

    @Async
    public CompletableFuture<Void> verifyRemaining() {
        merchantRepository.findByPanStatusOrAadhaarStatusOrGstStatus(
                        KycStatus.PENDING, KycStatus.PENDING, KycStatus.PENDING)
                .forEach(this::verifyMerchantRemaining);
        return CompletableFuture.completedFuture(null);
    }


    private void verifyMerchantRemaining(Merchant merchant) {
        verifyIfPending(merchant, KycType.PAN);
        verifyIfPending(merchant, KycType.AADHAAR);
        verifyIfPending(merchant, KycType.GST);
        merchantRepository.save(merchant);
    }

    private void verifyIfPending(Merchant merchant, KycType type) {
        String identifier = switch (type) {
            case PAN -> merchant.getPan();
            case AADHAAR -> merchant.getAadhaar();
            case GST -> merchant.getGstin();
        };

        KycStatus current = switch (type) {
            case PAN -> merchant.getPanStatus();
            case AADHAAR -> merchant.getAadhaarStatus();
            case GST -> merchant.getGstStatus();
        };

        if (current != KycStatus.PENDING) return;

        KycVerificationResult result = kycProvider.verify(type, identifier);

        switch (type) {
            case PAN -> merchant.setPanStatus(result.status());
            case AADHAAR -> merchant.setAadhaarStatus(result.status());
            case GST -> merchant.setGstStatus(result.status());
        }
    }

    public KycService(MerchantRepository merchantRepository,
                      KycProvider kycProvider,
                      OpsEventPublisher opsEvents) {
        this.merchantRepository = merchantRepository;
        this.kycProvider = kycProvider;
        this.opsEvents = opsEvents;
    }
    
    @Transactional
    public Merchant verify(long id) {
        Merchant merchant = get(id);
        verifyMerchantRemaining(merchant);
        Merchant saved = merchantRepository.save(merchant);
        opsEvents.publish("verify", "Verified merchant #" + id + " — " + saved.getMerchantName());
        return saved;
    }

    @Transactional
    public Merchant create(Merchant merchant) {
        Merchant saved = merchantRepository.save(merchant);
        opsEvents.publish("create", "New merchant created: " + saved.getMerchantName());
        return saved;
    }
}
