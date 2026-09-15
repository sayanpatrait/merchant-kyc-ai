package com.example.kyc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kyc.model.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}