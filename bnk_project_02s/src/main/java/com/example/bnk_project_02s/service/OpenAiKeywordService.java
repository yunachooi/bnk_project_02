package com.example.bnk_project_02s.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.bnk_project_02s.dto.KeywordItem;
import com.example.bnk_project_02s.dto.KeywordResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpenAiKeywordService {
	
	private final OpenAiService openAi;
	@Value("${openai.model:gpt-4o-mini}")
	  private String model;
	
	//리뷰 텍스트 > 긍/부정 키워드 (워드클라우드용) 10개씩
	public KeywordResult extract(List<String> texts) {
		//텍스트 너무 길면 잘라서 전송(토큰 안전)
		String joined = String.join("\n", texts);
		if(joined.length() > 6000) joined = joined.substring(0, 6000);
		
		ChatMessage system = new ChatMessage("system",
			      "당신은 한국어 텍스트 분석가입니다. 아래 리뷰 텍스트를 분석해 긍정/부정 '키워드'를 추출하세요. " +
			      "출력은 반드시 JSON만 반환합니다. " +
			      "형식: {\"positive\":[{\"name\":\"...\",\"value\":N},...],\"negative\":[...]} " +
			      "name=키워드(2~8자), value=가중치(1~100). 유사어는 병합하고 상위 10개씩만."
			    );
		ChatMessage user = new ChatMessage("user", "리뷰 텍스트:\n" + joined);
		
		ChatCompletionRequest req = ChatCompletionRequest.builder()
				.model(model)
				.messages(List.of(system, user))
				.temperature(0.2)
				.maxTokens(400)
				.build();
		
		String json = openAi.createChatCompletion(req).getChoices().get(0).getMessage().getContent();
		
		// 파싱 관용적으로 처리
	    try {
	      ObjectMapper om = new ObjectMapper();
	      JsonNode root = om.readTree(json);
	      List<KeywordItem> pos = new ArrayList<>();
	      List<KeywordItem> neg = new ArrayList<>();
	      if(root.has("positive")){
	        for(JsonNode n : root.get("positive")){
	          pos.add(new KeywordItem(n.get("name").asText(), n.get("value").asInt(10)));
	        }
	      }
	      if(root.has("negative")){
	        for(JsonNode n : root.get("negative")){
	          neg.add(new KeywordItem(n.get("name").asText(), n.get("value").asInt(10)));
	        }
	      }
	      return new KeywordResult(pos, neg);
	    } catch (Exception e){
	      // 실패 시 비어있는 결과
	      return new KeywordResult(List.of(), List.of());
	    }
	  }
	}