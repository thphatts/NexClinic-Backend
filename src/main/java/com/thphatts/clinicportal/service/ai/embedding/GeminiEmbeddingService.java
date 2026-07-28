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

    private static final String GEMINI_EMBED_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/%s:embedContent?key=%s";

    private static final List<String> MODEL_CANDIDATES = List.of(
            "models/text-embedding-004",
            "models/embedding-001"
    );

    @Value("${app.ai.google.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private volatile String workingModel = null;

    @Override
    public float[] embed(String text) {
        if (!isAvailable()) {
            throw new IllegalStateException("Gemini Embedding API Key chưa được cấu hình!");
        }

        List<String> modelsToTry = (workingModel != null)
                ? List.of(workingModel)
                : MODEL_CANDIDATES;

        for (String model : modelsToTry) {
            try {
                String url = String.format(GEMINI_EMBED_URL_TEMPLATE, model, apiKey);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> requestBody = Map.of(
                        "model", model,
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
                            if (this.workingModel == null) {
                                this.workingModel = model;
                                log.info("✅ Đã xác nhận Gemini Embedding Model hoạt động: {}", model);
                            }
                            log.debug("✅ Gemini Embedding thành công ({}) cho text dài {} ký tự", model, text.length());
                            return result;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Model Gemini {} không khả dụng: {}", model, e.getMessage());
            }
        }

        throw new RuntimeException("Tất cả Gemini Embedding models đều không khả dụng cho API Key hiện tại.");
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
