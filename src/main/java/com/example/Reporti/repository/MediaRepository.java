package com.example.Reporti.repository;

import com.example.Reporti.model.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {

    List<Media> findByReportId(Long reportId);
}