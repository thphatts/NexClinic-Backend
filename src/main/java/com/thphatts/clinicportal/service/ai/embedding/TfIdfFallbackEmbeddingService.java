package com.thphatts.clinicportal.service.ai.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TF-IDF Fallback Embedding Service - Hoàn toàn Offline, không cần API Key.
 * Tính toán Cosine Similarity dựa trên tần suất từ khóa (TF-IDF approximation).
 * Dùng khi không có Gemini API Key hoặc môi trường Dev/Test không có mạng.
 *
 * Dimension: 512 (compact TF-IDF feature vector)
 */
@Slf4j
@Component
public class TfIdfFallbackEmbeddingService implements EmbeddingService {

    private static final int VECTOR_DIM = 768;

    // Từ khóa y tế đặc biệt quan trọng - tăng trọng số
    private static final Set<String> MEDICAL_KEYWORDS = Set.of(
            "tim", "mạch", "hô", "hấp", "thần", "kinh", "nội", "tổng", "quát",
            "nhi", "sản", "khoa", "bác", "sĩ", "thuốc", "dược", "phẩm",
            "đặt", "lịch", "hẹn", "khám", "chữa", "bệnh", "triệu", "chứng",
            "chẩn", "đoán", "phòng", "giờ", "làm", "việc", "hotline"
    );

    @Override
    public float[] embed(String text) {
        float[] vector = new float[VECTOR_DIM];
        if (text == null || text.isBlank()) {
            return vector;
        }

        // Normalize & tokenize
        String normalized = text.toLowerCase()
                .replaceAll("[^a-zA-ZÀ-ỹ\\s]", " ")
                .trim();
        String[] tokens = normalized.split("\\s+");

        // Compute TF (term frequency with medical keyword boost)
        Map<String, Double> tf = new HashMap<>();
        for (String token : tokens) {
            if (token.length() < 2) continue;
            double weight = MEDICAL_KEYWORDS.contains(token) ? 2.5 : 1.0;
            tf.merge(token, weight, Double::sum);
        }

        // Map tokens to vector dimensions via consistent hashing
        for (Map.Entry<String, Double> entry : tf.entrySet()) {
            int idx = Math.abs(entry.getKey().hashCode()) % VECTOR_DIM;
            // Bigram feature: combine consecutive chars for richer representation
            int idx2 = Math.abs((entry.getKey() + "_v2").hashCode()) % VECTOR_DIM;
            vector[idx] += entry.getValue().floatValue();
            vector[idx2] += entry.getValue().floatValue() * 0.5f;
        }

        // L2 Normalize to unit vector (required for Cosine Similarity)
        double norm = 0.0;
        for (float v : vector) norm += v * v;
        if (norm > 0) {
            norm = Math.sqrt(norm);
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= (float) norm;
            }
        }

        log.debug("📐 TF-IDF Fallback Embedding: {} chiều cho text dài {} ký tự", VECTOR_DIM, text.length());
        return vector;
    }

    @Override
    public boolean isAvailable() {
        return true; // Luôn online, không cần API
    }

    @Override
    public String getProviderName() {
        return "tfidf-fallback";
    }
}
