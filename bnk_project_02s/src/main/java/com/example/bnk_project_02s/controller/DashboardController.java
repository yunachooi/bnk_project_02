package com.example.bnk_project_02s.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.repository.UserRepository;

@RestController
@RequestMapping("/api")
public class DashboardController {
	
	@Autowired
	UserRepository userRepo;
	
	@GetMapping("/ageStats")
	public ResponseEntity<Map<String, Integer>> getAgeStats() {
	    List<Object[]> results = userRepo.countByAgeGroup();

	    Map<String, Integer> ageStats = new LinkedHashMap<>();
	    // 연령대 순서 보장
	    ageStats.put("10대", 0);
	    ageStats.put("20대", 0);
	    ageStats.put("30대", 0);
	    ageStats.put("40대", 0);
	    ageStats.put("50대", 0);
	    ageStats.put("60대+", 0);

	    for (Object[] row : results) {
	        String group = (String) row[0];
	        Long count = (Long) row[1];
	        ageStats.put(group, count.intValue());
	    }

	    return ResponseEntity.ok(ageStats);
	}
}