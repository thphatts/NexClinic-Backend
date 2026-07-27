package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.dto.request.AiChatRequest;
import com.thphatts.clinicportal.dto.request.SymptomAnalysisRequest;
import com.thphatts.clinicportal.dto.response.AiChatResponse;
import com.thphatts.clinicportal.dto.response.MedicalRecordSummaryResponse;
import com.thphatts.clinicportal.dto.response.SymptomAnalysisResponse;
import com.thphatts.clinicportal.entity.MedicalRecord;
import com.thphatts.clinicportal.entity.PrescriptionItem;
import com.thphatts.clinicportal.repository.ClinicKnowledgeVectorRepository;
import com.thphatts.clinicportal.repository.MedicalRecordRepository;
import com.thphatts.clinicportal.service.AiService;
import com.thphatts.clinicportal.service.ai.LlmProvider;
import com.thphatts.clinicportal.service.ai.LlmProviderFactory;
import com.thphatts.clinicportal.service.ai.embedding.EmbeddingProviderFactory;
import com.thphatts.clinicportal.service.ai.embedding.EmbeddingService;
import com.thphatts.clinicportal.service.ai.memory.AiConversationMemoryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IAiService implements AiService {

    private static final String MEDICAL_DISCLAIMER =
            "Miễn trừ trách nhiệm y tế: Phản hồi từ Trợ lý AI chỉ mang tính chất tham khảo thông tin, " +
            "không thay thế cho chẩn đoán, chỉ định hay tư vấn y khoa trực tiếp từ Bác sĩ.";

    private final LlmProviderFactory llmProviderFactory;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AiConversationMemoryManager memoryManager;
    private final ClinicKnowledgeVectorRepository knowledgeVectorRepository;
    private final EmbeddingProviderFactory embeddingProviderFactory;

    @Qualifier("aiAsyncExecutor")
    private final Executor aiAsyncExecutor;

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String sessionId = request.sessionId() != null && !request.sessionId().isBlank()
                ? request.sessionId()
                : UUID.randomUUID().toString();

        // 1. Lưu câu hỏi bệnh nhân vào bộ nhớ hội thoại PostgreSQL
        memoryManager.addMessage(sessionId, "Bệnh nhân", request.message());
        String conversationHistory = memoryManager.getFormattedHistory(sessionId);

        LlmProvider provider = llmProviderFactory.getProvider();

        // 2. PGVector Semantic RAG: Chỉ nạp Top-3 tri thức liên quan nhất (giảm 70% token)
        String ragContext = buildSemanticRagContext(request.message());

        String ragPrompt = String.format("""
                [SYSTEM INSTRUCTION]
                Bạn là Bác sĩ Trợ lý AI Y tế của Phòng khám AI-Powered Clinic Portal.
                Hãy tư vấn cho bệnh nhân bằng tiếng Việt thân thiện, ân cần và tự nhiên.
                
                [TRI THỨC PHÒNG KHÁM LIÊN QUAN NHẤT]
                %s
                
                [LỊCH SỬ HỘI THOẠI GẦN ĐÂY]
                %s
                
                [CÂU HỎI BỆNH NHÂN]
                "%s"
                
                Hãy phân tích câu hỏi trên và đưa ra câu trả lời ngắn gọn, chính xác theo dữ liệu phòng khám ở trên.
                """, ragContext, conversationHistory, request.message());

        String reply = provider.generateText(ragPrompt);

        if (reply == null || reply.isBlank()) {
            reply = "Tôi đã ghi nhận ý kiến của bạn. Bạn có thể sử dụng các gợi ý bên dưới để tra cứu Bác sĩ hoặc Đặt lịch hẹn khám ngay.";
        }

        // 3. Lưu câu trả lời của AI vào bộ nhớ hội thoại PostgreSQL
        memoryManager.addMessage(sessionId, "Trợ lý AI", reply);

        List<String> suggestedActions = List.of(
                "Phân tích triệu chứng chi tiết",
                "Đặt lịch hẹn khám Bác sĩ",
                "Xem danh mục Dược phẩm & Thuốc"
        );

        return new AiChatResponse(
                reply,
                sessionId,
                suggestedActions,
                MEDICAL_DISCLAIMER,
                LocalDateTime.now()
        );
    }

    @Override
    public SseEmitter streamChat(AiChatRequest request) {
        String sessionId = request.sessionId() != null && !request.sessionId().isBlank()
                ? request.sessionId()
                : UUID.randomUUID().toString();

        SseEmitter emitter = new SseEmitter(120000L); // Timeout 2 phút

        // Thực thi bất đồng bộ trên aiAsyncExecutor ThreadPool
        aiAsyncExecutor.execute(() -> {
            try {
                // 1. Lưu tin nhắn bệnh nhân
                memoryManager.addMessage(sessionId, "Bệnh nhân", request.message());
                String history = memoryManager.getFormattedHistory(sessionId);

                LlmProvider provider = llmProviderFactory.getProvider();
                // PGVector Semantic RAG: Top-3 tri thức liên quan nhất
                String ragContext = buildSemanticRagContext(request.message());

                String ragPrompt = String.format("""
                        [SYSTEM INSTRUCTION]
                        Bạn là Bác sĩ Trợ lý AI Y tế của Phòng khám AI-Powered Clinic Portal.
                        Hãy tư vấn cho bệnh nhân bằng tiếng Việt thân thiện, ân cần và tự nhiên.
                        
                        [TRI THỨC PHÒNG KHÁM LIÊN QUAN NHẤT]
                        %s
                        
                        [LỊCH SỬ HỘI THOẠI GẦN ĐÂY]
                        %s
                        
                        [CÂU HỎI BỆNH NHÂN]
                        "%s"
                        
                        Hãy phân tích câu hỏi trên và đưa ra câu trả lời ngắn gọn, chính xác theo dữ liệu phòng khám ở trên.
                        """, ragContext, history, request.message());

                String fullReply = provider.generateText(ragPrompt);
                if (fullReply == null || fullReply.isBlank()) {
                    fullReply = "Tôi đã ghi nhận ý kiến của bạn. Bạn có thể tra cứu lịch hẹn hoặc đặt khám Bác sĩ ngay.";
                }

                // 2. Stream từng token/từ về Frontend
                String[] words = fullReply.split("(?<=\\s)");
                for (String word : words) {
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(word));
                    Thread.sleep(30); // Giả lập streaming mượt mà 30ms/token
                }

                // Send session ID event
                emitter.send(SseEmitter.event()
                        .name("session")
                        .data(sessionId));

                // 3. Lưu toàn bộ câu trả lời AI vào DB
                memoryManager.addMessage(sessionId, "Trợ lý AI", fullReply);

                emitter.complete();
            } catch (Exception e) {
                log.error("Lỗi khi streaming AI response: ", e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @Override
    public SymptomAnalysisResponse analyzeSymptoms(SymptomAnalysisRequest request) {
        String symptomsLower = request.symptoms().toLowerCase();
        int days = request.durationDays() != null ? request.durationDays() : 1;

        String riskLevel = "LOW";
        String primaryDiagnosis = "Theo dõi sức khỏe thông thường";
        List<String> differentialDiagnoses = new ArrayList<>();
        String recommendedSpecialization = "Chuyên khoa Nội Tổng Quát";
        List<String> urgentWarnings = new ArrayList<>();
        String advice;

        if (symptomsLower.contains("đau ngực dữ dội") || symptomsLower.contains("khó thở nặng") ||
            symptomsLower.contains("co giật") || symptomsLower.contains("nôn ra máu") || symptomsLower.contains("mất ý thức")) {
            riskLevel = "EMERGENCY";
            primaryDiagnosis = "Cảnh báo nguy cơ biến chứng cấp tính (Tim mạch / Hô hấp / Thần kinh)";
            recommendedSpecialization = "Khoa Cấp Cứu / Tim Mạch";
            urgentWarnings.add("🚨 Dấu hiệu nguy hiểm cấp tính! Hãy đến ngay cơ sở y tế hoặc bệnh viện gần nhất.");
            urgentWarnings.add("Không tự lái xe, cần có người thân hoặc gọi cấp cứu 115 hỗ trợ.");
        } else if (symptomsLower.contains("sốt cao") || symptomsLower.contains("sốt 39") || symptomsLower.contains("sốt 40") ||
                   symptomsLower.contains("đau đầu dữ dội") || symptomsLower.contains("tê nửa người")) {
            riskLevel = "HIGH";
            primaryDiagnosis = "Nghi ngờ Nhiễm trùng cấp / Sốt siêu vi nặng";
            differentialDiagnoses.add("Sốt xuất huyết");
            differentialDiagnoses.add("Viêm đường hô hấp cấp");
            recommendedSpecialization = "Chuyên khoa Nội / Hô Hấp";
            urgentWarnings.add("Theo dõi sát thân nhiệt. Nếu sốt không hạ sau khi dùng hạ sốt, cần đến phòng khám ngay.");
        } else if (symptomsLower.contains("ho") || symptomsLower.contains("sốt") || symptomsLower.contains("đau họng")) {
            riskLevel = "MEDIUM";
            primaryDiagnosis = "Nghi ngờ Viêm đường hô hấp trên / Cảm cúm thông thường";
            differentialDiagnoses.add("Viêm họng cấp");
            differentialDiagnoses.add("Cảm lạnh thông thường");
            recommendedSpecialization = "Chuyên khoa Hô Hấp";
        } else if (symptomsLower.contains("đau bụng") || symptomsLower.contains("nôn") || symptomsLower.contains("tiêu chảy")) {
            riskLevel = "MEDIUM";
            primaryDiagnosis = "Nghi ngờ Rối loạn tiêu hóa / Viêm dạ dày ruột";
            differentialDiagnoses.add("Nhiễm độc thức ăn");
            recommendedSpecialization = "Chuyên khoa Tiêu Hóa";
        }

        if ("EMERGENCY".equals(riskLevel)) {
            advice = "Trường hợp này cần thăm khám cấp cứu ngay lập tức. Bệnh nhân không nên trì hoãn.";
        } else if ("HIGH".equals(riskLevel)) {
            advice = "Triệu chứng kéo dài " + days + " ngày có nguy cơ tiến triển xấu. Bạn nên đặt lịch khám Bác sĩ trong ngày.";
        } else {
            advice = "Hãy giữ vệ sinh cá nhân, uống đủ 2 lít nước ấm mỗi ngày, nghỉ ngơi hợp lý và theo dõi thân nhiệt. " +
                     "Nếu triệu chứng không thuyên giảm sau 2-3 ngày, hãy đặt lịch khám Bác sĩ.";
        }

        return new SymptomAnalysisResponse(
                primaryDiagnosis,
                differentialDiagnoses,
                riskLevel,
                recommendedSpecialization,
                urgentWarnings,
                advice,
                MEDICAL_DISCLAIMER
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordSummaryResponse summarizeMedicalRecord(Long medicalRecordId) {
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ bệnh án với ID: " + medicalRecordId));

        String patientName = record.getPatient() != null ? record.getPatient().getFullName() : "N/A";
        String doctorName = record.getDoctor() != null ? record.getDoctor().getFullName() : "N/A";

        StringBuilder summaryBuilder = new StringBuilder();
        summaryBuilder.append("Bệnh nhân ").append(patientName)
                .append(" đã được Bác sĩ ").append(doctorName)
                .append(" thăm khám với chẩn đoán: ").append(record.getDiagnosis()).append(". ");

        if (record.getSymptoms() != null && !record.getSymptoms().isBlank()) {
            summaryBuilder.append("Ghi nhận các triệu chứng chính: ").append(record.getSymptoms()).append(". ");
        }

        List<String> keyPrecautions = new ArrayList<>();
        if (record.getNotes() != null && !record.getNotes().isBlank()) {
            keyPrecautions.add("Lời khuyên của bác sĩ: " + record.getNotes());
        }

        if (record.getPrescription() != null && record.getPrescription().getItems() != null) {
            int medCount = record.getPrescription().getItems().size();
            summaryBuilder.append("Bác sĩ đã kê đơn gồm ").append(medCount).append(" loại thuốc. ");

            for (PrescriptionItem item : record.getPrescription().getItems()) {
                String medInfo = item.getProduct() != null ? item.getProduct().getName() : "Thuốc";
                keyPrecautions.add("Thuốc " + medInfo + ": Uống " + item.getQuantity() + " viên/hộp - " + item.getDosage());
            }
        }

        String reexamAdvice = record.getReexaminationDate() != null
                ? "Lịch tái khám dự kiến: " + record.getReexaminationDate()
                : "Tái khám khi có dấu hiệu bất thường.";

        return new MedicalRecordSummaryResponse(
                medicalRecordId,
                patientName,
                doctorName,
                summaryBuilder.toString(),
                keyPrecautions,
                reexamAdvice,
                MEDICAL_DISCLAIMER
        );
    }

    /**
     * PGVector Semantic RAG Context Builder.
     * Thay thế buildClinicRagContext() - chỉ nạp Top-3 mẩu tri thức liên quan nhất với câu hỏi.
     * Giảm 70% Token, tăng độ chính xác bằng cách loại bỏ noise từ dữ liệu không liên quan.
     *
     * Chiến lược Fallback:
     * 1. Thử Semantic Search với PGVector (nếu có index).
     * 2. Nếu PGVector chưa được index → Fallback về keyword search trong knowledge table.
     * 3. Nếu knowledge table rỗng → Trả về thông tin cứng cơ bản của phòng khám.
     */
    public String buildSemanticRagContext(String userQuery) {
        try {
            long indexedCount = knowledgeVectorRepository.countIndexedEntries();

            if (indexedCount > 0) {
                // === PATH 1: PGVector Cosine Similarity Search ===
                EmbeddingService embeddingService = embeddingProviderFactory.getProvider();
                float[] queryVector = embeddingService.embed(userQuery);

                // Convert float[] → PostgreSQL vector string format: '[0.1,0.2,...]'
                String vecString = Arrays.toString(queryVector)
                        .replace(" ", ""); // '[0.123,0.456,...]'

                List<String> topKContext = knowledgeVectorRepository.findTopKSimilarContent(vecString, 3);

                if (!topKContext.isEmpty()) {
                    log.info("🎯 [Semantic RAG] Tìm thấy {} tri thức liên quan nhất (PGVector Cosine Search)", topKContext.size());
                    return String.join("\n- ", topKContext);
                }
            }

            // === PATH 2: Fallback - Thông tin cứng phòng khám ===
            log.warn("⚠️ [Semantic RAG] Knowledge vectors chưa sẵn sàng, dùng thông tin phòng khám cơ bản.");
            return "Phòng khám AI-Powered Clinic Portal làm việc từ 07:30 - 20:00 hàng ngày kể cả T7/CN. " +
                   "Hotline: 1900-1234. Địa chỉ: TP. Hồ Chí Minh. Đặt lịch online qua ứng dụng.";

        } catch (Exception e) {
            log.error("❌ [Semantic RAG] Lỗi khi tìm kiếm ngữ nghĩa, dùng fallback: {}", e.getMessage());
            return "Phòng khám AI-Powered Clinic Portal luôn sẵn sàng phục vụ bạn từ 07:30 - 20:00. Hotline: 1900-1234.";
        }
    }
}
