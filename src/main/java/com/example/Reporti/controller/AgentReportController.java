package com.example.Reporti.controller;

import com.example.Reporti.dto.ReportResponse;
import com.example.Reporti.model.ReportStatus;
import com.example.Reporti.service.AdminUserService;
import com.example.Reporti.service.AgentReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agents/me/assigned")
public class AgentReportController {

    private final AdminUserService userService;
    private final AgentReportService reportService;

    public AgentReportController(AdminUserService userService, AgentReportService reportService) {
        this.userService = userService;
        this.reportService = reportService;
    }

    /** GET /api/agents/me/assigned  ou  /api/agents/me/assigned?status=IN_PROGRESS */
    @GetMapping
    public List<ReportResponse> myAssigned(
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false) ReportStatus status) {
        String agentId = userService.resolveAgentFromToken(auth).id();
        return reportService.listAssigned(agentId, status);
    }
}