package com.example.bnk_project_02s.util;

import org.springframework.stereotype.Component;

import com.example.bnk_project_02s.dto.CardDto;
import com.example.bnk_project_02s.entity.Card;

@Component
public class CardConverter {
    public CardDto toDto(Card card) {
        if (card == null) {
            return null;
        }
        
        return CardDto.builder()
                .cardno(card.getCardno())
                .cardcvc(card.getCardcvc())
                .cardname(card.getCardname())
                .pano(card.getPano())
                .cuno(card.getCuno())
                .cardstatus(card.getCardstatus())
                .carddate(card.getCarddate())
                .build();
    }

    public Card toEntity(CardDto cardDto) {
        if (cardDto == null) {
            return null;
        }
        
        return Card.builder()
                .cardno(cardDto.getCardno())
                .cardcvc(cardDto.getCardcvc())
                .cardname(cardDto.getCardname())
                .pano(cardDto.getPano())
                .cuno(cardDto.getCuno())
                .cardstatus(cardDto.getCardstatus())
                .carddate(cardDto.getCarddate())
                .build();
    }
}