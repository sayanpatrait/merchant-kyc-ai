package com.isg.kyc.api;

import org.springframework.web.bind.annotation.*;

import com.isg.kyc.ai.AiChatService;
import com.isg.kyc.ai.ChatReply;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AiChatService aiChatService;

    public ChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String sessionId = (request.sessionId() == null || request.sessionId().isBlank())
                ? "default" : request.sessionId();
        ChatReply reply = aiChatService.chat(sessionId, request.message());
        return new ChatResponse(reply.content(), reply.toolsUsed());
    }

    public record ChatRequest(String sessionId, String message) {}
    public record ChatResponse(String response, List<String> toolsUsed) {}
}