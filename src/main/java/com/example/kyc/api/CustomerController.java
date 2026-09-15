package com.example.kyc.api;

import com.example.kyc.model.Customer;
import com.example.kyc.service.CustomerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<Customer> all() {
        return customerService.all();
    }

    @GetMapping("/{id}")
    public Customer get(@PathVariable long id) {
        return customerService.get(id);
    }

    @PostMapping
    public Customer create(@RequestBody Customer customer) {
        return customerService.create(customer);
    }

    @PostMapping("/{id}/verify")
    public Customer verify(@PathVariable long id) {
        return customerService.verify(id);
    }
}