package com.example.kyc.api;

import com.example.kyc.ops.OpsEventPublisher;
import com.example.kyc.ops.OpsStats;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/ops")
public class OpsController {

    private final OpsStats stats;
    private final OpsEventPublisher events;

    public OpsController(OpsStats stats, OpsEventPublisher events) {
        this.stats = stats;
        this.events = events;
    }

    /** One-shot snapshot — useful for initial render */
    @GetMapping("/snapshot")
    public Map<String, Object> snapshot() {
        return stats.snapshot();
    }

    /** Live stats stream — pushes every 5s */
    @GetMapping(value = "/stats/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> statsStream() {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(5))
                .onBackpressureDrop()                 // <- key fix
                .map(tick -> ServerSentEvent.<Map<String, Object>>builder()
                        .event("stats")
                        .data(stats.snapshot())
                        .build());
    }

    /** Live activity events */
    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> eventsStream() {
        return events.stream()
                .onBackpressureDrop()                 // <- defensive, in case upstream is a hot publisher
                .map(payload -> ServerSentEvent.<Map<String, Object>>builder()
                        .event("event")
                        .data(payload)
                        .build());
    }
}