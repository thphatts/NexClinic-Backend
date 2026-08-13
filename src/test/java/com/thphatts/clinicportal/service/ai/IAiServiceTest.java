package com.thphatts.clinicportal.service.ai;

import com.thphatts.clinicportal.dto.request.AiChatRequest;
import com.thphatts.clinicportal.dto.request.SymptomAnalysisRequest;
import com.thphatts.clinicportal.dto.response.AiAgentActionResult;
import com.thphatts.clinicportal.dto.response.AiChatResponse;
import com.thphatts.clinicportal.dto.response.MedicalRecordSummaryResponse;
import com.thphatts.clinicportal.dto.response.SymptomAnalysisResponse;
import com.thphatts.clinicportal.entity.Doctor;
import com.thphatts.clinicportal.entity.MedicalRecord;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.entity.Prescription;
import com.thphatts.clinicportal.entity.PrescriptionItem;
import com.thphatts.clinicportal.entity.Product;
import com.thphatts.clinicportal.repository.ClinicKnowledgeVectorRepository;
import com.thphatts.clinicportal.repository.MedicalRecordRepository;
import com.thphatts.clinicportal.service.ai.agent.AiAgentService;
import com.thphatts.clinicportal.service.ai.embedding.EmbeddingProviderFactory;
import com.thphatts.clinicportal.service.ai.embedding.EmbeddingService;
import com.thphatts.clinicportal.service.ai.memory.AiConversationMemoryManager;
import com.thphatts.clinicportal.service.impl.IAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IAiService Unit Tests")
class IAiServiceTest {

    @Mock private LlmProviderFactory llmProviderFactory;
    @Mock private LlmProvider llmProvider;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private AiConversationMemoryManager memoryManager;
    @Mock private ClinicKnowledgeVectorRepository knowledgeVectorRepository;
    @Mock private EmbeddingProviderFactory embeddingProviderFactory;
    @Mock private EmbeddingService embeddingService;
    @Mock private AiAgentService aiAgentService;
    @Mock private Executor aiAsyncExecutor;

    @InjectMocks
    private IAiService aiService;

    @BeforeEach
    void setUp() {
        when(llmProviderFactory.getProvider()).thenReturn(llmProvider);
        when(embeddingProviderFactory.getProvider()).thenReturn(embeddingService);
    }

    @Nested
    @DisplayName("Tư vấn Chat AI (chat)")
    class ChatTests {

        @Test
        @DisplayName("Trò chuyện AI thông thường — nhận được câu trả lời tư vấn và gợi ý hành động")
        void chat_GeneralQuery_ReturnsReplyAndSuggestions() {
            AiChatRequest request = new AiChatRequest("Tôi bị ho khan và mệt mỏi", "session-123", null);

            when(aiAgentService.isActionIntent(anyString())).thenReturn(false);
            when(knowledgeVectorRepository.countIndexedEntries()).thenReturn(0L);
            when(memoryManager.getFormattedHistory("session-123")).thenReturn("Bệnh nhân: Tôi bị ho khan");
            when(llmProvider.generateText(anyString())).thenReturn("Bạn nên nghỉ ngơi và uống nhiều nước ấm.");

            AiChatResponse response = aiService.chat(request);

            assertNotNull(response);
            assertEquals("session-123", response.sessionId());
            assertEquals("Bạn nên nghỉ ngơi và uống nhiều nước ấm.", response.reply());
            assertNotNull(response.suggestedActions());
            assertFalse(response.suggestedActions().isEmpty());
            assertNotNull(response.disclaimer());

            verify(memoryManager).addMessage("session-123", "Bệnh nhân", "Tôi bị ho khan và mệt mỏi");
            verify(memoryManager).addMessage("session-123", "Trợ lý AI", "Bạn nên nghỉ ngơi và uống nhiều nước ấm.");
        }

        @Test
        @DisplayName("Nhận diện lệnh AI Agent trong chat — thực thi Tool và trả về kết quả Agent")
        void chat_ActionIntent_ExecutesAgentTool() {
            AiChatRequest request = new AiChatRequest("Đặt lịch khám bác sĩ An sáng mai", "session-456", null);
            AiAgentActionResult agentResult = AiAgentActionResult.success(
                    "CREATE_APPOINTMENT",
                    "Đã tạo thành công lịch hẹn #100",
                    null
            );

            when(aiAgentService.isActionIntent(request.message())).thenReturn(true);
            when(aiAgentService.processUserAction(request.message())).thenReturn(agentResult);

            AiChatResponse response = aiService.chat(request);

            assertNotNull(response);
            assertEquals("session-456", response.sessionId());
            assertTrue(response.reply().contains("[AI Agent] Đã tạo thành công lịch hẹn #100"));
            verify(llmProviderFactory, never()).getProvider();
        }

        @Test
        @DisplayName("Sử dụng PGVector RAG khi đã có tri thức được index")
        void chat_WithPgVectorRAG_IntegratesSemanticContext() {
            AiChatRequest request = new AiChatRequest("Giờ làm việc phòng khám", "session-789", null);

            when(aiAgentService.isActionIntent(anyString())).thenReturn(false);
            when(knowledgeVectorRepository.countIndexedEntries()).thenReturn(5L);
            when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
            when(knowledgeVectorRepository.findTopKSimilarContent(anyString(), eq(3)))
                    .thenReturn(List.of("Phòng khám mở cửa từ 07:30 đến 20:00"));
            when(llmProvider.generateText(contains("Phòng khám mở cửa")))
                    .thenReturn("Phòng khám mở cửa từ 7h30 đến 20h00 hàng ngày.");

            AiChatResponse response = aiService.chat(request);

            assertNotNull(response);
            assertEquals("Phòng khám mở cửa từ 7h30 đến 20h00 hàng ngày.", response.reply());
            verify(knowledgeVectorRepository).findTopKSimilarContent(anyString(), eq(3));
        }
    }

    @Nested
    @DisplayName("Phân tích Triệu chứng (analyzeSymptoms)")
    class AnalyzeSymptomsTests {

        @Test
        @DisplayName("Triệu chứng cấp cứu — Phân loại EMERGENCY")
        void analyzeSymptoms_EmergencySymptoms() {
            SymptomAnalysisRequest request = new SymptomAnalysisRequest(
                    "Đau ngực dữ dội, khó thở nặng, vã mồ hôi",
                    1, 45, "Không"
            );

            SymptomAnalysisResponse response = aiService.analyzeSymptoms(request);

            assertNotNull(response);
            assertEquals("EMERGENCY", response.riskLevel());
            assertTrue(response.primaryDiagnosis().contains("cấp tính"));
            assertEquals("Khoa Cấp Cứu / Tim Mạch", response.recommendedSpecialization());
            assertFalse(response.urgentWarnings().isEmpty());
        }

        @Test
        @DisplayName("Triệu chứng sốt cao — Phân loại HIGH")
        void analyzeSymptoms_HighRiskSymptoms() {
            SymptomAnalysisRequest request = new SymptomAnalysisRequest(
                    "Sốt cao 39.5 độ, đau đầu dữ dội",
                    2, 30, "Tiền sử dị ứng"
            );

            SymptomAnalysisResponse response = aiService.analyzeSymptoms(request);

            assertNotNull(response);
            assertEquals("HIGH", response.riskLevel());
            assertEquals("Chuyên khoa Nội / Hô Hấp", response.recommendedSpecialization());
        }

        @Test
        @DisplayName("Triệu chứng cảm cúm nhẹ — Phân loại MEDIUM hoặc LOW")
        void analyzeSymptoms_MediumRiskSymptoms() {
            SymptomAnalysisRequest request = new SymptomAnalysisRequest(
                    "Ho khan, đau họng nhẹ",
                    2, 25, null
            );

            SymptomAnalysisResponse response = aiService.analyzeSymptoms(request);

            assertNotNull(response);
            assertEquals("MEDIUM", response.riskLevel());
            assertEquals("Chuyên khoa Hô Hấp", response.recommendedSpecialization());
            assertNotNull(response.advice());
        }
    }

    @Nested
    @DisplayName("Tóm tắt Hồ sơ Bệnh án (summarizeMedicalRecord)")
    class SummarizeMedicalRecordTests {

        @Test
        @DisplayName("Tóm tắt thành công bệnh án có đơn thuốc")
        void summarizeMedicalRecord_Success() {
            Patient patient = Patient.builder().fullName("Bệnh nhân Nguyễn Văn A").build();
            Doctor doctor = Doctor.builder().fullName("BS. Trần Thị B").build();
            Product medicine = Product.builder().name("Paracetamol 500mg").build();

            PrescriptionItem item = new PrescriptionItem();
            item.setProduct(medicine);
            item.setQuantity(10);
            item.setDosage("Uống 2 viên/ngày");

            Prescription prescription = new Prescription();
            prescription.setItems(List.of(item));

            MedicalRecord record = MedicalRecord.builder()
                    .id(100L)
                    .patient(patient)
                    .doctor(doctor)
                    .diagnosis("Viêm họng cấp")
                    .symptoms("Đau họng, sốt nhẹ")
                    .notes("Nghỉ ngơi, uống nước ấm")
                    .prescription(prescription)
                    .build();

            when(medicalRecordRepository.findById(100L)).thenReturn(Optional.of(record));

            MedicalRecordSummaryResponse summary = aiService.summarizeMedicalRecord(100L);

            assertNotNull(summary);
            assertEquals(100L, summary.medicalRecordId());
            assertEquals("Bệnh nhân Nguyễn Văn A", summary.patientName());
            assertEquals("BS. Trần Thị B", summary.doctorName());
            assertTrue(summary.simplifiedSummary().contains("Viêm họng cấp"));
            assertFalse(summary.keyPrecautions().isEmpty());
        }

        @Test
        @DisplayName("Tóm tắt thất bại khi hồ sơ không tồn tại")
        void summarizeMedicalRecord_NotFound_ThrowsException() {
            when(medicalRecordRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> aiService.summarizeMedicalRecord(999L));

            assertTrue(ex.getMessage().contains("999"));
        }
    }
}
