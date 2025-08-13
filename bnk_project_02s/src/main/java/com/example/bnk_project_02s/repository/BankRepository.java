package com.example.bnk_project_02s.repository;

import com.example.bnk_project_02s.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankRepository extends JpaRepository<Bank, Long> {

    // 좌표가 비어있는(=NULL) 행
    List<Bank> findByBlatitudeIsNullOrBlongitudeIsNull();

    // 좌표가 채워진 행 (NOT NULL + "" 제외)
    List<Bank> findByBlatitudeIsNotNullAndBlatitudeNotAndBlongitudeIsNotNullAndBlongitudeNot(
            String empty1, String empty2
    );

    // 디지털(Y) + 좌표가 채워진 행
    List<Bank> findByBdigitalIgnoreCaseAndBlatitudeIsNotNullAndBlatitudeNotAndBlongitudeIsNotNullAndBlongitudeNot(
            String y, String empty1, String empty2
    );

    /* -------- 편의 메서드(컨트롤러에서 깔끔하게 쓰기) -------- */

    default List<Bank> findAllWithCoords() {
        return findByBlatitudeIsNotNullAndBlatitudeNotAndBlongitudeIsNotNullAndBlongitudeNot("", "");
    }

    default List<Bank> findAllWithCoordsDigitalOnly() {
        return findByBdigitalIgnoreCaseAndBlatitudeIsNotNullAndBlatitudeNotAndBlongitudeIsNotNullAndBlongitudeNot(
                "Y", "", ""
        );
    }
}
