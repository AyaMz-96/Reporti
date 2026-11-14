package com.example.Reporti.repository;

import com.example.Reporti.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByAgentIdAndArchivedFalseOrderByCreatedAtDesc(UUID agentId);
}
