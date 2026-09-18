package com.isg.kyc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.isg.kyc.model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}