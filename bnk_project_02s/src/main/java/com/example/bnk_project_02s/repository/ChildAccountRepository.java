package com.example.bnk_project_02s.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bnk_project_02s.entity.ChildAccount;

@Repository
public interface ChildAccountRepository extends JpaRepository<ChildAccount, String> {
    List<ChildAccount> findByParentAccount_Pano(String pano);
    
    Optional<ChildAccount> findByParentAccount_PanoAndCurrency_Cuno(String pano, String cuno);
    
    Optional<ChildAccount> findByParentAccount_PanoAndCurrency_Cuname(String pano, String cuname);
}
