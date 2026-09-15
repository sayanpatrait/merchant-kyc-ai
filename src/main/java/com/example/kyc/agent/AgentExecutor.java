package com.example.kyc.agent;

import com.example.kyc.ops.OpsEventPublisher;

import org.springframework.stereotype.Service;

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
}