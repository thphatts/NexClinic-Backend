package com.thphatts.clinicportal.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiLlmProvider implements LlmProvider {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Value("${app.ai.openai.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String generateText(String prompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("OpenAI API Key chưa được cấu hình!");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                            Map.of("role", "system", "content", "Bạn là Trợ lý AI Y tế của Phòng khám AI-Powered Clinic Portal."),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(OPENAI_API_URL, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                List choices = (List) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map messageMap = (Map) firstChoice.get("message");
                    if (messageMap != null) {
                        return (String) messageMap.get("content");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi kết nối OpenAI API: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(apiKey) && !apiKey.equalsIgnoreCase("mock-key");
    }

    @Override
    public String getProviderName() {
        return "openai";
    }
}
