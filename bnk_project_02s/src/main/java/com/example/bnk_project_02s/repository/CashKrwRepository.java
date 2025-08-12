package com.example.bnk_project_02s.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bnk_project_02s.entity.CashKrw;

public interface CashKrwRepository extends JpaRepository<CashKrw, Long> {

	Optional<CashKrw> findByUid(String uid);
}
