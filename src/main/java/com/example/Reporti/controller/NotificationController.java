package com.example.Reporti.controller;

import com.example.Reporti.dto.NotificationResponse;
import com.example.Reporti.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final String CITIZEN_HEADER = "X-Citizen-Id"; // TEMPORARY until real authentication exists

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestHeader(CITIZEN_HEADER) Long userId) {
        return notificationService.list(userId);
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long id, @RequestHeader(CITIZEN_HEADER) Long userId) {
        notificationService.markAsRead(id, userId);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@RequestHeader(CITIZEN_HEADER) Long userId) {
        notificationService.markAllAsRead(userId);
    }
}