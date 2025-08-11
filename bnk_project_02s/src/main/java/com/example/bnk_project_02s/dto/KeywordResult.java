package com.example.bnk_project_02s.dto;
//워드클라우드 키워드 아이템/결과
public record KeywordResult(java.util.List<KeywordItem> positive, java.util.List<KeywordItem> negative) {}