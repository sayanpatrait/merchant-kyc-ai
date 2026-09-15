package com.example.kyc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kyc.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}