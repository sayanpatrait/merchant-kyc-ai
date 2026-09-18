package com.isg.kyc.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.isg.kyc.model.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}