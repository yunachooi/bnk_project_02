package com.example.bnk_project_02s.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EximRateDto {
	
    @JsonProperty("cur_unit")   
    private String code;      // USD, JPY(100) ...
    
    @JsonProperty("cur_nm")     
    private String name;      // 미국 달러
    
    @JsonProperty("deal_bas_r") 
    private String dealBasR;  // "1,386.7"
    
    @JsonProperty("tts")        
    private String tts;       // "1,400.56"
}
