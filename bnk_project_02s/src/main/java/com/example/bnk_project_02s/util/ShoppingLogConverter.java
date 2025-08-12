package com.example.bnk_project_02s.util;

import org.springframework.stereotype.Component;

import com.example.bnk_project_02s.dto.ShoppingLogDto;
import com.example.bnk_project_02s.entity.ShoppingLog;

@Component
public class ShoppingLogConverter {
    public ShoppingLogDto toDto(ShoppingLog shoppingLog) {
        if (shoppingLog == null) {
            return null;
        }
        
        return ShoppingLogDto.builder()
                .slno(shoppingLog.getSlno())
                .uid(shoppingLog.getUser() != null ? shoppingLog.getUser().getUid() : null)
                .spno(shoppingLog.getShoppingProducts() != null ? shoppingLog.getShoppingProducts().getSpno() : null)
                .cardno(shoppingLog.getCard() != null ? shoppingLog.getCard().getCardno() : null)
                .slamount(shoppingLog.getSlamount())
                .slcurrency(shoppingLog.getSlcurrency())
                .slexrate(shoppingLog.getSlexrate())
                .slstatus(shoppingLog.getSlstatus())
                .slrequest(shoppingLog.getSlrequest())
                .sldate(shoppingLog.getSldate())
                .build();
    }

    public ShoppingLog toEntity(ShoppingLogDto shoppingLogDto) {
        if (shoppingLogDto == null) {
            return null;
        }
        
        return ShoppingLog.builder()
                .slno(shoppingLogDto.getSlno())
                .slamount(shoppingLogDto.getSlamount())
                .slcurrency(shoppingLogDto.getSlcurrency())
                .slexrate(shoppingLogDto.getSlexrate())
                .slstatus(shoppingLogDto.getSlstatus())
                .slrequest(shoppingLogDto.getSlrequest())
                .sldate(shoppingLogDto.getSldate())
                .build();
    }
}