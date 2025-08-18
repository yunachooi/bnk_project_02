package com.example.bnk_project_02s.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.repository.UserRepository;
import com.example.bnk_project_02s.service.RecommendationPushService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/push")
@RequiredArgsConstructor
public class PushAdminController {
	
	private final RecommendationPushService svc;
	private final UserRepository userRepository;
	
	@GetMapping("/recommend/count")
	public Map<String, Long> countTargets(){
		return Map.of("count", userRepository.countRecommendationTargets());
	}
	
	@PostMapping("/recommend")
	public RecommendationPushService.Summary enqueue( @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "500") int size){
		return svc.enqueeRecommendations(page, size);
	}
}
