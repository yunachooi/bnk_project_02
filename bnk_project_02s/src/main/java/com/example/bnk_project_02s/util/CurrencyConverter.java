package com.example.bnk_project_02s.util;

import org.springframework.stereotype.Component;

import com.example.bnk_project_02s.dto.CurrencyDto;
import com.example.bnk_project_02s.entity.Currency;

@Component
public class CurrencyConverter {
    public CurrencyDto toDto(Currency entity) {
        if (entity == null) return null;
        return new CurrencyDto(
                entity.getCuno(),
                entity.getCuname()
        );
    }

    public Currency toEntity(CurrencyDto dto) {
        if (dto == null) return null;
        return new Currency(
                dto.getCuno(),
                dto.getCuname()
        );
    }
}
