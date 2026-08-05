package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.DoctorRequest;
import com.thphatts.clinicportal.dto.response.DoctorResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.entity.Doctor;
import com.thphatts.clinicportal.mapper.DoctorMapper;
import com.thphatts.clinicportal.repository.DoctorRepository;
import com.thphatts.clinicportal.repository.UserRepository;
import com.thphatts.clinicportal.service.impl.IDoctorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorService Unit Tests")
class DoctorServiceTest {

    @Mock private DoctorRepository doctorRepository;
    @Mock private UserRepository userRepository;
    @Mock private DoctorMapper doctorMapper;

    @InjectMocks
    private IDoctorService doctorService;

    private Doctor mockDoctor;
    private DoctorRequest doctorRequest;
    private DoctorResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockDoctor = Doctor.builder()
                .id(1L)
                .fullName("BS. Trần Thị Bình")
                .specialization("Tim Mạch")
                .degree("Tiến sĩ Y Khoa")
                .phone("0912345678")
                .email("binh.tran@clinic.com")
                .experienceYears(10)
                .consultationFee(BigDecimal.valueOf(300000))
                .build();

        doctorRequest = new DoctorRequest(
                "BS. Trần Thị Bình",
                "Tim Mạch",
                "Tiến sĩ Y Khoa",
                "0912345678",
                "binh.tran@clinic.com",
                10,
                BigDecimal.valueOf(300000),
                null
        );

        mockResponse = new DoctorResponse(
                1L, "BS. Trần Thị Bình", "Tim Mạch", "Tiến sĩ Y Khoa",
                "0912345678", "binh.tran@clinic.com", 10,
                BigDecimal.valueOf(300000), null, null, null
        );
    }

    // =========================================================
    // CREATE DOCTOR TESTS
    // =========================================================
    @Nested
    @DisplayName("Tạo hồ sơ Bác sĩ (Create Doctor)")
    class CreateDoctorTests {

        @Test
        @DisplayName("Tạo bác sĩ thành công")
        void createDoctor_Success() {
            // Arrange
            when(doctorRepository.existsByPhone("0912345678")).thenReturn(false);
            when(doctorRepository.existsByEmail("binh.tran@clinic.com")).thenReturn(false);
            when(doctorMapper.toEntity(doctorRequest)).thenReturn(mockDoctor);
            when(doctorRepository.save(mockDoctor)).thenReturn(mockDoctor);
            when(doctorMapper.toResponse(mockDoctor)).thenReturn(mockResponse);

            // Act
            DoctorResponse result = doctorService.createDoctor(doctorRequest);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.id());
            assertEquals("BS. Trần Thị Bình", result.fullName());
            assertEquals("Tim Mạch", result.specialization());
            verify(doctorRepository, times(1)).save(mockDoctor);
        }

        @Test
        @DisplayName("Tạo bác sĩ thất bại - Số điện thoại đã tồn tại")
        void createDoctor_Fail_PhoneDuplicate() {
            // Arrange
            when(doctorRepository.existsByPhone("0912345678")).thenReturn(true);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> doctorService.createDoctor(doctorRequest));

            assertTrue(ex.getMessage().contains("Số điện thoại bác sĩ đã tồn tại"));
            verify(doctorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Tạo bác sĩ thất bại - Email đã tồn tại")
        void createDoctor_Fail_EmailDuplicate() {
            // Arrange
            when(doctorRepository.existsByPhone("0912345678")).thenReturn(false);
            when(doctorRepository.existsByEmail("binh.tran@clinic.com")).thenReturn(true);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> doctorService.createDoctor(doctorRequest));

            assertTrue(ex.getMessage().contains("Email bác sĩ đã tồn tại"));
            verify(doctorRepository, never()).save(any());
        }
    }

    // =========================================================
    // GET DOCTOR TESTS
    // =========================================================
    @Nested
    @DisplayName("Lấy thông tin Bác sĩ (Get Doctor)")
    class GetDoctorTests {

        @Test
        @DisplayName("Lấy bác sĩ theo ID thành công")
        void getDoctorById_Success() {
            // Arrange
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(doctorMapper.toResponse(mockDoctor)).thenReturn(mockResponse);

            // Act
            DoctorResponse result = doctorService.getDoctorById(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.id());
        }

        @Test
        @DisplayName("Lấy bác sĩ theo ID - Không tìm thấy")
        void getDoctorById_NotFound() {
            // Arrange
            when(doctorRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> doctorService.getDoctorById(999L));

            assertTrue(ex.getMessage().contains("Không tìm thấy bác sĩ với ID: 999"));
        }

        @Test
        @DisplayName("Lấy danh sách bác sĩ có phân trang - không có từ khóa tìm kiếm")
        void getAllDoctors_NoSearch_ReturnsPaged() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Doctor> doctorPage = new PageImpl<>(List.of(mockDoctor), pageable, 1);

            when(doctorRepository.findAll(pageable)).thenReturn(doctorPage);
            when(doctorMapper.toResponse(mockDoctor)).thenReturn(mockResponse);

            // Act
            PagedResponse<DoctorResponse> result = doctorService.getAllDoctors(null, pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.items().size());
            assertEquals(1, result.totalPages());
            assertEquals(1L, result.totalElements());
        }

        @Test
        @DisplayName("Tìm kiếm bác sĩ theo từ khóa")
        void getAllDoctors_WithSearch_CallsSearchMethod() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Doctor> doctorPage = new PageImpl<>(List.of(mockDoctor), pageable, 1);

            when(doctorRepository.searchDoctors("tim mạch", pageable)).thenReturn(doctorPage);
            when(doctorMapper.toResponse(mockDoctor)).thenReturn(mockResponse);

            // Act
            PagedResponse<DoctorResponse> result = doctorService.getAllDoctors("tim mạch", pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.items().size());
            // Đảm bảo gọi đúng method searchDoctors, không phải findAll
            verify(doctorRepository, times(1)).searchDoctors("tim mạch", pageable);
            verify(doctorRepository, never()).findAll(pageable);
        }

        @Test
        @DisplayName("Lấy bác sĩ theo chuyên khoa")
        void getDoctorsBySpecialization_Success() {
            // Arrange
            when(doctorRepository.findBySpecialization("Tim Mạch")).thenReturn(List.of(mockDoctor));
            when(doctorMapper.toResponse(mockDoctor)).thenReturn(mockResponse);

            // Act
            List<DoctorResponse> result = doctorService.getDoctorsBySpecialization("Tim Mạch");

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Tim Mạch", result.get(0).specialization());
        }
    }

    // =========================================================
    // UPDATE DOCTOR TESTS
    // =========================================================
    @Nested
    @DisplayName("Cập nhật thông tin Bác sĩ (Update Doctor)")
    class UpdateDoctorTests {

        @Test
        @DisplayName("Cập nhật bác sĩ thành công")
        void updateDoctor_Success() {
            // Arrange
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(doctorRepository.existsByPhoneAndIdNot("0912345678", 1L)).thenReturn(false);
            when(doctorRepository.existsByEmailAndIdNot("binh.tran@clinic.com", 1L)).thenReturn(false);
            doNothing().when(doctorMapper).updateEntityFromRequest(doctorRequest, mockDoctor);
            when(doctorRepository.save(mockDoctor)).thenReturn(mockDoctor);
            when(doctorMapper.toResponse(mockDoctor)).thenReturn(mockResponse);

            // Act
            DoctorResponse result = doctorService.updateDoctor(1L, doctorRequest);

            // Assert
            assertNotNull(result);
            verify(doctorMapper, times(1)).updateEntityFromRequest(doctorRequest, mockDoctor);
            verify(doctorRepository, times(1)).save(mockDoctor);
        }

        @Test
        @DisplayName("Cập nhật bác sĩ thất bại - SĐT đã dùng bởi bác sĩ khác")
        void updateDoctor_Fail_PhoneConflictWithOtherDoctor() {
            // Arrange
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(doctorRepository.existsByPhoneAndIdNot("0912345678", 1L)).thenReturn(true);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> doctorService.updateDoctor(1L, doctorRequest));

            assertTrue(ex.getMessage().contains("Số điện thoại đã được sử dụng bởi bác sĩ khác"));
            verify(doctorRepository, never()).save(any());
        }
    }

    // =========================================================
    // DELETE DOCTOR TESTS
    // =========================================================
    @Nested
    @DisplayName("Xóa Bác sĩ (Delete Doctor)")
    class DeleteDoctorTests {

        @Test
        @DisplayName("Xóa bác sĩ thành công")
        void deleteDoctor_Success() {
            // Arrange
            when(doctorRepository.existsById(1L)).thenReturn(true);
            doNothing().when(doctorRepository).deleteById(1L);

            // Act
            assertDoesNotThrow(() -> doctorService.deleteDoctor(1L));

            // Assert
            verify(doctorRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("Xóa bác sĩ thất bại - Không tìm thấy ID")
        void deleteDoctor_Fail_NotFound() {
            // Arrange
            when(doctorRepository.existsById(999L)).thenReturn(false);

            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> doctorService.deleteDoctor(999L));

            assertTrue(ex.getMessage().contains("Không tìm thấy bác sĩ với ID: 999"));
            verify(doctorRepository, never()).deleteById(any());
        }
    }
}
