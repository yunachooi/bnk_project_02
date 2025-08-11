package com.example.bnk_project_02s.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bnk_project_02s.entity.CustomerRate;

public interface CustomerRateRepository extends JpaRepository<CustomerRate, Long> {
	
	Optional<CustomerRate> findByCcodeAndCdate(String ccode, LocalDate cdate);

}
