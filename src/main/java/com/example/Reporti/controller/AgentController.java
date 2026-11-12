package com.example.Reporti.controller;

import com.example.Reporti.service.AdminUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AdminUserService service;

    public AgentController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping("/available")
    public List<Map<String, Object>> getAvailableAgents(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer sectorId) {
        return service.getAvailableAgents(categoryId, sectorId);
    }
}
