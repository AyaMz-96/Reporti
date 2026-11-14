package com.example.Reporti.repository;

import com.example.Reporti.model.ReportComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {

    List<ReportComment> findByReportIdOrderByCreatedAtAsc(Long reportId);
}