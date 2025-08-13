package com.example.bnk_project_02s.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bnk_project_02s.dto.CardDto;
import com.example.bnk_project_02s.entity.Card;
import com.example.bnk_project_02s.repository.CardRepository;
import com.example.bnk_project_02s.util.CardConverter;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardConverter cardConverter;

    public CardDto getCardByUserId(String uid) {
        Card card = cardRepository.findByUser_Uid(uid)
                .orElseThrow(() -> new RuntimeException("카드 정보를 찾을 수 없습니다."));

        CardDto cardDto = cardConverter.toDto(card);
        
        return cardDto;
    }

    public String getFullCardNumber(String uid) {
        Card card = cardRepository.findByUser_Uid(uid)
                .orElseThrow(() -> new RuntimeException("카드 정보를 찾을 수 없습니다."));

        String cardNumber = card.getCardno();
        if (cardNumber != null && cardNumber.length() == 16) {
            return cardNumber.substring(0, 4) + "-" +
                   cardNumber.substring(4, 8) + "-" +
                   cardNumber.substring(8, 12) + "-" +
                   cardNumber.substring(12, 16);
        }

        return cardNumber;
    }

    @Transactional
    public boolean toggleCardStatus(String uid) {
        Card card = cardRepository.findByUser_Uid(uid)
                .orElseThrow(() -> new RuntimeException("카드 정보를 찾을 수 없습니다."));

        String newStatus = "Y".equals(card.getCardstatus()) ? "N" : "Y";
        card.setCardstatus(newStatus);

        cardRepository.save(card);
        return true;
    }
}