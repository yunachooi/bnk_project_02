package com.example.bnk_project_02s.repository;

import com.example.bnk_project_02s.entity.CashKrw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashKrwRepository extends JpaRepository<CashKrw, Long> {
    Optional<CashKrw> findByUser_Uid(String uid);
}