package com.example.Reporti.dto;

import com.example.Reporti.model.Notification;
import com.example.Reporti.model.NotificationType;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id,
        String message,
        OffsetDateTime createdAt,
        boolean isRead,
        NotificationType type,
        Long reportId
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getMessage(),
                n.getCreatedAt(),
                n.isRead(),
                n.getType(),
                n.getReportId()
        );
    }
}