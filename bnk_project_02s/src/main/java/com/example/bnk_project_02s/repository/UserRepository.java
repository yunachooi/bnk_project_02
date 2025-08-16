package com.example.bnk_project_02s.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.bnk_project_02s.entity.User;

/**
 * User 테이블 전용 JPA 리포지터리
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /** 아이디 중복 여부 */
    boolean existsByUid(String uid);

    /** 주민등록번호(HMAC) 중복 여부 */
    boolean existsByUrrnHmac(String urrnHmac);

    /** 휴대번호(HMAC) 중복 여부 — 엔티티에 uphoneHmac 필드가 있어야 함 */
    boolean existsByUphoneHmac(String uphoneHmac);

    /** (선택) 휴대번호 HMAC로 사용자 조회 */
    Optional<User> findByUphoneHmac(String uphoneHmac);

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
    
    // ✅ 관심 통화: CSV에서 각 토큰을 개별 카운트 (공백 무시)
    @Query(value = """
        SELECT t.k AS k,
               COALESCE(SUM(
                 CASE
                   WHEN FIND_IN_SET(t.k, REPLACE(UPPER(u.ucurrency), ' ', '')) > 0 THEN 1
                   ELSE 0
                 END
               ),0) AS cnt
        FROM (SELECT 'USD' AS k UNION ALL SELECT 'JPY' UNION ALL SELECT 'CNH'
              UNION ALL SELECT 'EUR' UNION ALL SELECT 'CHF' UNION ALL SELECT 'VND') t
        LEFT JOIN bnk_user2 u ON 1=1
        GROUP BY t.k
        ORDER BY FIELD(t.k,'USD','JPY','CNH','EUR','CHF','VND')
        """, nativeQuery = true)
    List<Object[]> countInterestCurrenciesCsv();

    // ✅ 관심 분야: CSV에서 각 토큰을 개별 카운트 (공백 무시)
    @Query(value = """
        SELECT t.k AS k,
               COALESCE(SUM(
                 CASE
                   WHEN FIND_IN_SET(t.k, REPLACE(UPPER(u.uinterest), ' ', '')) > 0 THEN 1
                   ELSE 0
                 END
               ),0) AS cnt
        FROM (SELECT 'TRAVEL' AS k UNION ALL SELECT 'STUDY' UNION ALL SELECT 'SHOPPING'
              UNION ALL SELECT 'FINANCE' UNION ALL SELECT 'ETC') t
        LEFT JOIN bnk_user2 u ON 1=1
        GROUP BY t.k
        ORDER BY FIELD(t.k,'TRAVEL','STUDY','SHOPPING','FINANCE','ETC')
        """, nativeQuery = true)
    List<Object[]> countInterestTopicsCsv();
    
    @Query("""
            select upper(trim(u.ugender)) as g, count(u)
            from User u
            group by upper(trim(u.ugender))
            """)
    List<Object[]> countByGenderRaw();
    
    // 이름/아이디 부분검색
    Page<User> findByUidContainingIgnoreCaseOrUnameContainingIgnoreCase(
            String uid, String uname, Pageable pageable);

    // 휴대폰은 AES-GCM이라 LIKE가 불가 → 동등검색만 지원.
    // @Convert가 파라미터에도 적용되므로, “평문”을 넘기면 내부에서 암호문으로 비교됨.
    Page<User> findByUphoneEnc(String phonePlain, Pageable pageable);
}