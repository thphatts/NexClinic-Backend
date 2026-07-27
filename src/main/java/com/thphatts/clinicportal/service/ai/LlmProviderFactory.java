package com.thphatts.clinicportal.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LlmProviderFactory {

    @Value("${app.ai.active-provider:auto}")
    private String activeProviderSetting;

    private final Map<String, LlmProvider> providerMap;
    private final FallbackClinicalLlmProvider fallbackProvider;

    public LlmProviderFactory(List<LlmProvider> providers, FallbackClinicalLlmProvider fallbackProvider) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(LlmProvider::getProviderName, Function.identity()));
        this.fallbackProvider = fallbackProvider;
    }

    public LlmProvider getProvider() {
        String targetProvider = activeProviderSetting.trim().toLowerCase();

        if ("gemini".equals(targetProvider)) {
            LlmProvider gemini = providerMap.get("gemini");
            if (gemini != null && gemini.isAvailable()) {
                return gemini;
            }
            log.warn("Cấu hình chọn Gemini nhưng API Key không hợp lệ. Chuyển sang mô hình dự phòng.");
        } else if ("openai".equals(targetProvider)) {
            LlmProvider openai = providerMap.get("openai");
            if (openai != null && openai.isAvailable()) {
                return openai;
            }
            log.warn("Cấu hình chọn OpenAI nhưng API Key không hợp lệ. Chuyển sang mô hình dự phòng.");
        }

        LlmProvider gemini = providerMap.get("gemini");
        if (gemini != null && gemini.isAvailable()) {
            log.info("Kích hoạt thành công LLM Provider: Google Gemini 1.5 Flash");
            return gemini;
        }

        LlmProvider openai = providerMap.get("openai");
        if (openai != null && openai.isAvailable()) {
            log.info("Kích hoạt thành công LLM Provider: OpenAI GPT");
            return openai;
        }

        log.info("Kích hoạt LLM Provider: Clinical Rule Engine Fallback (Offline Mode)");
        return fallbackProvider;
    }
}
