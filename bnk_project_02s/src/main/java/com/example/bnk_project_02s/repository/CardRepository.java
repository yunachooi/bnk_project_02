package com.example.bnk_project_02s.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bnk_project_02s.entity.Card;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    
    Optional<Card> findByUser_Uid(String uid);
    
}