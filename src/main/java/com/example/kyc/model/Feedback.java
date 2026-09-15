package com.example.kyc.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(length = 2000)
    private String userMessage;

    @Column(length = 4000)
    private String aiResponse;

    @Column(nullable = false)
    private int rating;      // +1 thumbs up, -1 thumbs down

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { sessionId = v; }
    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String v) { userMessage = v; }
    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String v) { aiResponse = v; }
    public int getRating() { return rating; }
    public void setRating(int v) { rating = v; }
    public Instant getCreatedAt() { return createdAt; }
}