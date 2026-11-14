package com.example.Reporti.controller;

import com.example.Reporti.service.AdminUserService;
import com.example.Reporti.service.AgentAvailabilityService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/agents/me/availability")
public class AgentAvailabilityController {

    public record AvailabilityRequest(Boolean available) {}

    public record AvailabilityResponse(boolean available) {}

    private final AdminUserService userService;
    private final AgentAvailabilityService availabilityService;

    public AgentAvailabilityController(AdminUserService userService,
                                       AgentAvailabilityService availabilityService) {
        this.userService = userService;
        this.availabilityService = availabilityService;
    }

    /** GET /api/agents/me/availability */
    @GetMapping
    public AvailabilityResponse get(@RequestHeader("Authorization") String auth) {
        return new AvailabilityResponse(userService.resolveAgentFromToken(auth).available());
    }

    /** PATCH /api/agents/me/availability   body: {"available": false} */
    @PatchMapping
    public AvailabilityResponse set(@RequestBody AvailabilityRequest request,
                                    @RequestHeader("Authorization") String auth) {
        userService.resolveAgentFromToken(auth); // vérifie le token et le rôle agent
        if (request.available() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "\"available\" is required");
        }
        return new AvailabilityResponse(availabilityService.setAvailability(auth, request.available()));
    }
}