package com.example.Reporti.dto;

import com.example.Reporti.model.Media;
import com.example.Reporti.model.PriorityLevel;
import com.example.Reporti.model.Report;
import com.example.Reporti.model.ReportStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record ReportResponse(
        Long id,
        String title,
        String description,
        ReportStatus status,
        PriorityLevel priority,
        double latitude,
        double longitude,
        Long categoryId,
        Long sectorId,
        OffsetDateTime createdAt,
        List<String> mediaUrls
) {
    public static ReportResponse from(Report r, List<Media> media) {
        return new ReportResponse(
                r.getId(),
                r.getTitle(),
                r.getDescription(),
                r.getStatus(),
                r.getPriority(),
                r.getLatitude(),
                r.getLongitude(),
                r.getCategoryId(),
                r.getSectorId(),
                r.getCreatedAt(),
                media.stream().map(Media::getUrl).toList()
        );
    }
}