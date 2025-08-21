package com.example.bnk_project_02s.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bnk_project_02s.entity.ChildAccount;

@Repository
public interface ChildAccountRepository extends JpaRepository<ChildAccount, String> {

    /** 부모계좌(pano) 하위 전체 자식계좌 조회 */
    List<ChildAccount> findByParentAccount_Pano(String pano);

    /** 부모계좌(pano) + 통화코드(cuname) → 특정 외화통장 1개 찾기 */
    Optional<ChildAccount> findByParentAccount_PanoAndCurrency_Cuname(String pano, String cuname);

    /** 특정 유저(uid)의 모든 자식계좌 */
    List<ChildAccount> findByParentAccount_User_Uid(String uid);
    
    Optional<ChildAccount> findByParentAccount_PanoAndCurrency_Cuno(String pano, String cuno);
    
    Optional<ChildAccount> findByCano(String cano);
}
