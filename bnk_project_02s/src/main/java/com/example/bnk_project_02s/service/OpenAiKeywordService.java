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
            if (joined.length() > 6000) joined = joined.substring(0, 6000);

            ChatMessage system = new ChatMessage("system",
                "너는 한국어 리뷰를 분석하는 도우미다. 반드시 JSON만 출력한다. " +
                "절차: (1) 문장을 양/음/중립으로 분류한다. (2) 각 문장에서 해당 감성의 서술어 위주의 핵심 키워드만 뽑아 합산한다. " +
                "부정 예시: 불편, 느리다, 오류, 버그, 지연, 아쉽다, 개선, 문제, 끊김, 부정확, 대기 시간, 떨어진다 등. " +
                "긍정 예시: 좋다, 만족, 추천, 괜찮다, 신뢰, 편리, 빠르다, 안정, 혜택, 친절 등. " +
                "반드시 JSON만 출력한다. ... " +
                "형식: {\"positive\":[{\"name\":\"…\",\"value\":n}], \"negative\":[…]} " +
                "각 배열은 **최대 15개**까지만, 추출 불가하면 비워라."
            );
            ChatMessage user = new ChatMessage("user",
                "리뷰 텍스트(개행 구분):\n" + joined + "\n\n반드시 JSON만 출력하세요."
            );

            var req = ChatCompletionRequest.builder()
                    .model(model).messages(List.of(system, user))
                    .temperature(0.2).maxTokens(800).build();

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

    /* -------- JSON 정리/복구 -------- */
    private static String stripToJson(String content) {
        if (content == null) return "{}";
        String s = content.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("(?s)^```(?:json)?\\s*", "").replaceFirst("(?s)\\s*```\\s*$", "");
        }
        int i = s.indexOf('{'), j = s.lastIndexOf('}');
        if (i >= 0 && j > i) s = s.substring(i, j + 1);
        return s.trim();
    }

    private static String repairJson(String s) {
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
