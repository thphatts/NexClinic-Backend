package com.thphatts.clinicportal.service.ai.embedding;

/**
 * Port/Interface cho Embedding Service.
 * Triển khai thực tế: GeminiEmbeddingService (online) hoặc TfIdfFallbackEmbeddingService (offline).
 */
public interface EmbeddingService {

    /**
     * Chuyển văn bản thành vector embedding dạng float[]
     * @param text Văn bản đầu vào (query hoặc knowledge content)
     * @return float[] - mảng vector embedding (768 chiều với Gemini)
     */
    float[] embed(String text);

    /**
     * Kiểm tra provider có khả dụng không (API Key hợp lệ, mạng kết nối được)
     */
    boolean isAvailable();

    /**
     * Tên của embedding provider (dùng cho logging và monitoring)
     */
    String getProviderName();
}
