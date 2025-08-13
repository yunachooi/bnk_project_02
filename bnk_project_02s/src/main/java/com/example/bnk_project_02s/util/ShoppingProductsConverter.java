package com.example.bnk_project_02s.util;

import com.example.bnk_project_02s.dto.ShoppingProductsDto;
import com.example.bnk_project_02s.entity.ShoppingProducts;

public class ShoppingProductsConverter {
    
    public static ShoppingProductsDto toDto(ShoppingProducts entity) {
        ShoppingProductsDto dto = new ShoppingProductsDto();
        dto.setSpno(entity.getSpno());
        dto.setSpname(entity.getSpname());
        dto.setSpnameKo(entity.getSpnameKo());
        dto.setSpdescription(entity.getSpdescription());
        dto.setSpdescriptionKo(entity.getSpdescriptionKo());
        dto.setSpprice(entity.getSpprice());
        dto.setSpcurrency(entity.getSpcurrency());
        dto.setSprating(entity.getSprating());
        dto.setSpreviews(entity.getSpreviews());
        dto.setSpimgurl(entity.getSpimgurl());
        dto.setSpurl(entity.getSpurl());
        dto.setSpat(entity.getSpat());
        return dto;
    }
    
    public static ShoppingProducts toEntity(ShoppingProductsDto dto) {
        ShoppingProducts entity = new ShoppingProducts();
        entity.setSpno(dto.getSpno());
        entity.setSpname(dto.getSpname());
        entity.setSpnameKo(dto.getSpnameKo());
        entity.setSpdescription(dto.getSpdescription());
        entity.setSpdescriptionKo(dto.getSpdescriptionKo());
        entity.setSpprice(dto.getSpprice());
        entity.setSpcurrency(dto.getSpcurrency());
        entity.setSprating(dto.getSprating());
        entity.setSpreviews(dto.getSpreviews());
        entity.setSpimgurl(dto.getSpimgurl());
        entity.setSpurl(dto.getSpurl());
        entity.setSpat(dto.getSpat());
        return entity;
    }
}