package com.isg.kyc.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.isg.kyc.ai.AiChatService;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class StreamController {

    private final AiChatService aiChatService;

    public StreamController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody ChatController.ChatRequest request) {
        String sessionId = (request.sessionId() == null || request.sessionId().isBlank())
                ? "default" : request.sessionId();
        return aiChatService.stream(sessionId, request.message());
    }
}