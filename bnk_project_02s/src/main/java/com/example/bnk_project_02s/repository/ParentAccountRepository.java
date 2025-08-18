package com.example.bnk_project_02s.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.bnk_project_02s.entity.ParentAccount;

@Repository
public interface ParentAccountRepository extends JpaRepository<ParentAccount, String> {
    Optional<ParentAccount> findByUser_Uid(String uid);
    Optional<ParentAccount> findFirstByUser_Uid(String uid);
}