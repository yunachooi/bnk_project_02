package com.example.bnk_project_02s.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.example.bnk_project_02s.entity.History;

public interface HistoryStatsRepository extends Repository<History, Long> {

    /** 총 원화 사용액 (hkrw가 DECIMAL이면 그대로 SUM; 문자형이면 암묵 캐스팅되어도 동작) */
    @Query(value = "SELECT COALESCE(SUM(h.hkrw), 0) FROM bnk_history h", nativeQuery = true)
    java.math.BigDecimal sumTotalKrw();

    /** [수정] 일자별 KRW 합계 — LEFT(hdate,10)로 안전 집계 */
    @Query(value = """
        SELECT LEFT(h.hdate, 10) AS d, COALESCE(SUM(h.hkrw),0) AS total
        FROM bnk_history h
        WHERE LEFT(h.hdate,10) >= DATE_FORMAT(:start, '%Y-%m-%d')
          AND LEFT(h.hdate,10) <  DATE_FORMAT(:endEx, '%Y-%m-%d')
        GROUP BY LEFT(h.hdate,10)
        ORDER BY d
        """, nativeQuery = true)
    List<Object[]> sumKrwByDay(@Param("start") LocalDateTime start,
                               @Param("endEx") LocalDateTime endEx);

    /** [수정] 일자별 통화별 외화 환전금액 합계 — 입금-출금 절댓값 합(숫자/콤마 대응) */
    @Query(value = """
        SELECT LEFT(h.hdate,10) AS d,
               h.cuno AS cuno,
               SUM(
                 ABS(
                   COALESCE(CAST(REPLACE(h.hdeposit,  ',', '') AS DECIMAL(18,4)), 0) -
                   COALESCE(CAST(REPLACE(h.hwithdraw, ',', '') AS DECIMAL(18,4)), 0)
                 )
               ) AS amt
        FROM bnk_history h
        WHERE LEFT(h.hdate,10) >= DATE_FORMAT(:start, '%Y-%m-%d')
          AND LEFT(h.hdate,10) <  DATE_FORMAT(:endEx, '%Y-%m-%d')
        GROUP BY LEFT(h.hdate,10), h.cuno
        ORDER BY d, cuno
        """, nativeQuery = true)
    List<Object[]> sumFxByDayCurrency(@Param("start") LocalDateTime start,
                                      @Param("endEx") LocalDateTime endEx);
}

