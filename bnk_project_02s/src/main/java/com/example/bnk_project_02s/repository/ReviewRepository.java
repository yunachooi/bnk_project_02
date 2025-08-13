package com.example.bnk_project_02s.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bnk_project_02s.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, String> {
	
	 @Query(value = "SELECT COALESCE(MAX(CAST(rvno AS SIGNED)), 0) FROM bnk_review", nativeQuery = true)
	 int findMaxRvno();
	 
	// 최신 리뷰 n건 (rvdate 내림차순 → 최근순)
	@Query(value = "SELECT * FROM bnk_review ORDER BY rvdate DESC, rvno DESC LIMIT :limit", nativeQuery = true)
	List<Review> findRecent(@Param("limit") int limit);

	    // 기간 내 리뷰(yyyy-MM-dd 문자열 비교 가능)
	@Query(value = """
	        SELECT *
	        FROM bnk_review
	        WHERE STR_TO_DATE(rvdate,'%Y-%m-%d %H:%i:%s') >= :start
	          AND STR_TO_DATE(rvdate,'%Y-%m-%d %H:%i:%s') < :endEx
	        ORDER BY rvdate ASC
	        """, nativeQuery = true)
	List<Review> findByDateRange(@Param("start") LocalDateTime start,
	                             @Param("endEx") LocalDateTime endEx);
}