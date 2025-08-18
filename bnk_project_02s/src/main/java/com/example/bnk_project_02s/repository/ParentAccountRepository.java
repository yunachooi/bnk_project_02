package com.example.bnk_project_02s.repository;

import com.example.bnk_project_02s.entity.ParentAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParentAccountRepository extends JpaRepository<ParentAccount, String> {
    // uid로 대표 부모계좌 1건 조회 (여러 개면 최신/대표 기준 추가 가능)
    Optional<ParentAccount> findFirstByUser_Uid(String uid);
}
