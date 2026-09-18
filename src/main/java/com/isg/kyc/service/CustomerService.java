package com.isg.kyc.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isg.kyc.kyc.KycProvider;
import com.isg.kyc.kyc.KycVerificationResult;
import com.isg.kyc.model.Customer;
import com.isg.kyc.model.KycStatus;
import com.isg.kyc.ops.OpsEventPublisher;
import com.isg.kyc.repository.CustomerRepository;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final KycProvider kycProvider;
    private final OpsEventPublisher opsEvents;

    public CustomerService(CustomerRepository customerRepository,
                           KycProvider kycProvider,
                           OpsEventPublisher opsEvents) {
        this.customerRepository = customerRepository;
        this.kycProvider = kycProvider;
        this.opsEvents = opsEvents;
    }

    @Transactional(readOnly = true)
    public List<Customer> all() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Customer get(long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    }

    @Transactional
    public Customer create(Customer customer) {
        Customer saved = customerRepository.save(customer);
        opsEvents.publish("create", "New customer created: " + saved.getFullName());
        return saved;
    }

    @Transactional
    public Customer verify(long id) {
        Customer c = get(id);

        if (c.getKycStatus() == KycStatus.PENDING && c.getIdType() != null) {
            KycVerificationResult r = kycProvider.verify(c.getIdType(), c.getIdNumber());
            c.setKycStatus(r.status());

            if (r.status() == KycStatus.FAILED && r.reason() != null) {
                c.setFailureReason(r.reason());
            }

            customerRepository.save(c);
            opsEvents.publish("verify", "Verified customer #" + id + " — " + c.getFullName());
        }
        return c;
    }
}