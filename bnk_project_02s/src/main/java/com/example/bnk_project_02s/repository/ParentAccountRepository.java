package com.example.bnk_project_02s.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bnk_project_02s.entity.ParentAccount;

@Repository
public interface ParentAccountRepository extends JpaRepository<ParentAccount, String> {

    /** 특정 유저의 부모계좌(여러 개일 수 있으면 List, 단일만 보장되면 Optional) */
    Optional<ParentAccount> findByUser_Uid(String uid);
    Optional<ParentAccount> findFirstByUser_Uid(String uid);

    /** ✅ pano로 직접 조회 (ExchangeHistoryService에서 사용) */
    Optional<ParentAccount> findByPano(String pano);

    /** 유저별 전체 부모계좌 목록 (멀티 계좌 운영 가능성 대비) */
    List<ParentAccount> findAllByUser_Uid(String uid);

    /** 존재 여부/개수 체크 (선택적 유틸) */
    boolean existsByUser_Uid(String uid);
    long countByUser_Uid(String uid);

    /** 유저 소유의 특정 pano 검증이 필요할 때 */
    Optional<ParentAccount> findByPanoAndUser_Uid(String pano, String uid);
}
