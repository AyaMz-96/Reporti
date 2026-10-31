package com.example.Reporti.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.Reporti.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.Reporti.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
}