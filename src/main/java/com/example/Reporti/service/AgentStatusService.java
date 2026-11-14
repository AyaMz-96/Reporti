package com.example.Reporti.service;

import com.example.Reporti.dto.ReportResponse;
import com.example.Reporti.model.NotificationType;
import com.example.Reporti.model.Report;
import com.example.Reporti.model.ReportStatus;
import com.example.Reporti.repository.AgentReportRepository;
import com.example.Reporti.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Set;

@Service
public class AgentStatusService {

    private static final Logger log = LoggerFactory.getLogger(AgentStatusService.class);
    private static final Set<ReportStatus> ALLOWED = EnumSet.of(ReportStatus.IN_PROGRESS, ReportStatus.RESOLVED);

    private final AgentReportRepository reportRepository;
    private final MediaRepository mediaRepository;
    private final NotificationService citizenNotifications;

    public AgentStatusService(AgentReportRepository reportRepository,
                              MediaRepository mediaRepository,
                              NotificationService citizenNotifications) {
        this.reportRepository = reportRepository;
        this.mediaRepository = mediaRepository;
        this.citizenNotifications = citizenNotifications;
    }

    // Volontairement sans @Transactional : si la notification du citoyen échoue,
    // le changement de statut déjà enregistré ne doit pas être annulé.
    public ReportResponse changeStatus(Long reportId, String agentId, ReportStatus newStatus) {
        if (newStatus == null || !ALLOWED.contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status must be IN_PROGRESS or RESOLVED");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));

        if (report.getAgentId() == null || !report.getAgentId().toString().equals(agentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This report is not assigned to you");
        }
        if (report.getStatus() == ReportStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A rejected report cannot be changed");
        }
        if (report.getStatus() == newStatus) {
            return toResponse(report); // rien à changer
        }

        report.setStatus(newStatus);
        Report saved = reportRepository.save(report);

        try {
            String label = newStatus == ReportStatus.RESOLVED ? "Resolved" : "In progress";
            citizenNotifications.create(saved.getCitizenId(), NotificationType.STATUS_CHANGED,
                    "Your report \"" + saved.getTitle() + "\" is now " + label, saved.getId());
        } catch (RuntimeException e) {
            log.warn("Could not notify citizen {} for report {}: {}",
                    saved.getCitizenId(), saved.getId(), e.getMessage());
        }
        return toResponse(saved);
    }

    private ReportResponse toResponse(Report report) {
        return ReportResponse.from(report, mediaRepository.findByReportId(report.getId()));
    }
}