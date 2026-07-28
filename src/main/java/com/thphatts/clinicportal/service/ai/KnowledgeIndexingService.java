package com.thphatts.clinicportal.service.ai;

import com.thphatts.clinicportal.entity.ClinicKnowledgeVector;
import com.thphatts.clinicportal.entity.Doctor;
import com.thphatts.clinicportal.entity.Product;
import com.thphatts.clinicportal.repository.ClinicKnowledgeVectorRepository;
import com.thphatts.clinicportal.repository.DoctorRepository;
import com.thphatts.clinicportal.repository.ProductRepository;
import com.thphatts.clinicportal.service.ai.embedding.EmbeddingProviderFactory;
import com.thphatts.clinicportal.service.ai.embedding.EmbeddingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * KnowledgeIndexingService - Tự động Vectorize và Index tri thức phòng khám vào PostgreSQL.
 *
 * Chiến lược indexing:
 * - @PostConstruct: Chạy khi khởi động app, index các entity chưa có vector.
 * - @Scheduled: Re-index toàn bộ mỗi ngày lúc 2h sáng để cập nhật thay đổi.
 * - Upsert logic: Nếu entity đã có vector → update, chưa có → insert mới.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeIndexingService {

    private final ClinicKnowledgeVectorRepository knowledgeVectorRepository;
    private final DoctorRepository doctorRepository;
    private final ProductRepository productRepository;
    private final EmbeddingProviderFactory embeddingProviderFactory;
    private final com.thphatts.clinicportal.service.ai.embedding.TfIdfFallbackEmbeddingService tfIdfFallbackService;

    /**
     * Chạy khi khởi động app: index các tri thức chưa được vector hóa.
     */
    @PostConstruct
    public void indexOnStartup() {
        try {
            long indexed = knowledgeVectorRepository.countIndexedEntries();
            if (indexed == 0) {
                log.info("[KnowledgeIndex] Chưa có knowledge vector nào. Bắt đầu index lần đầu...");
                performFullReindex();
            } else {
                log.info("[KnowledgeIndex] Đã có {} knowledge vectors trong DB. Bỏ qua index lại.", indexed);
                // Chỉ index entity mới chưa có vector
                indexNewEntities();
            }
        } catch (Exception e) {
            log.warn("[KnowledgeIndex] Lỗi khi index lúc startup (không ảnh hưởng app): {}", e.getMessage());
        }
    }

    /**
     * Re-index toàn bộ mỗi ngày lúc 2h sáng (cập nhật thay đổi bác sĩ/dược phẩm).
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledFullReindex() {
        log.info("[KnowledgeIndex] Bắt đầu tác vụ Re-index tri thức phòng khám định kỳ...");
        performFullReindex();
    }

    /**
     * Full re-index: Xóa toàn bộ vector cũ và tạo mới
     */
    @Transactional
    public void performFullReindex() {
        EmbeddingService embeddingService = embeddingProviderFactory.getProvider();
        log.info("[KnowledgeIndex] Sử dụng Embedding Provider: {}", embeddingService.getProviderName());

        List<ClinicKnowledgeVector> toSave = new ArrayList<>();

        // 1. Index tất cả Bác sĩ
        List<Doctor> doctors = doctorRepository.findAll();
        for (Doctor doctor : doctors) {
            String content = buildDoctorContent(doctor);
            float[] vector = safeEmbed(embeddingService, content);
            toSave.add(ClinicKnowledgeVector.builder()
                    .category("DOCTOR")
                    .title("Bác sĩ: " + doctor.getFullName())
                    .content(content)
                    .embeddingVector(vector)
                    .sourceEntityType("DOCTOR")
                    .sourceEntityId(doctor.getId())
                    .build());
        }

        // 2. Index tất cả Dược phẩm
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            String content = buildProductContent(product);
            float[] vector = safeEmbed(embeddingService, content);
            toSave.add(ClinicKnowledgeVector.builder()
                    .category("PRODUCT")
                    .title("Dược phẩm: " + product.getName())
                    .content(content)
                    .embeddingVector(vector)
                    .sourceEntityType("PRODUCT")
                    .sourceEntityId(product.getId())
                    .build());
        }

        // 3. Index thông tin tĩnh phòng khám
        String clinicContent = "Phòng khám AI-Powered Clinic Portal làm việc từ 07:30 - 20:00 hàng ngày, " +
                "kể cả Thứ 7 và Chủ Nhật. Địa chỉ: TP. Hồ Chí Minh. Hotline: 1900-1234. " +
                "Đặt lịch hẹn online, thanh toán linh hoạt, bãi giữ xe miễn phí.";
        float[] clinicVector = safeEmbed(embeddingService, clinicContent);
        toSave.add(ClinicKnowledgeVector.builder()
                .category("CLINIC_INFO")
                .title("Thông tin phòng khám")
                .content(clinicContent)
                .embeddingVector(clinicVector)
                .sourceEntityType("CLINIC")
                .sourceEntityId(0L)
                .build());

        if (!toSave.isEmpty()) {
            // Xóa entries cũ của từng entity type trước khi insert mới
            knowledgeVectorRepository.deleteBySourceEntityType("DOCTOR");
            knowledgeVectorRepository.deleteBySourceEntityType("PRODUCT");
            knowledgeVectorRepository.deleteBySourceEntityType("CLINIC");

            knowledgeVectorRepository.saveAll(toSave);
            log.info("[KnowledgeIndex] Đã index thành công {} knowledge vectors vào PostgreSQL.", toSave.size());
        }
    }

    /**
     * Index nhẹ: Chỉ index entity chưa có vector (tránh gọi Embedding API thừa).
     */
    private void indexNewEntities() {
        EmbeddingService embeddingService = embeddingProviderFactory.getProvider();
        List<ClinicKnowledgeVector> newEntries = new ArrayList<>();

        List<Doctor> doctors = doctorRepository.findAll();
        for (Doctor doctor : doctors) {
            Optional<ClinicKnowledgeVector> existing =
                    knowledgeVectorRepository.findBySourceEntityTypeAndSourceEntityId("DOCTOR", doctor.getId());
            if (existing.isEmpty()) {
                String content = buildDoctorContent(doctor);
                try {
                    float[] vector = embeddingService.embed(content);
                    newEntries.add(ClinicKnowledgeVector.builder()
                            .category("DOCTOR").title("Bác sĩ: " + doctor.getFullName())
                            .content(content).embeddingVector(vector)
                            .sourceEntityType("DOCTOR").sourceEntityId(doctor.getId())
                            .build());
                } catch (Exception e) {
                    log.warn("Không thể embed Doctor mới ID={}: {}", doctor.getId(), e.getMessage());
                }
            }
        }

        if (!newEntries.isEmpty()) {
            knowledgeVectorRepository.saveAll(newEntries);
            log.info("[KnowledgeIndex] Đã index {} entity mới.", newEntries.size());
        }
    }

    private String buildDoctorContent(Doctor doctor) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bác sĩ ").append(doctor.getFullName());
        if (doctor.getSpecialization() != null) {
            sb.append(", chuyên khoa ").append(doctor.getSpecialization());
        }
        if (doctor.getExperienceYears() != null) {
            sb.append(", kinh nghiệm ").append(doctor.getExperienceYears()).append(" năm");
        }
        if (doctor.getDegree() != null && !doctor.getDegree().isBlank()) {
            sb.append(", học vị ").append(doctor.getDegree());
        }
        return sb.toString();
    }

    private String buildProductContent(Product product) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dược phẩm ").append(product.getName());
        if (product.getCategory() != null && product.getCategory().getName() != null) {
            sb.append(", danh mục ").append(product.getCategory().getName());
        }
        if (product.getPrice() != null) {
            sb.append(". Giá: ").append(product.getPrice()).append(" VNĐ");
        }
        if (product.getStatus() != null) {
            sb.append(". Trạng thái: ").append(product.getStatus());
        }
        return sb.toString();
    }

    private float[] safeEmbed(EmbeddingService primaryService, String content) {
        try {
            return primaryService.embed(content);
        } catch (Exception e) {
            log.warn("⚠️ Embedding chính ({}) thất bại, tự động chuyển sang TF-IDF Fallback: {}", 
                    primaryService.getProviderName(), e.getMessage());
            return tfIdfFallbackService.embed(content);
        }
    }
}
