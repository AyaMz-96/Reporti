package com.example.Reporti.service;

import com.example.Reporti.model.Report;
import com.example.Reporti.model.ReportComment;
import com.example.Reporti.repository.AgentReportRepository;
import com.example.Reporti.repository.ReportCommentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class AgentCommentService {

    private static final int MAX_LENGTH = 2000;

    private final ReportCommentRepository commentRepository;
    private final AgentReportRepository reportRepository;

    public AgentCommentService(ReportCommentRepository commentRepository,
                               AgentReportRepository reportRepository) {
        this.commentRepository = commentRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public List<ReportComment> list(Long reportId, String agentId) {
        checkOwnership(reportId, agentId);
        return commentRepository.findByReportIdOrderByCreatedAtAsc(reportId);
    }

    @Transactional
    public ReportComment add(Long reportId, String agentId, String authorName, String content) {
        checkOwnership(reportId, agentId);

        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment cannot be empty");
        }
        if (text.length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Comment is too long (max " + MAX_LENGTH + " characters)");
        }

        ReportComment comment = new ReportComment();
        comment.setReportId(reportId);
        comment.setAuthorId(UUID.fromString(agentId));
        comment.setAuthorName(authorName);
        comment.setContent(text);
        return commentRepository.save(comment);
    }

    private void checkOwnership(Long reportId, String agentId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        if (report.getAgentId() == null || !report.getAgentId().toString().equals(agentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This report is not assigned to you");
        }
    }
}