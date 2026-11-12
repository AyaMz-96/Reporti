package com.example.Reporti.controller;

import com.example.Reporti.dto.ReportResponse;
import com.example.Reporti.service.AdminUserService;
import com.example.Reporti.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AdminUserService userService;
    private final ReportService reportService;

    public AgentController(AdminUserService userService, ReportService reportService) {
        this.userService = userService;
        this.reportService = reportService;
    }

    /** Admin flow: list all agents matching a given report's category + sector. */
    @GetMapping("/available")
    public List<Map<String, Object>> getAvailableAgents(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer sectorId) {
        return userService.getAvailableAgents(categoryId, sectorId);
    }

    /** Agent flow: reports I can take up right now, based on my own profile. */
    @GetMapping("/me/available")
    public List<ReportResponse> myAvailable(@RequestHeader("Authorization") String auth) {
        AdminUserService.AgentInfo me = userService.resolveAgentFromToken(auth);
        return reportService.listClaimableForAgent(me.categoryIds(), me.sectorId());
    }

    /** Agent flow: reports I am currently working on. */
    @GetMapping("/me/active")
    public List<ReportResponse> myActive(@RequestHeader("Authorization") String auth) {
        AdminUserService.AgentInfo me = userService.resolveAgentFromToken(auth);
        return reportService.listActiveForAgent(me.id());
    }

    /** Agent flow: claim a report for myself. */
    @PostMapping("/me/claim/{reportId}")
    public ReportResponse claim(
            @PathVariable Long reportId,
            @RequestHeader("Authorization") String auth) {
        AdminUserService.AgentInfo me = userService.resolveAgentFromToken(auth);
        return reportService.claimReport(reportId, me.id(), me.categoryIds(), me.sectorId());
    }
}