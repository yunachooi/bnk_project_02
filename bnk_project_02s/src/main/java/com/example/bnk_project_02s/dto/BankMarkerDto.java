package com.example.bnk_project_02s.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BankMarkerDto {
	private Long bno;
	private String name;
	private String addr;
	private double lat; //위도
	private double lng; //경도
	private String phone;
	private boolean digital; //디지털 영업점 여부

}
