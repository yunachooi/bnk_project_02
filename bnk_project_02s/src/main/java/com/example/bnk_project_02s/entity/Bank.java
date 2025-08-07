package com.example.bnk_project_02s.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bnk_bank")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank {
	
	@Id
	@Column(name = "bno", length = 30)
	private Long bno;
	
	@Column(name = "bname", length = 100)
    private String bname; // 영업점명

    @Column(name = "baddress", length = 255)
    private String baddress; // 주소

    @Column(name = "blatitude", length = 50)
    private String blatitude; // 위도

    @Column(name = "blongitude", length = 50)
    private String blongitude; // 경도

    @Column(name = "bphone", length = 30)
    private String bphone; // 연락처

    @Column(name = "bdigital", length = 1)
    private String bdigital; // 디지털 영업점 여부 (Y/N)

    @Column(name = "bopenhour", length = 100)
    private String bopenhour; // 영업시간
	

}
