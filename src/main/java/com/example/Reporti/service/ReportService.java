package com.example.Reporti.service;

import com.example.Reporti.dto.CreateReportRequest;
import com.example.Reporti.dto.ReportResponse;
import com.example.Reporti.model.Media;
import com.example.Reporti.model.MediaKind;
import com.example.Reporti.model.Report;
import com.example.Reporti.model.ReportStatus;
import com.example.Reporti.repository.CategoryRepository;
import com.example.Reporti.repository.MediaRepository;
import com.example.Reporti.repository.ReportRepository;
import com.example.Reporti.repository.SectorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final MediaRepository mediaRepository;
    private final CategoryRepository categoryRepository;
    private final SectorRepository sectorRepository;
    private final SupabaseStorageService storage;

    public ReportService(ReportRepository reportRepository,
                         MediaRepository mediaRepository,
                         CategoryRepository categoryRepository,
                         SectorRepository sectorRepository,
                         SupabaseStorageService storage) {
        this.reportRepository = reportRepository;
        this.mediaRepository = mediaRepository;
        this.categoryRepository = categoryRepository;
        this.sectorRepository = sectorRepository;
        this.storage = storage;
    }

    @Transactional
    public ReportResponse create(CreateReportRequest request, Long citizenId) {
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category");
        }
        if (!sectorRepository.existsById(request.sectorId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown sector");
        }
        Report report = new Report();
        report.setTitle(request.title().trim());
        report.setDescription(request.description().trim());
        report.setLatitude(request.latitude());
        report.setLongitude(request.longitude());
        report.setCategoryId(request.categoryId());
        report.setSectorId(request.sectorId());
        report.setCitizenId(citizenId);
        // Entity defaults to NORMAL; only override when the client sent a value
        if (request.priority() != null) {
            report.setPriority(request.priority());
        }
        Report saved = reportRepository.save(report);
        return ReportResponse.from(saved, List.of());
    }

    @Transactional
    public ReportResponse assignReport(Long reportId, String agentId) {
        Report report = findReport(reportId);
        UUID uuid;
        try {
            uuid = UUID.fromString(agentId.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "agentId must be a valid UUID (got: " + agentId + ")");
        }
        report.setAgentId(uuid);
        report.setStatus(ReportStatus.IN_PROGRESS);
        return ReportResponse.from(reportRepository.save(report), mediaRepository.findByReportId(reportId));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listClaimableForAgent(List<Integer> categoryIds, Integer sectorId) {
        if ((categoryIds == null || categoryIds.isEmpty()) && sectorId == null) {
            return List.of();
        }
        return reportRepository.findByArchivedFalseOrderByCreatedAtDesc().stream()
            .filter(r -> r.getStatus() == ReportStatus.RECEIVED && r.getAgentId() == null)
            .filter(r -> categoryIds == null || categoryIds.isEmpty()
                    || categoryIds.contains(r.getCategoryId().intValue()))
            .filter(r -> sectorId == null || r.getSectorId().equals(sectorId.longValue()))
            .map(r -> ReportResponse.from(r, mediaRepository.findByReportId(r.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listActiveForAgent(String agentId) {
        return reportRepository.findByArchivedFalseOrderByCreatedAtDesc().stream()
            .filter(r -> r.getAgentId() != null && r.getAgentId().toString().equals(agentId))
            .map(r -> ReportResponse.from(r, mediaRepository.findByReportId(r.getId())))
            .toList();
    }

    @Transactional
    public ReportResponse claimReport(Long reportId, String agentId, List<Integer> categoryIds, Integer sectorId) {
        Report report = findReport(reportId);
        if (report.getStatus() != ReportStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This report is no longer available");
        }
        if (report.getAgentId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already assigned to another agent");
        }
        if ((categoryIds == null || categoryIds.isEmpty()) && sectorId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Configure at least one category or sector in your profile before taking assignments");
        }
        if (categoryIds != null && !categoryIds.isEmpty()
                && !categoryIds.contains(report.getCategoryId().intValue())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "This report's category is not in your profile");
        }
        if (sectorId != null && !report.getSectorId().equals(sectorId.longValue())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "This report is outside your sector");
        }
        report.setAgentId(UUID.fromString(agentId));
        report.setStatus(ReportStatus.IN_PROGRESS);
        return ReportResponse.from(reportRepository.save(report), mediaRepository.findByReportId(reportId));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> list() {
        return reportRepository.findByArchivedFalseOrderByCreatedAtDesc().stream()
                .map(report -> ReportResponse.from(report, mediaRepository.findByReportId(report.getId())))
                .toList();
    }

    public ReportResponse get(Long reportId) {
        Report report = findReport(reportId);
        return ReportResponse.from(report, mediaRepository.findByReportId(reportId));
    }

    public List<String> addMedia(Long reportId, Long citizenId, List<MultipartFile> files) {
        Report report = findReport(reportId);
        if (!report.getCitizenId().equals(citizenId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This report belongs to another citizen");
        }
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String contentType = file.getContentType() == null ? "" : file.getContentType();
            MediaKind kind;
            if (contentType.startsWith("image/")) kind = MediaKind.PHOTO;
            else if (contentType.startsWith("video/")) kind = MediaKind.VIDEO;
            else throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only photos and videos are accepted");

            byte[] bytes;
            try { bytes = file.getBytes(); } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
            }
            String path = "reports/" + reportId + "/" + UUID.randomUUID() + "." + extensionOf(file.getOriginalFilename());
            String url = storage.upload(path, bytes, contentType);
            Media media = new Media();
            media.setReportId(reportId);
            media.setType(kind);
            media.setUrl(url);
            mediaRepository.save(media);
            urls.add(url);
        }
        return urls;
    }

    private Report findReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
    }

    private String extensionOf(String filename) {
        if (filename == null) return "bin";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "bin";
        String ext = filename.substring(dot + 1).toLowerCase();
        return ext.matches("[a-z0-9]{1,5}") ? ext : "bin";
    }
}
