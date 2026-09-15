package com.example.kyc.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    private String email;
    private String phone;

    @Enumerated(EnumType.STRING)
    private KycType idType;                 // PAN, AADHAAR, GST (reused)

    private String idNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KycStatus kycStatus = KycStatus.PENDING;

    private String city;
    private String state;
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    // getters + setters
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { fullName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public KycType getIdType() { return idType; }
    public void setIdType(KycType v) { idType = v; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String v) { idNumber = v; }
    public KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(KycStatus v) { kycStatus = v; }
    public String getCity() { return city; }
    public void setCity(String v) { city = v; }
    public String getState() { return state; }
    public void setState(String v) { state = v; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate v) { dateOfBirth = v; }
    public Instant getCreatedAt() { return createdAt; }
}