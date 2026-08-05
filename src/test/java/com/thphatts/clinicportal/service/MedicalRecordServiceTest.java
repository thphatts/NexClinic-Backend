package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.MedicalRecordRequest;
import com.thphatts.clinicportal.dto.request.PrescriptionRequest;
import com.thphatts.clinicportal.dto.request.PrescriptionItemRequest;
import com.thphatts.clinicportal.dto.response.MedicalRecordResponse;
import com.thphatts.clinicportal.entity.*;
import com.thphatts.clinicportal.entity.enums.AppointmentStatus;
import com.thphatts.clinicportal.mapper.MedicalRecordMapper;
import com.thphatts.clinicportal.repository.*;
import com.thphatts.clinicportal.service.impl.IMedicalRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicalRecordService Unit Tests")
class MedicalRecordServiceTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ProductRepository productRepository;
    @Mock private MedicalRecordMapper medicalRecordMapper;

    @InjectMocks
    private IMedicalRecordService medicalRecordService;

    private Appointment mockAppointment;
    private Patient mockPatient;
    private Doctor mockDoctor;
    private MedicalRecord mockRecord;
    private MedicalRecordResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockPatient = Patient.builder()
                .id(1L)
                .fullName("Bệnh nhân Nguyễn Văn X")
                .phone("0901234567")
                .citizenId("079000000001")
                .build();

        mockDoctor = Doctor.builder()
                .id(1L)
                .fullName("BS. Trần Thị Bình")
                .specialization("Nội Khoa")
                .build();

        mockAppointment = Appointment.builder()
                .id(1L)
                .patient(mockPatient)
                .doctor(mockDoctor)
                .appointmentDate(LocalDate.now())
                .timeSlot("09:00 - 09:30")
                .status(AppointmentStatus.CONFIRMED)
                .amount(BigDecimal.valueOf(200000))
                .build();

        mockRecord = MedicalRecord.builder()
                .id(100L)
                .appointment(mockAppointment)
                .patient(mockPatient)
                .doctor(mockDoctor)
                .diagnosis("Cảm cúm thông thường")
                .symptoms("Sốt, ho, sổ mũi")
                .build();

        // MedicalRecordResponse record: id, appointmentId, patientId, patientName, patientPhone,
        // citizenId, doctorId, doctorName, doctorSpecialization, diagnosis, symptoms, notes,
        // reexaminationDate, prescription, createdAt
        mockResponse = new MedicalRecordResponse(
                100L, 1L, 1L,
                "Bệnh nhân Nguyễn Văn X", "0901234567", "079000000001",
                1L, "BS. Trần Thị Bình", "Nội Khoa",
                "Cảm cúm thông thường", "Sốt, ho, sổ mũi", null,
                null, null,
                LocalDateTime.now()
        );
    }

    // =========================================================
    // CREATE MEDICAL RECORD TESTS
    // =========================================================
    @Nested
    @DisplayName("Tạo Hồ sơ Bệnh án (Create Medical Record)")
    class CreateMedicalRecordTests {

        @Test
        @DisplayName("Tạo bệnh án thành công - không có toa thuốc")
        void createMedicalRecord_Success_WithoutPrescription() {
            // Arrange
            MedicalRecordRequest request = new MedicalRecordRequest(
                    1L, "Cảm cúm thông thường", "Sốt, ho, sổ mũi",
                    "Nghỉ ngơi, uống nhiều nước", LocalDate.now().plusDays(7), null
            );

            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(mockAppointment));
            when(medicalRecordRepository.existsByAppointmentId(1L)).thenReturn(false);
            when(medicalRecordRepository.save(any(MedicalRecord.class))).thenReturn(mockRecord);
            when(medicalRecordMapper.toResponse(mockRecord)).thenReturn(mockResponse);

            // Act
            MedicalRecordResponse result = medicalRecordService.createMedicalRecord(request);

            // Assert
            assertNotNull(result);
            assertEquals(100L, result.id());
            assertEquals("Cảm cúm thông thường", result.diagnosis());

            // Lịch hẹn phải được chuyển thành COMPLETED
            assertEquals(AppointmentStatus.COMPLETED, mockAppointment.getStatus());
            verify(appointmentRepository, times(1)).save(mockAppointment);
            verify(medicalRecordRepository, times(1)).save(any(MedicalRecord.class));
        }

        @Test
        @DisplayName("Tạo bệnh án thành công - có toa thuốc với 2 loại thuốc")
        void createMedicalRecord_Success_WithPrescription() {
            // Arrange
            Product paracetamol = new Product();
            paracetamol.setId(1L);
            paracetamol.setName("Paracetamol 500mg");
            paracetamol.setPrice(BigDecimal.valueOf(5000));

            Product amoxicillin = new Product();
            amoxicillin.setId(2L);
            amoxicillin.setName("Amoxicillin 500mg");
            amoxicillin.setPrice(BigDecimal.valueOf(8000));

            // PrescriptionItemRequest(productId, quantity, dosage)
            PrescriptionItemRequest item1 = new PrescriptionItemRequest(1L, 20, "Uống 2 viên/lần x 3 lần/ngày");
            PrescriptionItemRequest item2 = new PrescriptionItemRequest(2L, 14, "Uống 1 viên/lần x 2 lần/ngày");
            // PrescriptionRequest(notes, items)
            PrescriptionRequest prescriptionRequest = new PrescriptionRequest("Uống sau ăn", List.of(item1, item2));

            MedicalRecordRequest requestWithPrescription = new MedicalRecordRequest(
                    1L, "Viêm họng cấp", "Đau họng, sốt nhẹ",
                    "Tránh đồ lạnh", LocalDate.now().plusDays(5), prescriptionRequest
            );

            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(mockAppointment));
            when(medicalRecordRepository.existsByAppointmentId(1L)).thenReturn(false);
            when(productRepository.findById(1L)).thenReturn(Optional.of(paracetamol));
            when(productRepository.findById(2L)).thenReturn(Optional.of(amoxicillin));
            when(medicalRecordRepository.save(any(MedicalRecord.class))).thenReturn(mockRecord);
            when(medicalRecordMapper.toResponse(mockRecord)).thenReturn(mockResponse);

            // Act
            MedicalRecordResponse result = medicalRecordService.createMedicalRecord(requestWithPrescription);

            // Assert
            assertNotNull(result);
            verify(productRepository, times(1)).findById(1L);
            verify(productRepository, times(1)).findById(2L);
            verify(medicalRecordRepository, times(1)).save(any(MedicalRecord.class));
        }

        @Test
        @DisplayName("Tạo bệnh án thất bại - Lịch hẹn không tồn tại")
        void createMedicalRecord_Fail_AppointmentNotFound() {
            // Arrange
            MedicalRecordRequest badRequest = new MedicalRecordRequest(
                    999L, "Chẩn đoán", "Triệu chứng", null, null, null
            );
            when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> medicalRecordService.createMedicalRecord(badRequest));

            assertTrue(ex.getMessage().contains("Không tìm thấy lịch hẹn với ID: 999"));
            verify(medicalRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Tạo bệnh án thất bại - Hồ sơ bệnh án cho lịch hẹn này đã tồn tại")
        void createMedicalRecord_Fail_AlreadyExists() {
            // Arrange
            MedicalRecordRequest request = new MedicalRecordRequest(
                    1L, "Cảm cúm", "Sốt", null, null, null
            );
            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(mockAppointment));
            when(medicalRecordRepository.existsByAppointmentId(1L)).thenReturn(true);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> medicalRecordService.createMedicalRecord(request));

            assertTrue(ex.getMessage().contains("Hồ sơ bệnh án cho lịch hẹn này đã tồn tại"));
            verify(medicalRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Tạo bệnh án thất bại - Thuốc trong đơn không tồn tại")
        void createMedicalRecord_Fail_ProductNotFound() {
            // Arrange
            PrescriptionItemRequest badItem = new PrescriptionItemRequest(999L, 10, "2 lần/ngày");
            PrescriptionRequest prescription = new PrescriptionRequest(null, List.of(badItem));

            MedicalRecordRequest requestWithBadProduct = new MedicalRecordRequest(
                    1L, "Chẩn đoán", "Triệu chứng", null, null, prescription
            );

            when(appointmentRepository.findById(1L)).thenReturn(Optional.of(mockAppointment));
            when(medicalRecordRepository.existsByAppointmentId(1L)).thenReturn(false);
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> medicalRecordService.createMedicalRecord(requestWithBadProduct));

            assertTrue(ex.getMessage().contains("Không tìm thấy thuốc/sản phẩm với ID: 999"));
        }
    }

    // =========================================================
    // GET MEDICAL RECORD TESTS
    // =========================================================
    @Nested
    @DisplayName("Lấy Hồ sơ Bệnh án (Get Medical Record)")
    class GetMedicalRecordTests {

        @Test
        @DisplayName("Lấy bệnh án theo ID thành công")
        void getMedicalRecordById_Success() {
            // Arrange
            when(medicalRecordRepository.findById(100L)).thenReturn(Optional.of(mockRecord));
            when(medicalRecordMapper.toResponse(mockRecord)).thenReturn(mockResponse);

            // Act
            MedicalRecordResponse result = medicalRecordService.getMedicalRecordById(100L);

            // Assert
            assertNotNull(result);
            assertEquals(100L, result.id());
        }

        @Test
        @DisplayName("Lấy bệnh án theo ID - Không tìm thấy")
        void getMedicalRecordById_NotFound() {
            // Arrange
            when(medicalRecordRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> medicalRecordService.getMedicalRecordById(999L));

            assertTrue(ex.getMessage().contains("Không tìm thấy hồ sơ bệnh án với ID: 999"));
        }

        @Test
        @DisplayName("Lấy danh sách bệnh án của bệnh nhân")
        void getMedicalRecordsByPatientId_Success() {
            // Arrange
            when(medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(mockRecord));
            when(medicalRecordMapper.toResponse(mockRecord)).thenReturn(mockResponse);

            // Act
            List<MedicalRecordResponse> results = medicalRecordService.getMedicalRecordsByPatientId(1L);

            // Assert
            assertNotNull(results);
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Lấy bệnh án theo Appointment ID - Không tìm thấy")
        void getMedicalRecordByAppointmentId_NotFound() {
            // Arrange
            when(medicalRecordRepository.findByAppointmentId(999L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> medicalRecordService.getMedicalRecordByAppointmentId(999L));

            assertTrue(ex.getMessage().contains("Không tìm thấy hồ sơ bệnh án cho lịch hẹn ID: 999"));
        }
    }
}
