package com.thphatts.clinicportal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity JPA mapping bảng clinic_knowledge_vectors.
 * Lưu trữ tri thức y tế phòng khám đã được vectorize để tìm kiếm ngữ nghĩa (Semantic Search).
 */
@Entity
@Table(name = "clinic_knowledge_vectors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicKnowledgeVector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nhóm loại tri thức: CLINIC_INFO, DOCTOR, SERVICE, PRODUCT
     */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /**
     * Tiêu đề tri thức (dùng để debug và quản lý)
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Nội dung văn bản đầy đủ (được embed thành vector)
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Vector embedding (768 chiều Gemini hoặc 512 chiều TF-IDF).
     * Lưu dưới dạng float[] để JPA tự quản lý (native query dùng CAST AS vector).
     */
    @Column(name = "embedding_vector", columnDefinition = "vector")
    private float[] embeddingVector;

    /**
     * Loại entity gốc (DOCTOR, PRODUCT, CLINIC) - dùng cho re-indexing thông minh
     */
    @Column(name = "source_entity_type", length = 50)
    private String sourceEntityType;

    /**
     * ID của entity gốc (doctorId, productId) - dùng để update khi entity thay đổi
     */
    @Column(name = "source_entity_id")
    private Long sourceEntityId;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
