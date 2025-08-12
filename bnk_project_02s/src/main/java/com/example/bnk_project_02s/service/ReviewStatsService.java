package com.example.bnk_project_02s.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.bnk_project_02s.dto.MonthlyStatsDto;
import com.example.bnk_project_02s.dto.ReviewRowDto;
import com.example.bnk_project_02s.entity.Review;
import com.example.bnk_project_02s.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewStatsService {

    private final ReviewRepository reviewRepo;

    public List<ReviewRowDto> recent(int limit){
        return reviewRepo.findRecent(limit).stream()
          .map(r -> new ReviewRowDto(
              r.getRvdate(),
              maskUid(r.getUid()),
              parseRating(r.getRvrating()),
              r.getRvcontent()))
          .toList();
      }
    
    public MonthlyStatsDto Monthly(int months) {
    	//기간 계산 (오늘 포함 과거 n-1개월)
    	LocalDate end = LocalDate.now();
    	LocalDate start = end.minusMonths(months-1).withDayOfMonth(1);
    	List<Review> rows = reviewRepo.findByDateRange(start.toString(), end.toString());
    	
    	//월 키(yyyy-mm)
    	Map<String, List<Double>> bucket = new LinkedHashMap<>();
    	LocalDate cursor = start;
    	for(int i = 0; i<months;i++) {
    		String key = cursor.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    		bucket.put(key, new ArrayList<>());
    		cursor = cursor.plusMonths(1);
    	}
    	rows.forEach(r -> {
    	      String key = r.getRvdate().substring(0,7);
    	      bucket.computeIfAbsent(key, k -> new ArrayList<>()).add(parseRating(r.getRvrating()));
    	    });

    	    List<String> labels = new ArrayList<>();
    	    List<Integer> counts = new ArrayList<>();
    	    List<Double> avgs   = new ArrayList<>();
    	    bucket.forEach((k, list) -> {
    	      labels.add(YearMonth.parse(k).getMonthValue()+"월");
    	      counts.add(list.size());
    	      avgs.add(list.isEmpty()? 0d : Math.round((list.stream().mapToDouble(x->x).average().orElse(0))*10.0)/10.0);
    	    });
    	    return new MonthlyStatsDto(labels, counts, avgs);
    	  }
    
    private static double parseRating(String s) {
    	try { return Double.parseDouble(s.trim()); } catch(Exception e) {return 0.0;}
    	}
    private static String maskUid(String uid) {
    	if(uid == null || uid.length() < 3) return "****";
    	return uid.substring(0, Math.min(3, uid.length())) + "***";
    			
    }
    }

