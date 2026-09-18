package com.isg.kyc.agent;

import org.springframework.stereotype.Service;

import com.isg.kyc.ops.OpsEventPublisher;

import java.time.Instant;

@Service
public class AgentExecutor {

    private final AgentRunTime runtime;
    private final AgentRunRepository repo;
    private final OpsEventPublisher events;

    public AgentExecutor(AgentRunTime runtime,
                         AgentRunRepository repo,
                         OpsEventPublisher events) {
        this.runtime = runtime;
        this.repo = repo;
        this.events = events;
    }

    /** LLM-based execution (chat-triggered) */
    public String execute(String taskName, String prompt) {
        long t0 = System.currentTimeMillis();
        AgentRun run = new AgentRun();
        run.setTaskName(taskName);
        run.setInput(prompt);

        try {
            String out = runtime.run(prompt);
            run.setOutput(out);
            run.setSuccess(true);
            events.publish("agent", "🤖 " + taskName + " completed");
            return out;
        } catch (Exception e) {
            run.setOutput("ERROR: " + e.getMessage());
            run.setSuccess(false);
            events.publish("agent-fail", "🤖 " + taskName + " failed: " + e.getMessage());
            throw e;
        } finally {
            run.setDurationMs(System.currentTimeMillis() - t0);
            repo.save(run);
        }
    }

    /** NEW — Deterministic execution (scheduled, no LLM) */
    public void recordRun(String taskName, String output, boolean success, long durationMs) {
        AgentRun run = new AgentRun();
        run.setTaskName(taskName);
        run.setInput("scheduled sweep");
        run.setOutput(output);
        run.setSuccess(success);
        run.setDurationMs(durationMs);
        run.setStartedAt(Instant.now());
        repo.save(run);

        events.publish(success ? "agent" : "agent-fail", output);
    }
}