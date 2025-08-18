package com.example.bnk_project_02s.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bnk_project_02s.entity.ShoppingLog;

@Repository
public interface ShoppingLogRepository extends JpaRepository<ShoppingLog, Long> {
	
}