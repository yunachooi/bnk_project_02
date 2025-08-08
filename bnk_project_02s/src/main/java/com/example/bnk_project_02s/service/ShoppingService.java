package com.example.bnk_project_02s.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.bnk_project_02s.dto.ShoppingProductDto;
import com.example.bnk_project_02s.entity.ShoppingProduct;
import com.example.bnk_project_02s.repository.ShoppingProductRepository;
import com.example.bnk_project_02s.util.ShoppingProductConveter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
public class ShoppingService {

    @Autowired
    private ShoppingProductRepository shoppingProductRepository;

    @Value("${amazon.api.key}")
    private String apiKey;

    @Value("${amazon.api.host}")
    private String apiHost;

    @Value("${amazon.api.url}")
    private String apiUrl;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ShoppingProductDto> getProductList() {
        List<ShoppingProduct> entities = shoppingProductRepository.findAll();
        return entities.stream()
                .map(ShoppingProductConveter::toDto)
                .collect(Collectors.toList());
    }

    public ShoppingProductDto getOrFetchProduct(String spno) {
        try {
            System.out.println("DB에서 제품 조회 시도: " + spno);
            return getProductFromDb(spno);
        } catch (Exception e) {
            System.out.println("DB에 없음, API에서 가져와서 저장: " + e.getMessage());
            return fetchAndSaveProduct(spno);
        }
    }

    public ShoppingProductDto getProductFromDb(String spno) {
        ShoppingProduct entity = shoppingProductRepository.findById(spno)
                .orElseThrow(() -> new RuntimeException("제품을 찾을 수 없습니다: " + spno));
        
        return ShoppingProductConveter.toDto(entity);
    }

    public ShoppingProductDto fetchAndSaveProduct(String spno) {
        ShoppingProductDto dto = fetchProduct(spno);
        return saveProduct(dto);
    }

    public ShoppingProductDto fetchProduct(String spno) {
        String url = "https://real-time-amazon-data.p.rapidapi.com/product-details?asin=" + spno + "&country=US";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-key", apiKey)
                .addHeader("x-rapidapi-host", "real-time-amazon-data.p.rapidapi.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("API 호출 실패: HTTP " + response.code());
            }

            String json = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(json);
            
            JsonNode root = null;
            if (jsonResponse.has("data")) {
                root = jsonResponse.get("data");
            } else if (jsonResponse.has("product")) {
                root = jsonResponse.get("product");
            } else if (jsonResponse.isObject()) {
                root = jsonResponse;
            }

            if (root == null || root.isNull()) {
                throw new RuntimeException("API 응답에서 제품 데이터를 찾을 수 없습니다");
            }

            ShoppingProductDto dto = new ShoppingProductDto();
            dto.setSpno(getTextValue(root, "asin", spno));
            dto.setSpname(getTextValue(root, "product_title", "제품명 없음"));
            dto.setSpdescription(getTextValue(root, "product_description", "설명 없음"));
            dto.setSpprice(getDoubleValue(root, "product_price", 0.0));
            dto.setSpcurrency(getTextValue(root, "currency", "USD"));
            dto.setSprating(getDoubleValue(root, "product_star_rating", 0.0));
            dto.setSpreviews(getIntValue(root, "product_num_ratings", 0));
            dto.setSpurl(getTextValue(root, "product_photo", ""));

            return dto;
            
        } catch (Exception e) {
            throw new RuntimeException("API 호출 또는 파싱 실패: " + e.getMessage(), e);
        }
    }

    public ShoppingProductDto saveProduct(ShoppingProductDto dto) {
        try {
            ShoppingProduct entity = ShoppingProductConveter.toEntity(dto);
            
            entity.setSpname(truncateString(dto.getSpname(), 1000));
            entity.setSpdescription(truncateString(dto.getSpdescription(), 2000));
            entity.setSpurl(truncateString(dto.getSpurl(), 2000));

            System.out.println("저장 전 Entity: " + entity);
            ShoppingProduct savedEntity = shoppingProductRepository.save(entity);
            System.out.println("저장 후 Entity: " + savedEntity);
            
            return ShoppingProductConveter.toDto(savedEntity);
        } catch (Exception e) {
            System.err.println("DB 저장 중 오류: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("DB 저장 실패: " + e.getMessage(), e);
        }
    }

    public String getProductDetails(String spno) throws Exception {
        String fullUrl = apiUrl + "?asin=" + spno + "&country=US";

        Request request = new Request.Builder()
                .url(fullUrl)
                .get()
                .addHeader("x-rapidapi-key", apiKey)
                .addHeader("x-rapidapi-host", apiHost)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("API 호출 실패: " + response.code());
            }
            return response.body().string();
        }
    }

    private String getTextValue(JsonNode node, String fieldName, String defaultValue) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asText(defaultValue);
    }

    private Double getDoubleValue(JsonNode node, String fieldName, Double defaultValue) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return defaultValue;
        }
        
        if (fieldNode.isTextual()) {
            String text = fieldNode.asText().replaceAll("[^0-9.]", "");
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        return fieldNode.asDouble(defaultValue);
    }

    private Integer getIntValue(JsonNode node, String fieldName, Integer defaultValue) {
        JsonNode fieldNode = node.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return defaultValue;
        }
        
        if (fieldNode.isTextual()) {
            String text = fieldNode.asText().replaceAll("[^0-9]", "");
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        return fieldNode.asInt(defaultValue);
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}