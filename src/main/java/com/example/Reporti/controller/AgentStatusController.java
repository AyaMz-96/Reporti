package com.example.Reporti.controller;

import com.example.Reporti.dto.ReportResponse;
import com.example.Reporti.model.ReportStatus;
import com.example.Reporti.service.AdminUserService;
import com.example.Reporti.service.AgentStatusService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents/me/reports")
public class AgentStatusController {

    public record StatusRequest(ReportStatus status) {}

    private final AdminUserService userService;
    private final AgentStatusService statusService;

    public AgentStatusController(AdminUserService userService, AgentStatusService statusService) {
        this.userService = userService;
        this.statusService = statusService;
    }

    /** PATCH /api/agents/me/reports/{reportId}/status   body: {"status":"RESOLVED"} */
    @PatchMapping("/{reportId}/status")
    public ReportResponse changeStatus(@PathVariable Long reportId,
                                       @RequestBody StatusRequest request,
                                       @RequestHeader("Authorization") String auth) {
        String agentId = userService.resolveAgentFromToken(auth).id();
        return statusService.changeStatus(reportId, agentId, request.status());
    }
}