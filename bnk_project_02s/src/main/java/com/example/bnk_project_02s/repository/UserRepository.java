package com.example.bnk_project_02s.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.bnk_project_02s.entity.User;

/**
 * User 테이블 전용 JPA 리포지터리
 *  - 기본 CRUD (save, findById, delete 등) 는 JpaRepository 가 제공
 *  - 커스텀 쿼리 메서드 existsByUid 로 중복 ID 체크
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /** 아이디 중복 여부 */
    boolean existsByUid(String uid);
    
    
    
    
    
    
    
    
    
    
    
    
    
    @Query("""
    		  SELECT 
    		    CASE 
    		      WHEN TIMESTAMPDIFF(YEAR, STR_TO_DATE(u.ubirth, '%Y-%m-%d'), CURDATE()) BETWEEN 10 AND 19 THEN '10대'
    		      WHEN TIMESTAMPDIFF(YEAR, STR_TO_DATE(u.ubirth, '%Y-%m-%d'), CURDATE()) BETWEEN 20 AND 29 THEN '20대'
    		      WHEN TIMESTAMPDIFF(YEAR, STR_TO_DATE(u.ubirth, '%Y-%m-%d'), CURDATE()) BETWEEN 30 AND 39 THEN '30대'
    		      WHEN TIMESTAMPDIFF(YEAR, STR_TO_DATE(u.ubirth, '%Y-%m-%d'), CURDATE()) BETWEEN 40 AND 49 THEN '40대'
    		      WHEN TIMESTAMPDIFF(YEAR, STR_TO_DATE(u.ubirth, '%Y-%m-%d'), CURDATE()) BETWEEN 50 AND 59 THEN '50대'
    		      ELSE '60대+'
    		    END AS ageGroup,
    		    COUNT(*) 
    		  FROM User u
    		  WHERE u.ubirth IS NOT NULL AND u.ubirth != ''
    		  GROUP BY ageGroup
    		""")
    		List<Object[]> countByAgeGroup();
    
}