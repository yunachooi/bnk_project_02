package com.example.bnk_project_02s.util;

import org.springframework.stereotype.Component;

import com.example.bnk_project_02s.dto.CardDto;
import com.example.bnk_project_02s.entity.Card;
import com.example.bnk_project_02s.entity.User;

@Component
public class CardConverter {
    
    public CardDto toDto(Card card) {
        if (card == null) {
            return null;
        }
        
        return CardDto.builder()
                .cardno(card.getCardno())
                .cano(card.getCano())
                .uid(card.getUser() != null ? card.getUser().getUid() : null)
                .cardcvc(card.getCardcvc())
                .cardname(card.getCardname())
                .cardstatus(card.getCardstatus())
                .carddate(card.getCarddate())
                .build();
    }

    public Card toEntity(CardDto cardDto) {
        if (cardDto == null) {
            return null;
        }
        
        Card card = Card.builder()
                .cardno(cardDto.getCardno())
                .cano(cardDto.getCano())
                .cardcvc(cardDto.getCardcvc())
                .cardname(cardDto.getCardname())
                .cardstatus(cardDto.getCardstatus())
                .carddate(cardDto.getCarddate())
                .build();
        
        if (cardDto.getUid() != null) {
            User user = new User();
            user.setUid(cardDto.getUid());
            card.setUser(user);
        }
        
        return card;
    }
}