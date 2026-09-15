package com.example.kyc.agent;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentExecutor executor;
    private final AgentRunRepository repo;

    public AgentController(AgentExecutor executor, AgentRunRepository repo) {
        this.executor = executor;
        this.repo = repo;
    }

    @PostMapping("/run/{task}")
    public String run(@PathVariable String task) {
        return switch (task) {
            case "auto-verify" -> executor.execute(
                "auto-verify",
                "Verify all remaining pending KYC for merchants and customers. Report counts.");
            default -> "Unknown task: " + task;
        };
    }

    @GetMapping("/runs")
    public List<AgentRun> recentRuns() {
        return repo.findTop20ByOrderByStartedAtDesc();
    }
}
