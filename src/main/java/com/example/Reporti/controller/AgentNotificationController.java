package com.example.Reporti.controller;

import com.example.Reporti.dto.NotificationResponse;
import com.example.Reporti.service.AdminUserService;
import com.example.Reporti.service.AgentNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents/me/notifications")
public class AgentNotificationController {

    private final AdminUserService userService;
    private final AgentNotificationService notificationService;

    public AgentNotificationController(AdminUserService userService,
                                       AgentNotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestHeader("Authorization") String auth) {
        return notificationService.list(userService.resolveAgentFromToken(auth).id());
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        notificationService.markAsRead(id, userService.resolveAgentFromToken(auth).id());
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@RequestHeader("Authorization") String auth) {
        notificationService.markAllAsRead(userService.resolveAgentFromToken(auth).id());
    }
}
