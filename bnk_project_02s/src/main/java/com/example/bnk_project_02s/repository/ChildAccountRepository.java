package com.example.bnk_project_02s.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bnk_project_02s.entity.ChildAccount;

public interface ChildAccountRepository extends JpaRepository<ChildAccount, String> {
	Optional<ChildAccount> findByCano(String cano);
}
