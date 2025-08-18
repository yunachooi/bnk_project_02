package com.example.bnk_project_02s.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** KRW(원화) 전용 Converter: 정수, 소수점 버림 */
@Converter(autoApply = false)
public class KrwConverter implements AttributeConverter<BigDecimal, String> {

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        if (attribute == null) return "0";
        return attribute.setScale(0, RoundingMode.DOWN).toPlainString();
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(dbData.trim()).setScale(0, RoundingMode.DOWN);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
