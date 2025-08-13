package com.example.bnk_project_02s.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bnk_project_02s.entity.ShoppingProducts;

public interface ShoppingProductRepository extends JpaRepository<ShoppingProducts, String> {
	
}
