package com.isg.kyc.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {
	private final ChatClient chatClient;

	public AiChatService(ChatClient.Builder builder, MerchantTools merchantTools, CustomerTools customerTools,
			ReportTools reportTools) {
		this.chatClient = builder.defaultSystem("""
				You are a KYC operations assistant for a bank.

				RESOLVE FIRST — always:
				- Merchant by name/PAN/GSTIN/email → findMerchants first.
				- Customer by name/email/ID/city/state → findCustomers first.

				THEN ACT:
				- verifyMerchant / verifyCustomer
				- verifyAllRemainingMerchants / verifyAllRemainingCustomers
				- getMerchantDetails / getCustomerDetails
				- getMerchantSummary / getCustomerSummary

				REPORTS:
				- For "how many failed/pending" → failedPendingSummary()
				- For "give me the report" or "Excel file" → generateFailedPendingReport()

				RULES:
				- Never claim a verification succeeded unless the tool result says so.
				- If multiple matches, ask which one.
				- Never invent IDs.
				- Keep replies concise.
				""")
				.defaultTools(merchantTools, customerTools, reportTools).defaultAdvisors(
				MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(20).build()).build())
				.build();
	}

	/** Blocking call. Returns content + tool names invoked. */
	public ChatReply chat(String conversationId, String message) {
		ChatResponse response = chatClient.prompt().user(message)
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId)).call().chatResponse();

		String content = response.getResult() != null ? response.getResult().getOutput().getText() : "";

		List<String> tools = extractToolNames(response);
		return new ChatReply(content, tools);
	}

	/** Streaming call. Emits raw text chunks (SSE-friendly). */
	public Flux<String> stream(String conversationId, String message) {
		return chatClient.prompt().user(message).advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
				.stream().content();
	}

	@SuppressWarnings("unchecked")
	private List<String> extractToolNames(ChatResponse response) {
		List<String> names = new ArrayList<>();
		try {
			Map<String, Object> metadata = response.getMetadata() != null ? response.getMetadata().entrySet().stream()
					.collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)) : Map.of();

			Object calls = metadata.get("toolCalls");
			if (calls instanceof List<?> list) {
				for (Object o : list) {
					if (o instanceof Map<?, ?> m && m.get("name") != null) {
						names.add(m.get("name").toString());
					}
				}
			}
		} catch (Exception ignored) {
			/* metadata shape varies by Spring AI version */ }
		return names;
	}
}