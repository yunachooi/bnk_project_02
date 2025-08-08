package com.example.bnk_project_02s.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForexRateDto {
	
	@JsonProperty("TIME")
    private String rdate;

    @JsonProperty("ITEM_CODE1")
    private String rcode;

    @JsonProperty("ITEM_NAME1")
    private String rcurrency;

    @JsonProperty("DATA_VALUE")
    private String rvalue;
	

	

}
