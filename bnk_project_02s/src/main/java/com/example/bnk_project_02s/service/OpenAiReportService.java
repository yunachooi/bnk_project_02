package com.example.bnk_project_02s.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

@Service
public class OpenAiReportService {
	
	private final OpenAiService service;
	
	public OpenAiReportService(@Value("${openai.api.key}") String apiKey) {
        this.service = new OpenAiService(apiKey, Duration.ofSeconds(60));
    }
	
	public String generateDashBoardReport(String statsJson) {
        try {
            ChatMessage systemMessage = new ChatMessage(
                "system",
                "You are a senior product analyst. Summarize the dashboard data in Korean for executives. " +
                "Include: 가입자 추이 핵심 포인트, 연령/성별 인사이트, 리뷰/평점 변화, 키워드 요약, " +
                "누적 사용액·공유 클릭 의미, 다음 액션 3가지를 간결히."
            );

            ChatMessage userMessage = new ChatMessage(
                "user",
                "아래 JSON은 대시보드 원시 데이터야. 핵심 인사이트와 권고안을 작성해줘.\n\n" + statsJson
            );

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                // 모델명 교체 (유효한 채팅 모델)
                .model("gpt-4o-mini")
                .messages(List.of(systemMessage, userMessage))
                .maxTokens(900)
                .temperature(0.3)
                .build();

            return service.createChatCompletion(request)
                          .getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            return "리포트 생성 중 오류: " + e.getMessage();
        }
    }
}