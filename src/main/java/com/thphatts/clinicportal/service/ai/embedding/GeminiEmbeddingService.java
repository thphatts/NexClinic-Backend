package com.thphatts.clinicportal.service.ai.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Gemini Embedding Service - Sử dụng Google Gemini text-embedding-004 (768 chiều).
 * Miễn phí với quota cao, tận dụng cùng GEMINI_API_KEY đã cấu hình.
 */
@Slf4j
@Component
public class GeminiEmbeddingService implements EmbeddingService {

    private static final String GEMINI_EMBED_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=%s";

    @Value("${app.ai.google.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public float[] embed(String text) {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini Embedding API Key chưa được cấu hình!");
        }

        try {
            String url = String.format(GEMINI_EMBED_URL, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "model", "models/text-embedding-004",
                    "content", Map.of(
                            "parts", List.of(
                                    Map.of("text", text)
                            )
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map embedding = (Map) response.getBody().get("embedding");
                if (embedding != null) {
                    List<Double> values = (List<Double>) embedding.get("values");
                    if (values != null && !values.isEmpty()) {
                        float[] result = new float[values.size()];
                        for (int i = 0; i < values.size(); i++) {
                            result[i] = values.get(i).floatValue();
                        }
                        log.debug("✅ Gemini Embedding thành công: {} chiều cho text dài {} ký tự", result.length, text.length());
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi Gemini Embedding API: {}", e.getMessage());
        }

        throw new RuntimeException("Không thể tạo Embedding vector từ Gemini API.");
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(apiKey) && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }
}
