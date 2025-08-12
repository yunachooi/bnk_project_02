package com.example.bnk_project_02s.repository;

import com.example.bnk_project_02s.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BankRepository extends JpaRepository<Bank, Long> {

    /** 위경도 비어있는 행(컬럼이 String일 때 공백까지 포함) */
    @Query("""
           SELECT b FROM Bank b
           WHERE (b.blatitude IS NULL OR b.blatitude = '')
              OR (b.blongitude IS NULL OR b.blongitude = '')
           """)
    List<Bank> findAllMissingCoords();

    /** 위경도 채워진 행만 (마커용) */
    @Query("""
           SELECT b FROM Bank b
           WHERE (b.blatitude IS NOT NULL AND b.blatitude <> '')
             AND (b.blongitude IS NOT NULL AND b.blongitude <> '')
           """)
    List<Bank> findAllWithCoords();

    
}
