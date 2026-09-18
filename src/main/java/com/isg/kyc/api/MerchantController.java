package com.isg.kyc.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.isg.kyc.model.Merchant;
import com.isg.kyc.service.KycService;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {
    private final KycService kycService;

    public MerchantController(KycService kycService) {
        this.kycService = kycService;
    }

    @GetMapping
    public List<Merchant> all() {
        return kycService.all();
    }

    @GetMapping("/{id}")
    public Merchant get(@PathVariable long id) {
        return kycService.get(id);
    }

    @PostMapping
    public Merchant create(@RequestBody Merchant merchant) {
        return kycService.create(merchant);
    }

    @PostMapping("/{id}/verify")
    public Merchant verify(@PathVariable long id) {
        return kycService.verify(id);
    }

    @GetMapping("/summary")
    public String summary() {
        return kycService.summary();
    }

    @PostMapping("/verify-remaining")
    public ResponseEntity<String> verifyRemaining() {
        kycService.verifyRemaining();
        return ResponseEntity.accepted().body("Remaining KYC verification started.");
    }
}
