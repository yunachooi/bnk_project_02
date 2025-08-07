package com.example.bnk_project_02s.util;

import com.example.bnk_project_02s.dto.ShoppingProductDto;
import com.example.bnk_project_02s.entity.ShoppingProduct;

public class ShoppingProductConveter {
	public static ShoppingProductDto toDto(ShoppingProduct entity) {
        ShoppingProductDto dto = new ShoppingProductDto();
        dto.setSpno(entity.getSpno());
        dto.setSpname(entity.getSpname());
        dto.setSpdescription(entity.getSpdescription());
        dto.setSpprice(entity.getSpprice());
        dto.setSpcurrency(entity.getSpcurrency());
        dto.setSprating(entity.getSprating());
        dto.setSpreviews(entity.getSpreviews());
        dto.setSpurl(entity.getSpurl());
        dto.setSpat(entity.getSpat());
        return dto;
    }

    public static ShoppingProduct toEntity(ShoppingProductDto dto) {
        ShoppingProduct entity = new ShoppingProduct();
        entity.setSpno(dto.getSpno());
        entity.setSpname(dto.getSpname());
        entity.setSpdescription(dto.getSpdescription());
        entity.setSpprice(dto.getSpprice());
        entity.setSpcurrency(dto.getSpcurrency());
        entity.setSprating(dto.getSprating());
        entity.setSpreviews(dto.getSpreviews());
        entity.setSpurl(dto.getSpurl());
        entity.setSpat(dto.getSpat());
        return entity;
    }
}
