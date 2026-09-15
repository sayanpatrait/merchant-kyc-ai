package com.example.kyc.agent;

import com.example.kyc.ai.CustomerTools;
import com.example.kyc.ai.MerchantTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AgentRunTime {

    private final ChatClient agentClient;

    public AgentRunTime(ChatClient.Builder builder,
                        MerchantTools merchantTools,
                        CustomerTools customerTools) {
        this.agentClient = builder
            .defaultSystem("""
                You are an autonomous KYC operations agent.
                You process tasks in the background without user input.
                You have the same tools as the interactive assistant.
                Rules:
                - Never invent IDs.
                - Never perform destructive actions.
                - Only verify records that are in PENDING status.
                - Report concisely what you did (IDs, counts, failures).
                """)
            .defaultTools(merchantTools, customerTools)
            .build();
    }

    public String run(String task) {
        return agentClient.prompt()
            .user(task)
            .call()
            .content();
    }
}