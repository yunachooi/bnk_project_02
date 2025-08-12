package com.example.bnk_project_02s.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bnk_project_02s.entity.Card;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    
    Optional<Card> findByUserUid(String uid);
    
    Optional<Card> findByCardno(String cardno);
    
    Optional<Card> findByUserUidAndCardstatus(String uid, String cardstatus);
    
    Optional<Card> findByCano(String cano);
    
    boolean existsByUserUid(String uid);
    
    List<Card> findAllByUserUid(String uid);
}