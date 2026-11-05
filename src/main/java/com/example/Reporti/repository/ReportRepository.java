package com.example.Reporti.repository;

import com.example.Reporti.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByArchivedFalseOrderByCreatedAtDesc();
}