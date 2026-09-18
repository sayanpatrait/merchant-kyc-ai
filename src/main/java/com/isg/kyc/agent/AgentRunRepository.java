package com.isg.kyc.agent;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {
    List<AgentRun> findTop20ByOrderByStartedAtDesc();
}