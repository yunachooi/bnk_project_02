package com.example.bnk_project_02s.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.bnk_project_02s.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, String> {
	
	 @Query(value = "SELECT COALESCE(MAX(CAST(rvno AS SIGNED)), 0) FROM bnk_review", nativeQuery = true)
	    int findMaxRvno();
}
