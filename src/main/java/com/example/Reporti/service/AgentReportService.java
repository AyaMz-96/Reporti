package com.example.Reporti.service;

import com.example.Reporti.dto.ReportResponse;
import com.example.Reporti.model.ReportStatus;
import com.example.Reporti.repository.AgentReportRepository;
import com.example.Reporti.repository.MediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AgentReportService {

    private final AgentReportRepository agentReportRepository;
    private final MediaRepository mediaRepository;

    public AgentReportService(AgentReportRepository agentReportRepository,
                              MediaRepository mediaRepository) {
        this.agentReportRepository = agentReportRepository;
        this.mediaRepository = mediaRepository;
    }

    /** Signalements assignés à l'agent, du plus récent au plus ancien, avec filtre optionnel sur le statut. */
    @Transactional(readOnly = true)
    public List<ReportResponse> listAssigned(String agentId, ReportStatus status) {
        return agentReportRepository
                .findByAgentIdAndArchivedFalseOrderByCreatedAtDesc(UUID.fromString(agentId))
                .stream()
                .filter(r -> status == null || r.getStatus() == status)
                .map(r -> ReportResponse.from(r, mediaRepository.findByReportId(r.getId())))
                .toList();
    }
}
