package com.example.bnk_project_02s.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
	@Id
	private String username;
}
