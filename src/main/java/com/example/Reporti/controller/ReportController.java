package com.example.Reporti.controller;

import com.example.Reporti.dto.CreateReportRequest;
import com.example.Reporti.dto.ReportResponse;
import com.example.Reporti.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final String CITIZEN_HEADER = "X-Citizen-Id"; // TEMPORARY until real authentication exists

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ReportResponse> create(@Valid @RequestBody CreateReportRequest request,
                                                 @RequestHeader(CITIZEN_HEADER) Long citizenId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.create(request, citizenId));
    }

    @PostMapping(value = "/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<String> uploadMedia(@PathVariable Long id,
                                    @RequestHeader(CITIZEN_HEADER) Long citizenId,
                                    @RequestParam("files") List<MultipartFile> files) {
        return reportService.addMedia(id, citizenId, files);
    }
        @GetMapping
    public List<ReportResponse> list() {
        return reportService.list();
    }

    @GetMapping("/{id}")
    public ReportResponse get(@PathVariable Long id) {
        return reportService.get(id);                                                               
    }
}