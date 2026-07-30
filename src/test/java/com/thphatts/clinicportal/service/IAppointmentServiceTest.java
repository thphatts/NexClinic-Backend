package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.AppointmentRequest;
import com.thphatts.clinicportal.dto.response.AppointmentResponse;
import com.thphatts.clinicportal.entity.Appointment;
import com.thphatts.clinicportal.entity.Doctor;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.entity.enums.AppointmentStatus;
import com.thphatts.clinicportal.mapper.AppointmentMapper;
import com.thphatts.clinicportal.repository.AppointmentRepository;
import com.thphatts.clinicportal.repository.DoctorRepository;
import com.thphatts.clinicportal.repository.PatientRepository;
import com.thphatts.clinicportal.service.impl.IAppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IAppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @InjectMocks
    private IAppointmentService appointmentService;

    private Patient mockPatient;
    private Doctor mockDoctor;
    private AppointmentRequest validRequest;
    private Appointment mockAppointment;
    private AppointmentResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockPatient = Patient.builder()
                .id(1L)
                .fullName("Bệnh nhân Nguyễn Văn X")
                .phone("0901234567")
                .build();

        mockDoctor = Doctor.builder()
                .id(1L)
                .fullName("Bác sĩ Nguyễn Văn A")
                .specialization("Nội Khoa")
                .consultationFee(BigDecimal.valueOf(200000))
                .build();

        validRequest = new AppointmentRequest(
                1L,
                1L,
                LocalDate.now().plusDays(1),
                "09:00 - 09:30",
                "Tái khám định kỳ",
                "Mang theo sổ khám cũ"
        );

        mockAppointment = Appointment.builder()
                .id(100L)
                .patient(mockPatient)
                .doctor(mockDoctor)
                .appointmentDate(validRequest.appointmentDate())
                .timeSlot(validRequest.timeSlot())
                .status(AppointmentStatus.PENDING)
                .reason(validRequest.reason())
                .notes(validRequest.notes())
                .build();

        mockResponse = new AppointmentResponse(
                100L,
                1L,
                "Bệnh nhân Nguyễn Văn X",
                "0901234567",
                1L,
                "Bác sĩ Nguyễn Văn A",
                "Nội Khoa",
                BigDecimal.valueOf(200000),
                validRequest.appointmentDate(),
                validRequest.timeSlot(),
                AppointmentStatus.PENDING,
                validRequest.reason(),
                validRequest.notes(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("Chống trùng lịch (Double-Booking Prevention)")
    class DoubleBookingTests {

        @Test
        @DisplayName("Tạo lịch hẹn thành công khi không có trùng lịch")
        void createAppointment_Success() {
            when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatusNot(
                    1L, validRequest.appointmentDate(), validRequest.timeSlot(), AppointmentStatus.CANCELLED
            )).thenReturn(false);
            when(appointmentMapper.toEntity(validRequest)).thenReturn(mockAppointment);
            when(appointmentRepository.saveAndFlush(mockAppointment)).thenReturn(mockAppointment);
            when(appointmentMapper.toResponse(mockAppointment)).thenReturn(mockResponse);

            AppointmentResponse response = appointmentService.createAppointment(validRequest);

            assertNotNull(response);
            assertEquals(100L, response.id());
            assertEquals(AppointmentStatus.PENDING, response.status());
            verify(appointmentRepository, times(1)).saveAndFlush(mockAppointment);
        }

        @Test
        @DisplayName("Phát hiện trùng lịch ở tầng Application (Check-then-Act conflict)")
        void createAppointment_ApplicationConflict_ThrowsException() {
            when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatusNot(
                    1L, validRequest.appointmentDate(), validRequest.timeSlot(), AppointmentStatus.CANCELLED
            )).thenReturn(true);

            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    appointmentService.createAppointment(validRequest)
            );

            assertTrue(exception.getMessage().contains("đã có lịch hẹn khác vào khung giờ"));
            verify(appointmentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Phát hiện trùng lịch đồng thời ở tầng Database Constraint (Race Condition Handling)")
        void createAppointment_DatabaseRaceCondition_ThrowsException() {
            when(patientRepository.findById(1L)).thenReturn(Optional.of(mockPatient));
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatusNot(
                    1L, validRequest.appointmentDate(), validRequest.timeSlot(), AppointmentStatus.CANCELLED
            )).thenReturn(false); // Do 2 request đến cùng lúc nên check ở app đều trả về false
            when(appointmentMapper.toEntity(validRequest)).thenReturn(mockAppointment);
            
            // Giả lập Database ném DataIntegrityViolationException do trùng Partial Unique Index
            when(appointmentRepository.saveAndFlush(mockAppointment))
                    .thenThrow(new DataIntegrityViolationException("Unique index constraint violation: idx_unique_doctor_active_slot"));

            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    appointmentService.createAppointment(validRequest)
            );

            assertTrue(exception.getMessage().contains("vừa có lịch hẹn mới được đăng ký vào khung giờ"));
            verify(appointmentRepository, times(1)).saveAndFlush(mockAppointment);
        }
    }
}
