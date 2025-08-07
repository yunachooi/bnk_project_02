package com.example.bnk_project_02s.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForexRateDto {
	
	@JsonProperty("cur_unit")
	private String rcode; //통화코드 (USD, JPY)

	@JsonProperty("cur_nm")
	private String rcurrency; //통화명 (미국달러, 일본 엔)
 
	@JsonProperty("deal_bas_r")
	private String rvalue; //환율값 (매매기준율)
	
//	private String rcode; //통화코드
//	private String rvalue; //환율값 (매매기준율)
//	private String ramount; //차액
//	private String rpercent; //변동률
	

}
