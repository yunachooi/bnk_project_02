package com.example.bnk_project_02s.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.bnk_project_02s.dto.KeywordItem;   // record KeywordItem(String name, int value)
import com.example.bnk_project_02s.dto.KeywordResult; // record KeywordResult(List<KeywordItem> positive, List<KeywordItem> negative)
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
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

    private static final Logger log = LoggerFactory.getLogger(OpenAiKeywordService.class);

    private static final ObjectMapper OM = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

    private static final Pattern POS_PAT = Pattern.compile("(좋|만족|추천|괜찮|신뢰|편리|빠르|안정|혜택|친절|유용|감사|훌륭|만점|잘됨|깔끔)");
    private static final Pattern NEG_PAT = Pattern.compile("(불편|느리|오류|버그|지연|아쉽|개선|문제|끊김|부정확|떨어진다|대기\\s*시간|불만|짜증|불친절|불만족)");

    /* === 추가: 개수 고정용 설정/시드 === */
    private static final int TARGET_K = 15;

    private static final List<String> POS_SEED = List.of(
        "좋다","만족","추천","괜찮다","신뢰","편리","빠르다","안정",
        "혜택","친절","유용","감사","훌륭","만점","깔끔","잘됨"
    );

    private static final List<String> NEG_SEED = List.of(
        "불편","느리다","오류","버그","지연","아쉽다","개선","문제",
        "끊김","부정확","대기 시간","불만","짜증","불친절","불만족","느림"
    );

    public KeywordResult extract(List<String> texts) {
        try {
            String joined = String.join("\n", texts == null ? List.of() : texts);
            if (joined.length() > 6000) joined = joined.substring(0, 6000); //최대 6000자 까지

            ChatMessage system = new ChatMessage("system",  //모델의 “역할·원칙·출력 형식”을 강하게 못 박는 상위 규칙. system은 규칙서
                "너는 한국어 리뷰를 분석하는 도우미다. 반드시 JSON만 출력한다. " +
                "절차: (1) 문장을 양/음/중립으로 분류한다. (2) 각 문장에서 해당 감성의 서술어 위주의 핵심 키워드만 뽑아 합산한다. " +
                "부정 예시: 불편, 느리다, 오류, 버그, 지연, 아쉽다, 개선, 문제, 끊김, 부정확, 대기 시간, 떨어진다 등. " +
                "긍정 예시: 좋다, 만족, 추천, 괜찮다, 신뢰, 편리, 빠르다, 안정, 혜택, 친절 등. " +
                "반드시 JSON만 출력한다. ... " +
                "형식: {\"positive\":[{\"name\":\"…\",\"value\":n}], \"negative\":[…]} " +
                "각 배열은 **최대 15개**까지만, 추출 불가하면 비워라."
            );
            ChatMessage user = new ChatMessage("user", //실제 분석 데이터와 요청을 전달, user는 실제 문제지  user와 system이 충돌하게 된다면 보통 system쪽이 우선임
                "리뷰 텍스트(개행 구분):\n" + joined + "\n\n반드시 JSON만 출력하세요."
            );

            var req = ChatCompletionRequest.builder()
                    .model(model).messages(List.of(system, user))
                    .temperature(0.2).maxTokens(800).build(); //tempreature: 창의성 조절 (낮을수록 예측가능, 높을수록 창의적), max_tokens : 응답길이 제한 (약 1토큰 : 4자)

            var resp = openAi.createChatCompletion(req);
            String content = resp.getChoices().get(0).getMessage().getContent();
            if (content == null) return new KeywordResult(List.of(), List.of());

            String json = repairJson(stripToJson(content));
            JsonNode root = OM.readTree(json);

            List<KeywordItem> pos = readArray(root, "positive");
            List<KeywordItem> neg = readArray(root, "negative");

            return polarityGuard(new KeywordResult(pos, neg), texts != null && texts.size() >= 10);

        } catch (Exception e) {
            log.warn("[keywords] OpenAI/파싱 실패: {}", e.toString());
            return new KeywordResult(List.of(), List.of());
        }
    }
    //모델이 늘 완벽한 JSON을 주는것은 아니고 코드패스(````)이 섞이거나, 앞뒤로 설명이 붙거나, 중괄호 대괄호가 어긋나거나 트레일링 콤마 같은 자잘한 문법오류가 섞이기도 함.
    /* -------- JSON 정리/복구 -------- */
	//예시 : (분석완료) { "positive":[], "negative":[{"name":"느리다","value":2},] } 	-- 끝 -- >> { "positive":[], "negative":[{"name":"느리다","value":2},] }
    private static String stripToJson(String content) { //...같은 코드펜스 제거 , 선/후행 여분 텍스트 제거
        if (content == null) return "{}";
        String s = content.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("(?s)^```(?:json)?\\s*", "").replaceFirst("(?s)\\s*```\\s*$", "");
        }
        int i = s.indexOf('{'), j = s.lastIndexOf('}');
        if (i >= 0 && j > i) s = s.substring(i, j + 1);
        return s.trim();
    }

    private static String repairJson(String s) { //트레일링 콤마제거, 괄호 문자열 스택기반 보정, 따옴표 짝 안맞으면 보충
    	//예시 : {"positive":[{"name":"친절","value":3},], "negative":[{"name":"느리다","value":2},]} >> {"positive":[{"name":"친절","value":3}], "negative":[{"name":"느리다","value":2}]}
        if (s == null || s.isBlank()) return "{}";
        String r = s.replaceAll(",\\s*([}\\]])", "$1");
        StringBuilder out = new StringBuilder();
        Deque<Character> stack = new ArrayDeque<>();
        boolean inStr = false; char prev = 0;
        for (int i=0;i<r.length();i++){
            char c=r.charAt(i); out.append(c);
            if (c=='"' && prev!='\\') inStr=!inStr;
            if (!inStr){
                if (c=='{'||c=='[') stack.push(c);
                else if (c=='}' && !stack.isEmpty() && stack.peek()=='{') stack.pop();
                else if (c==']' && !stack.isEmpty() && stack.peek()=='[') stack.pop();
            }
            prev=c;
        }
        while(!stack.isEmpty()) out.append(stack.pop()=='{'?'}':']');
        if (inStr) out.append('"');
        return out.toString().replaceAll(",\\s*([}\\]])", "$1");
    }

    private static List<KeywordItem> readArray(JsonNode root, String field){
    	//LLM이 준 JSON에서 field(예: "positive", "negative") 배열을 꺼내 키워드 이름/빈도를 뽑아 List<KeywordItem>로 만듬.
    	//name|keyword|word|text} 중 아무거나 이름으로, {value|count}는 값으로 인식.
    	//배열 요소가 문자열 한 줄(예: "만족")이면 값 1로 처리, 값이 0 또는 음수여도 최소 1로 보정.
    	/* 예시 : {
    	  "positive": [
    	               {"name": "친절", "value": 3},
    	               {"keyword": "빠르다", "count": 2},
    	               "만족"
    	             ],
    	             "negative": [
    	               {"word": "느리다", "value": 0},
    	               {"text": "오류", "count": -5}
    	             ]
    	           } >>> [ ("친절",3), ("빠르다",2), ("만족",1) ] / [ ("느리다",1), ("오류",1) ] */ 
        List<KeywordItem> out = new ArrayList<>();
        JsonNode arr = root.path(field);
        if (!arr.isArray()) return out;
        for (JsonNode n : arr) {
            if (n.isTextual()) { out.add(new KeywordItem(n.asText(), 1)); continue; }
            String name = getStr(n, "name","keyword","word","text");
            int val = getInt(n, "value","count");
            if (name != null && !name.isBlank()) out.add(new KeywordItem(name, Math.max(1, val == 0 ? 1 : val)));
        }
        return out;
    }
    private static String getStr(JsonNode n, String... keys){ for(String k:keys){var v=n.get(k); if(v!=null && !v.isNull()) return v.asText();} return null; }
    private static int getInt(JsonNode n, String... keys){ for(String k:keys){var v=n.get(k); if(v!=null && v.isNumber()) return v.asInt();} return 0; }

    /* -------- 병합/정렬/상위K + 시드 패딩 -------- */
    private static List<KeywordItem> mergeSortLimitPad(
    		/* 
    		 src의 동일 키워드 이름을 합산(trim, 빈/널 필터, 최소 1 보장).
			 내림차순 정렬 → 상위 k개로 자르기.
			 padWhenNonEmpty==true이고 src가 비어있지 않으면, 부족한 개수만큼 seed 목록으로 채움(중복 제외). src 자체가 빈 리스트라면 패딩하지 않음.
			 예시 : src = [("친절",3), ("빠르다",2), ("만족",1), ("친절",4)]
			 seed = ["신뢰","안정","혜택"]
			 k = 5
			 padWhenNonEmpty = true
			 결과:
			 중복 합산: 친절(3+4)=7, 빠르다(2), 만족(1) → [(친절,7),(빠르다,2),(만족,1)]
			 상위 5: 현재 3개뿐 → 부족 2개를 시드로 채움(중복 아닌 것부터): 신뢰, 안정
			 결과: [(친절,7),(빠르다,2),(만족,1),(신뢰,1),(안정,1)]
    		 */
    	    List<KeywordItem> src, List<String> seed, int k, boolean padWhenNonEmpty) {

    	    LinkedHashMap<String, Integer> map = src.stream()
    	        .filter(it -> it != null && it.name() != null && !it.name().isBlank())
    	        .collect(Collectors.toMap(
    	            it -> it.name().trim(),
    	            it -> Math.max(1, it.value()),
    	            Integer::sum,
    	            LinkedHashMap::new
    	        ));

    	    List<KeywordItem> out = map.entrySet().stream()
    	        .map(e -> new KeywordItem(e.getKey(), e.getValue()))
    	        .sorted((a,b) -> Integer.compare(b.value(), a.value()))
    	        .limit(k)
    	        .collect(Collectors.toCollection(ArrayList::new));

    	    // ★ src가 비었으면 패딩하지 않음
    	    if (!padWhenNonEmpty || src.isEmpty()) return out;

    	    // 부족분을 시드로만 채움(원할 때만)
    	    Set<String> existing = out.stream().map(KeywordItem::name).collect(Collectors.toSet());
    	    for (String s : seed) {
    	        if (out.size() >= k) break;
    	        if (!existing.contains(s)) out.add(new KeywordItem(s, 1));
    	    }
    	    return out;
    	}

    /* -------- 폴라리티 가드(오분류 교정 + 15개 고정) -------- */
    private KeywordResult polarityGuard(KeywordResult in, boolean allowPad){
    	/*
    	처음 LLM이 분류한 in.positive, in.negative에서 정규식 패턴으로 단어를 검사:
		긍정 리스트에 있는데 부정 패턴(예: 느리, 오류, 지연 …)이 매치되면 부정으로 이동
		부정 리스트에 있는데 긍정 패턴(예: 좋, 만족, 추천, 친절 …)이 매치되면 긍정으로 이동
		교정된 두 리스트에 대해 mergeSortLimitPad(..., TARGET_K=15, allowPad) 실행 → 상위 15개 고정 + 원할 때만 시드 패딩. 이동 건수는 로그로 남김. 
    	 */
        if (in == null) return new KeywordResult(List.of(), List.of());
        List<KeywordItem> posOut = new ArrayList<>(), negOut = new ArrayList<>();
        int toPos=0, toNeg=0;

        if (in.positive()!=null) for (KeywordItem k : in.positive()){
            if (k==null || k.name()==null) continue;
            if (NEG_PAT.matcher(k.name()).find()){ negOut.add(k); toNeg++; } else posOut.add(k);
        }
        if (in.negative()!=null) for (KeywordItem k : in.negative()){
            if (k==null || k.name()==null) continue;
            if (POS_PAT.matcher(k.name()).find()){ posOut.add(k); toPos++; } else negOut.add(k);
        }

        var posFixed = mergeSortLimitPad(posOut, POS_SEED, TARGET_K, allowPad);
        var negFixed = mergeSortLimitPad(negOut, NEG_SEED, TARGET_K, allowPad);

        log.info("[keywords] polarityGuard moved -> toPos={}, toNeg={}, pos={}, neg={}",
                 toPos, toNeg, posFixed.size(), negFixed.size());
        return new KeywordResult(posFixed, negFixed);
    }
}
