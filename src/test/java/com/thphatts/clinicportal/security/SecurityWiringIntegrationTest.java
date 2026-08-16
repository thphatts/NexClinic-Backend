package com.thphatts.clinicportal.security;

import com.thphatts.clinicportal.controller.DoctorScheduleController;
import com.thphatts.clinicportal.controller.MedicalRecordController;
import com.thphatts.clinicportal.config.security.CustomUserDetailsService;
import com.thphatts.clinicportal.config.security.JwtAuthenticationFilter;
import com.thphatts.clinicportal.config.security.JwtTokenProvider;
import com.thphatts.clinicportal.config.security.SecurityConfig;
import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.entity.User;
import com.thphatts.clinicportal.entity.enums.Role;
import com.thphatts.clinicportal.repository.UserRepository;
import com.thphatts.clinicportal.service.DoctorScheduleService;
import com.thphatts.clinicportal.service.MedicalRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

@WebMvcTest(controllers = {MedicalRecordController.class, DoctorScheduleController.class})
@Import({com.thphatts.clinicportal.config.security.SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class, CustomUserDetailsService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=c2VjcmV0S2V5Rm9ySnd0VG9rZW5HZW5lcmF0aW9uVGVzdGluZ1B1cnBvc2VzT25seTEyMzQ1Njc4OTA=",
        "app.jwt.expiration-ms=86400000"
})
@DisplayName("Integration Tests: Spring Security & JWT Filter Wiring")
class SecurityWiringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MedicalRecordService medicalRecordService;

    @MockBean
    private DoctorScheduleService doctorScheduleService;

    @MockBean
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    private User doctorUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        doctorUser = User.builder()
                .id("doctor-uuid-1")
                .username("dr.smith")
                .email("smith@clinic.com")
                .password("encoded_pass")
                .role(Role.ROLE_DOCTOR)
                .build();

        jwtToken = jwtTokenProvider.generateToken(doctorUser);
    }

    @Test
    @DisplayName("CustomUserDetailsService trả về đúng kiểu UserPrincipal (không gây Type Mismatch)")
    void customUserDetailsService_ReturnsUserPrincipal() {
        when(userRepository.findByUsername("dr.smith")).thenReturn(Optional.of(doctorUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("dr.smith");

        assertNotNull(userDetails);
        assertInstanceOf(UserPrincipal.class, userDetails);
        UserPrincipal userPrincipal = (UserPrincipal) userDetails;
        org.junit.jupiter.api.Assertions.assertEquals("doctor-uuid-1", userPrincipal.getUserId());
        org.junit.jupiter.api.Assertions.assertEquals(Role.ROLE_DOCTOR, userPrincipal.getRole());
    }

    @Test
    @DisplayName("Request kèm JWT Token thật inject thành công UserPrincipal vào @AuthenticationPrincipal của MedicalRecordController")
    void getMedicalRecordsByDoctorId_WithValidJwt_ShouldPassSecurityAndBindUserPrincipal() throws Exception {
        when(userRepository.findByUsername("dr.smith")).thenReturn(Optional.of(doctorUser));
        when(medicalRecordService.getMedicalRecordsByDoctorId(eq(1L), any(UserPrincipal.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/medical-records/doctor/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Request kèm JWT Token thật lấy danh sách lịch làm việc của Bác sĩ thành công")
    void getDoctorSchedules_WithValidJwt_ShouldPassSecurity() throws Exception {
        when(userRepository.findByUsername("dr.smith")).thenReturn(Optional.of(doctorUser));
        when(doctorScheduleService.getSchedulesByDoctor(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/doctors/1/schedules")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }
}
