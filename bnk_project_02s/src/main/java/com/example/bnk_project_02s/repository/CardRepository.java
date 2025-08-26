package com.example.bnk_project_02s.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.bnk_project_02s.entity.Card;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    
    Optional<Card> findByUser_Uid(String uid);
    
    @Modifying @Transactional void deleteByUser_Uid(String uid);
    boolean existsByUser_Uid(String uid);
    
}