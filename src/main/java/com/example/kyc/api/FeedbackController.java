package com.example.kyc.api;

import com.example.kyc.model.Feedback;
import com.example.kyc.repository.FeedbackRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackRepository repo;

    public FeedbackController(FeedbackRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public Feedback submit(@RequestBody FeedbackRequest req) {
        Feedback f = new Feedback();
        f.setSessionId(req.sessionId());
        f.setUserMessage(req.userMessage());
        f.setAiResponse(req.aiResponse());
        f.setRating(req.rating());
        return repo.save(f);
    }

    @GetMapping
    public List<Feedback> all() { return repo.findAll(); }

    public record FeedbackRequest(String sessionId, String userMessage,
                                  String aiResponse, int rating) {}
}