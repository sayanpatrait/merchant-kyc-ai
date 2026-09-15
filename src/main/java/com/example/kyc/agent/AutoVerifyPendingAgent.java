package com.example.kyc.agent;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoVerifyPendingAgent {

    private final AgentExecutor executor;

    public AutoVerifyPendingAgent(AgentExecutor executor) {
        this.executor = executor;
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void run() {
        executor.execute(
            "auto-verify",
            """
            Task: Auto-verify pending KYC.
            1. Call getMerchantSummary to see if any merchants have pending failed checks.
            2. If there are pending items, call findMerchants with an empty-ish query
               or iterate — actually, DO NOT invent IDs. Instead:
               Call verifyAllRemainingMerchants once.
            3. Then call verifyAllRemainingCustomers once.
            4. Report exactly what was verified (counts).
            If nothing was pending, reply "Nothing to verify."
            """
        );
    }
}