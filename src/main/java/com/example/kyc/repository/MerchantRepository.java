package com.example.kyc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kyc.model.KycStatus;
import com.example.kyc.model.Merchant;

import java.util.List;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    long countByPanStatus(KycStatus status);
    long countByAadhaarStatus(KycStatus status);
    long countByGstStatus(KycStatus status);
    List<Merchant> findByPanStatusOrAadhaarStatusOrGstStatus(
            KycStatus panStatus, KycStatus aadhaarStatus, KycStatus gstStatus);
}
