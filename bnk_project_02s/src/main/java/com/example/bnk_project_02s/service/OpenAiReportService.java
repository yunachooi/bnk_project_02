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
        		    String.join("\n",
        		        "당신은 한국어를 사용하는 시니어 프로덕트 애널리스트다.",
        		        "아래 JSON 대시보드 원시 데이터를 근거로 경영진용 심화 분석 리포트를 작성하라.",

        		        "형식 규칙:",
        		        "1) 반드시 아래 섹션 제목을 H4 헤딩(#### )으로, 이 순서 그대로 출력한다.",
        		        "   - #### 가입자 추이",
        		        "   - #### 연령/성별 인사이트",
        		        "   - #### 리뷰/평점 변화",
        		        "   - #### 키워드 요약",
        		        "   - #### 누적 원화 사용액",
        		        "   - #### 공유 클릭",
        		        "   - #### 다음 액션",
        		        "2) 표/코드블록/굵게(**)/기울임(*) 같은 마크다운 스타일은 쓰지 말 것. 불릿(-)만 사용.",
        		        "3) 수치 제시는 가능한 한 원시 수치와 증감(%, 증감폭)을 함께 표기하라. (예: 60→80, +33.3%)",

        		        "분량 가이드:",
        		        "- 전체 분량: 최소 1,600~2,200자(한글, A4 1쪽 이상).",
        		        "- 각 섹션은 최소 5~8개 불릿, 각 불릿은 1~2문장으로 작성.",

        		        "분석 지시(섹션별 디테일):",
        		        "- 가입자 추이: 일/월/분기 DoD·MoM·QoQ 변화율 계산, 급증/급감(±10%↑↓) 탐지, 원인 가설(프로모션/이벤트/시즌성/가격/제품변경)과 리스크.",
        		        "- 연령/성별: 상위/하위 연령대와 성별 비중, 전월 대비 변화, 유의미 조합(예: 20대 남성)과 타겟별 가설/액션.",
        		        "- 리뷰/평점: 최근 3개월 리뷰수↔평점 상관/비상관, 대표 긍/부 키 문장 요약, 개선 포인트(UX/안정성/정책).",
        		        "- 키워드 요약: 긍정/부정 상위 3개씩과 시사점(제품, CX, 마케팅 관점).",
        		        "- 누적 원화 사용액: 분기 흐름(억 단위 환산), 전환점(가속/둔화), 파이프라인 시사점.",
        		        "- 공유 클릭: 최근 7일 최고/최저, 전일 대비, 클릭→가입 전환에 대한 가설 및 실험안.",
        		        "- 다음 액션: 임팩트/노력 기준으로 Quick win 1개, 단기 1개(≤4주), 중기 1개(≤1~2분기).",

        		        "표현 지시:",
        		        "- 군더더기 없이 데이터 근거 중심으로 서술하되, 해석/가설/리스크/우선순위를 명확히 제시하라.",
        		        "- 데이터가 부족한 경우 '데이터 없음'이라고 명시하고 대안 데이터/실험을 제안하라."
        		    )
        		);
        		ChatMessage userMessage = new ChatMessage(
        		    "user",
        		    "아래는 대시보드 원시 데이터(JSON)입니다. 위 형식과 분량 지침에 맞춰 심화 분석 리포트를 작성해 주세요.\n\n" + statsJson
        		);

        		ChatCompletionRequest request = ChatCompletionRequest.builder()
        			    .model("gpt-4o-mini")
        			    .messages(List.of(systemMessage, userMessage))
        			    .maxTokens(2200)        // A4 1~1.5쪽 분량 확보
        			    .temperature(0.2)       // 수치/일관성 위주
        			    .build();
        		
            return service.createChatCompletion(request)
                          .getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            return "리포트 생성 중 오류: " + e.getMessage();
        }
    }
}