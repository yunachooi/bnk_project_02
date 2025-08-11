package com.example.bnk_project_02s.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class TranslationService {
    
    @Value("${google.translate.api.key}")
    private String apiKey;
    
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public String translateText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        if (isKoreanText(text)) {
            return text;
        }
        
        try {
            String url = "https://translation.googleapis.com/language/translate/v2?key=" + apiKey;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("q", text);
            requestBody.put("source", "en");
            requestBody.put("target", "ko");
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            RequestBody body = RequestBody.create(
                jsonBody, 
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    
                    JsonNode translations = jsonResponse.path("data").path("translations");
                    if (translations.isArray() && translations.size() > 0) {
                        String translatedText = translations.get(0).path("translatedText").asText();
                        System.out.println("번역 성공: " + text + " -> " + translatedText);
                        return translatedText;
                    }
                }
                System.out.println("번역 API 응답 오류: " + response.code());
            }
        } catch (Exception e) {
            System.err.println("번역 실패: " + e.getMessage());
        }
        
        return text;
    }
    
    private boolean isKoreanText(String text) {
        return text.matches(".*[ㄱ-ㅎ가-힣]+.*");
    }
}