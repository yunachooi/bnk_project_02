package com.example.bnk_project_02s.repository;

import java.util.List;
import java.util.Optional;

import com.example.bnk_project_02s.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * User 테이블 전용 JPA 리포지터리
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /** 아이디 중복 여부 */
    boolean existsByUid(String uid);

    /** 주민등록번호(HMAC) 중복 여부 — 엔티티 필드명에 맞춰 변경 */
    boolean existsByUrrnHmac(String urrnHmac);

    /** 로그인/조회 시 사용 */
    Optional<User> findByUid(String uid);

    @Query(value = """
        SELECT 
          CASE 
            WHEN TIMESTAMPDIFF(YEAR, STR_TO_DATE(u.ubirth, '%Y-%m-%d'), CURDATE()) BETWEEN 10 AND 19 THEN '10대'
            WHEN TIMESTAMPDIFF(YEAR, STR_TO_DATE(u.ubirth, '%Y-%m-%d'), CURDATE()) BETWEEN 20 AND 29 THEN '20대'
            WHEN TIMESTAMPDIFF(YEAR, STR_TO_DATE(u.ubirth, '%Y-%m-%d'), CURDATE()) BETWEEN 30 AND 39 THEN '30대'
            WHEN TIMESTAMPDIFF(YEAR, STR_TO_DATE(u.ubirth, '%Y-%m-%d'), CURDATE()) BETWEEN 40 AND 49 THEN '40대'
            WHEN TIMESTAMPDIFF(YEAR, STR_TO_DATE(u.ubirth, '%Y-%m-%d'), CURDATE()) BETWEEN 50 AND 59 THEN '50대'
            ELSE '60대+'
          END AS ageGroup,
          COUNT(*) AS cnt
        FROM bnk_user2 u
        WHERE u.ubirth IS NOT NULL AND u.ubirth != ''
        GROUP BY ageGroup
    """, nativeQuery = true)
    List<Object[]> countByAgeGroup();
}