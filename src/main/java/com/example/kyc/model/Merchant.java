package com.example.kyc.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "merchants")
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String merchantName;

    private String email;
    private String phone;
    private String pan;
    private String aadhaar;
    private String gstin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus panStatus = KycStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus aadhaarStatus = KycStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus gstStatus = KycStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String v) { merchantName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getPan() { return pan; }
    public void setPan(String v) { pan = v; }
    public String getAadhaar() { return aadhaar; }
    public void setAadhaar(String v) { aadhaar = v; }
    public String getGstin() { return gstin; }
    public void setGstin(String v) { gstin = v; }
    public KycStatus getPanStatus() { return panStatus; }
    public void setPanStatus(KycStatus v) { panStatus = v; }
    public KycStatus getAadhaarStatus() { return aadhaarStatus; }
    public void setAadhaarStatus(KycStatus v) { aadhaarStatus = v; }
    public KycStatus getGstStatus() { return gstStatus; }
    public void setGstStatus(KycStatus v) { gstStatus = v; }
    public Instant getCreatedAt() { return createdAt; }
}
