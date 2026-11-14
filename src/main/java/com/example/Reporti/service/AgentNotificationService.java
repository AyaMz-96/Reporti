package com.example.Reporti.service;

import com.example.Reporti.dto.NotificationResponse;
import com.example.Reporti.model.AgentNotification;
import com.example.Reporti.model.NotificationType;
import com.example.Reporti.repository.AgentNotificationRepository;
import com.example.Reporti.repository.ReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AgentNotificationService {

    private final AgentNotificationRepository notificationRepository;
    private final ReportRepository reportRepository;

    public AgentNotificationService(AgentNotificationRepository notificationRepository,
                                    ReportRepository reportRepository) {
        this.notificationRepository = notificationRepository;
        this.reportRepository = reportRepository;
    }

    /** Crée la notification "signalement assigné" pour l'agent en charge du signalement. */
    @Transactional
    public void notifyAssigned(Long reportId) {
        reportRepository.findById(reportId).ifPresent(report -> {
            if (report.getAgentId() == null) return;
            AgentNotification n = new AgentNotification();
            n.setAgentId(report.getAgentId());
            n.setReportId(report.getId());
            n.setType(NotificationType.ASSIGNED);
            n.setMessage("Report #" + report.getId() + " \"" + report.getTitle()
                    + "\" has been assigned to you.");
            notificationRepository.save(n);
        });
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String agentId) {
        return notificationRepository.findByAgentIdOrderByCreatedAtDesc(UUID.fromString(agentId)).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getMessage(), n.getCreatedAt(),
                        n.isRead(), n.getType(), n.getReportId()))
                .toList();
    }

    @Transactional
    public void markAsRead(Long id, String agentId) {
        AgentNotification n = notificationRepository.findByIdAndAgentId(id, UUID.fromString(agentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        n.setRead(true);
    }

    @Transactional
    public void markAllAsRead(String agentId) {
        notificationRepository.markAllAsRead(UUID.fromString(agentId));
    }
}