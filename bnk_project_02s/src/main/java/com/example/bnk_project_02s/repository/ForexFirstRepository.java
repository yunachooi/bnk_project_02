package com.example.bnk_project_02s.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bnk_project_02s.entity.Rate;

public interface ForexFirstRepository extends JpaRepository<Rate, Long> {
	
	// 1. 특정 날짜 기준 환율 전체 조회 (예: 오늘 날짜 기준)
    List<Rate> findByRdate(LocalDate rdate);

}
