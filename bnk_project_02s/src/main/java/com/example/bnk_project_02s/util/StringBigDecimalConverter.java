package com.example.bnk_project_02s.util;

import java.math.BigDecimal;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class StringBigDecimalConverter implements AttributeConverter<BigDecimal, String> {
    @Override public String convertToDatabaseColumn(BigDecimal attribute) {
        return attribute == null ? "0" : attribute.toPlainString();
    }
    @Override public BigDecimal convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(dbData.trim()); }
        catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
}