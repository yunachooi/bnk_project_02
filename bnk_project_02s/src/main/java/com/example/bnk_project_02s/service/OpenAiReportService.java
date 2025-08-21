package com.example.bnk_project_02s.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

@Service
public class OpenAiReportService {

    private final OpenAiService service;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

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

        		        // ===== 형식 규칙 =====
        		        "형식 규칙:",
        		        "1) 반드시 아래 섹션 제목을 H4 헤딩(#### )으로, 이 순서 그대로 출력한다.",
        		        "   - #### 가입자 추이",
        		        "   - #### 연령/성별 인사이트",
        		        "   - #### 리뷰/평점 변화",
        		        "   - #### 키워드 요약",
        		        "   - #### 누적 원화 사용액",
        		        "   - #### 환전/사용액 추이",        // ⬅️ 신규 섹션
        		        "   - #### 관심 통화/관심 분야",
        		        "   - #### 다음 액션",
        		        "2) 표/코드블록/굵게(**)/기울임(*) 같은 마크다운은 쓰지 말고, 불릿(-)만 사용한다.",
        		        "3) 각 섹션은 4~7개 불릿으로 간결하게 작성한다.",
        		        "4) 데이터가 없거나 계산 불가하면 '데이터 없음'이라고 명시한다.",

        		        // ===== 분석 지시 =====
        		        "분석 지시(섹션별 디테일):",
        		        "- 가입자 추이: 최근 일/월/분기 변화율(DoD, MoM, QoQ) 계산, 급증/급감(±10% 이상) 탐지, 원인 가설(프로모션/이벤트/시즌성/가격/제품변경)과 리스크.",
        		        "- 연령/성별 인사이트: 상위/하위 연령대, 성별 분포 및 전월 대비 변화, 유의미 조합(예: 20대 남성)과 목표 액션 제안.",
        		        "- 리뷰/평점 변화: 최근 3개월 리뷰수↔평점의 동행/역행 여부, 대표 긍/부 포인트 요약(UX/안정성/정책 관점).",
        		        "- 키워드 요약: positive/negative 각각 상위 3개 키워드와 시사점(제품, CX, 마케팅 관점).",
        		        "- 누적 원화 사용액: 총액(statsJson.usage.krwTotal)을 제시하고 의미/전년·전월 대비 코멘트.",
        		        "- 환전/사용액 추이:",                                       // ⬅️ 신규 지시
        		        "    • 일자별 원화사용액(statsJson.usage.krwDaily)의 추이에서 급등/급락일, 평균 대비 편차, 주중/주말 패턴을 도출하라.",
        		        "    • 일자별 통화 환전금액(statsJson.usage.fxDaily: labels, series[{name,data}])을 사용해 상위 통화, 점유율 변화, 변동성(표준편차/최대·최소)을 서술하라.",
        		        "    • krwDaily와 fxDaily 사이 상관 신호(예: JPY 급증일에 원화사용액도 동반 상승)를 탐지하고, 비즈니스 맥락 가설을 제시하라.",
        		        "- 관심 통화/관심 분야:",
        		        "    • 통화 분포는 USD, JPY, CNH, EUR, CHF, VND 키만 고려해 상위 3개와 비중을 제시한다.",
        		        "    • 관심분야 분포는 TRAVEL, STUDY, SHOPPING, FINANCE, ETC 키만 고려해 상위 3개와 시사점을 제시한다.",
        		        "    • 가능하면 두 지표 간 교차 가설(예: TRAVEL 이용자는 JPY/USD 선호)을 제안한다.",
        		        "    • 데이터 구조 힌트: statsJson.affinity.currency 는 {USD:103,...}, statsJson.affinity.topicMap 은 {TRAVEL:120,...} 형태다.",
        		        "- 다음 액션: 임팩트/노력 기준으로 Quick win 1개, 단기(≤4주) 1개, 중기(≤1~2분기) 1개 제시.",

        		        // ===== 표현 지시 =====
        		        "표현 지시:",
        		        "- 수치 제시는 가능한 한 원시 수치와 증감(%, 증감폭)을 함께 표기하라. (예: 60→80, +33.3%)",
        		        "- 군더더기 없이 데이터 근거 중심으로 서술하되, 해석/가설/리스크/우선순위를 명확히 제시하라.",
        		        "- A4용지 한 바닥 이상의 분량을 고려하여 자세하기 분석 내용을 표시하라.",
        		        "- statsJson에 존재하지 않는 지표는 추정하지 말고 대안 데이터/실험을 제안하라.",

        		        // ===== 데이터 구조 힌트(신규 필드 포함) =====
        		        "데이터 구조 힌트:",
        		        "- statsJson.usage.krwTotal: 숫자(총 원화 누적 사용액).",
        		        "- statsJson.usage.krwDaily: {labels:[yyyy-MM-dd...], data:[원화 사용액...]}.",
        		        "- statsJson.usage.fxDaily: {labels:[yyyy-MM-dd...], series:[{name:'USD', data:[외화금액...]}, ...]}."
        		    )
        		);

            ChatMessage userMessage = new ChatMessage(
                "user",
                String.join("\n",
                    "아래는 대시보드 원시 데이터(JSON)입니다.",
                    "위 형식과 분량 지침에 맞춰 심화 분석 리포트를 작성해 주세요.",
                    "",
                    statsJson
                )
            );

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(systemMessage, userMessage))
                .maxTokens(1800)   // 리포트 길이(불릿 위주) 확보
                .temperature(0.2)  // 수치/일관성 위주
                .build();

            return service.createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent();

        } catch (Exception e) {
            return "리포트 생성 중 오류: " + e.getMessage();
        }
    }
}
