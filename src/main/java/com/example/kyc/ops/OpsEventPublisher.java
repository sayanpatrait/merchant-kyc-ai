package com.example.kyc.ops;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.Map;

@Component
public class OpsEventPublisher {

    // Replay last 20 events to new subscribers, then stream live
    private final Sinks.Many<Map<String, Object>> sink =
            Sinks.many().replay().limit(20);

    public void publish(String type, String message) {
        sink.tryEmitNext(Map.of(
                "type", type,
                "message", message,
                "timestamp", Instant.now().toString()
        ));
    }

    public Flux<Map<String, Object>> stream() {
        return sink.asFlux();
    }
}