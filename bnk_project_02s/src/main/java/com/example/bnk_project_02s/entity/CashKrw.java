package com.example.bnk_project_02s.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_cash")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashKrw {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column
	private Long cno;
	
	@Column(name = "uid", length = 50, nullable = false, unique = true)
	private String uid;
	
	@Column(name = "ccount")
	private Integer ccount;
	
	@Column(name = "cfxamount", length = 50)
	private String cfxamount;

}
