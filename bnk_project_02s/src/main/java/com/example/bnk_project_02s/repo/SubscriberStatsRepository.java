package com.example.bnk_project_02s.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bnk_project_02s.entity.ParentAccount;

public interface SubscriberStatsRepository extends JpaRepository<ParentAccount, String> {
	  @Query(value = """
	      SELECT DATE(pajoin) AS d, COUNT(*) AS c
	      FROM bnk_parent_account
	      WHERE pajoin >= :start AND pajoin < :end
	      GROUP BY d ORDER BY d
	      """, nativeQuery = true)
	  List<DayCount> countDaily(@Param("start") LocalDate start, @Param("end") LocalDate end);

	  @Query(value = """
	      SELECT DATE_FORMAT(pajoin, '%Y-%m') AS ym, COUNT(*) AS c
	      FROM bnk_parent_account
	      WHERE pajoin >= :start AND pajoin < :end
	      GROUP BY ym ORDER BY ym
	      """, nativeQuery = true)
	  List<MonthCount> countMonthly(@Param("start") LocalDate startMonthFirstDay,
	                                @Param("end")   LocalDate endMonthFirstDay);

	  @Query(value = """
	      SELECT CONCAT(YEAR(pajoin), ' Q', QUARTER(pajoin)) AS yq, COUNT(*) AS c
	      FROM bnk_parent_account
	      WHERE pajoin >= :start AND pajoin < :end
	      GROUP BY yq ORDER BY MIN(pajoin)
	      """, nativeQuery = true)
	  List<QuarterCount> countQuarterly(@Param("start") LocalDate startQuarterFirstDay,
	                                    @Param("end")   LocalDate endQuarterFirstDay);
	}