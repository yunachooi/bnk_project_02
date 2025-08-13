package com.example.bnk_project_02s.auth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class AesGcmConverter implements AttributeConverter<String,String> {
    @Override public String convertToDatabaseColumn(String v){
        return (v==null||v.isBlank())?null:CryptoBeans.AES.encrypt(v);
    }
    @Override public String convertToEntityAttribute(String v){
        return (v==null||v.isBlank())?null:CryptoBeans.AES.decrypt(v);
    }
}