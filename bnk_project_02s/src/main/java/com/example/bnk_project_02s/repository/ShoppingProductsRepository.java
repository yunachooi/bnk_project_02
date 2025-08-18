package com.example.bnk_project_02s.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bnk_project_02s.entity.ShoppingProducts;

public interface ShoppingProductsRepository extends JpaRepository<ShoppingProducts, String> {
	
}
