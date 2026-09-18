package com.isg.kyc.api;

import com.isg.kyc.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/heatmap")
    public Map<String, Object> heatmap() {
        return analyticsService.hourlyHeatmap();
    }

    @GetMapping("/failure-reasons")
    public Map<String, Object> failureReasons() {
        return analyticsService.failureReasons();
    }

    @GetMapping("/success-rate")
    public Map<String, Object> successRate() {
        return analyticsService.successRate();
    }
}