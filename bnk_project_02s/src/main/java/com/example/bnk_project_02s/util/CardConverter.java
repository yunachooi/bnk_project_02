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
                .cano(card.getCano())
                .cacvc(card.getCacvc())
                .caname(card.getCaname())
                .pano(card.getPano())
                .cuno(card.getCuno())
                .castatus(card.getCastatus())
                .cadate(card.getCadate())
                .build();
    }

    public Card toEntity(CardDto cardDto) {
        if (cardDto == null) {
            return null;
        }
        
        return Card.builder()
                .cano(cardDto.getCano())
                .cacvc(cardDto.getCacvc())
                .caname(cardDto.getCaname())
                .pano(cardDto.getPano())
                .cuno(cardDto.getCuno())
                .castatus(cardDto.getCastatus())
                .cadate(cardDto.getCadate())
                .build();
    }
}