package com.thphatts.clinicportal.service.ai.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory chọn Embedding Provider phù hợp (Gemini hoặc TF-IDF Fallback).
 * Tự động chọn Gemini nếu API Key hợp lệ, fallback sang TF-IDF nếu không có mạng/key.
 */
@Slf4j
@Component
public class EmbeddingProviderFactory {

    @Value("${app.ai.embedding.provider:auto}")
    private String providerSetting;

    private final GeminiEmbeddingService geminiEmbeddingService;
    private final TfIdfFallbackEmbeddingService tfIdfFallbackService;

    public EmbeddingProviderFactory(GeminiEmbeddingService geminiEmbeddingService,
                                    TfIdfFallbackEmbeddingService tfIdfFallbackService) {
        this.geminiEmbeddingService = geminiEmbeddingService;
        this.tfIdfFallbackService = tfIdfFallbackService;
    }

    public EmbeddingService getProvider() {
        String target = providerSetting.trim().toLowerCase();

        if ("tfidf".equals(target) || "offline".equals(target)) {
            log.info("[Embedding] Chế độ offline TF-IDF được chọn thủ công.");
            return tfIdfFallbackService;
        }

        // Auto mode: ưu tiên Gemini nếu có API Key
        if (geminiEmbeddingService.isAvailable()) {
            log.info("[Embedding] Kích hoạt Gemini text-embedding-004 (768 chiều).");
            return geminiEmbeddingService;
        }

        log.warn("[Embedding] Gemini API Key không hợp lệ. Chuyển sang TF-IDF Offline Fallback.");
        return tfIdfFallbackService;
    }
}
