package com.thphatts.clinicportal.service.ai.agent;

import com.thphatts.clinicportal.dto.request.AppointmentRequest;
import com.thphatts.clinicportal.dto.response.AiAgentActionResult;
import com.thphatts.clinicportal.dto.response.AppointmentResponse;
import com.thphatts.clinicportal.entity.Doctor;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.repository.DoctorRepository;
import com.thphatts.clinicportal.repository.PatientRepository;
import com.thphatts.clinicportal.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AiToolRegistry - Đăng ký và thực thi các Tool / Function Calling cho AI Agent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiToolRegistry {

    private final AppointmentService appointmentService;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    /**
     * Thực thi Tool dựa trên actionName và parameters truyền từ AI Agent
     */
    public AiAgentActionResult executeTool(String actionName, Map<String, Object> params) {
        log.info("[AI Agent Tool Execution] Thực thi Action [{}] với tham số: {}", actionName, params);

        try {
            return switch (actionName.toUpperCase()) {
                case "CREATE_APPOINTMENT" -> executeCreateAppointment(params);
                case "CANCEL_APPOINTMENT" -> executeCancelAppointment(params);
                case "SEARCH_DOCTOR" -> executeSearchDoctor(params);
                default -> AiAgentActionResult.failure(actionName, "Tool không được hỗ trợ: " + actionName);
            };
        } catch (Exception e) {
            log.error("Lỗi khi AI Agent thực thi Tool [{}]: {}", actionName, e.getMessage());
            return AiAgentActionResult.failure(actionName, "Thực thi thất bại: " + e.getMessage());
        }
    }

    private AiAgentActionResult executeCreateAppointment(Map<String, Object> params) {
        // 1. Resolve Doctor
        Long doctorId = parseLong(params.get("doctorId"));
        if (doctorId == null && params.get("doctorName") != null) {
            String doctorName = params.get("doctorName").toString();
            List<Doctor> doctors = doctorRepository.findAll();
            Optional<Doctor> matchedDoc = doctors.stream()
                    .filter(d -> d.getFullName().toLowerCase().contains(doctorName.toLowerCase()))
                    .findFirst();
            if (matchedDoc.isPresent()) {
                doctorId = matchedDoc.get().getId();
            }
        }

        if (doctorId == null) {
            // Pick first doctor if none specified
            List<Doctor> doctors = doctorRepository.findAll();
            if (!doctors.isEmpty()) {
                doctorId = doctors.get(0).getId();
            } else {
                return AiAgentActionResult.failure("CREATE_APPOINTMENT", "Không tìm thấy Bác sĩ phù hợp.");
            }
        }

        // 2. Resolve Patient
        Long patientId = parseLong(params.get("patientId"));
        if (patientId == null) {
            List<Patient> patients = patientRepository.findAll();
            if (!patients.isEmpty()) {
                patientId = patients.get(0).getId();
            } else {
                return AiAgentActionResult.failure("CREATE_APPOINTMENT", "Không tìm thấy Bệnh nhân. Vui lòng đăng ký tài khoản trước!");
            }
        }

        // 3. Resolve Date
        String dateStr = (String) params.get("appointmentDate");
        LocalDate appointmentDate;
        try {
            appointmentDate = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now().plusDays(1);
        } catch (Exception e) {
            appointmentDate = LocalDate.now().plusDays(1);
        }

        // 4. Resolve Time Slot
        String timeSlot = (String) params.get("timeSlot");
        if (timeSlot == null || timeSlot.isBlank()) {
            timeSlot = "09:00 - 10:00";
        }

        String reason = (String) params.getOrDefault("reason", "Khám bệnh tư vấn AI Agent");

        AppointmentRequest request = new AppointmentRequest(
                patientId,
                doctorId,
                appointmentDate,
                timeSlot,
                reason,
                "Tạo tự động bởi AI Agent Function Calling"
        );

        AppointmentResponse response = appointmentService.createAppointment(request);

        String message = String.format("Đã tạo thành công lịch hẹn #%d với Bác sĩ %s vào ngày %s (%s). Trạng thái: PENDING",
                response.id(), response.doctorName(), response.appointmentDate(), response.timeSlot());

        log.info("[AI Agent] {}", message);
        return AiAgentActionResult.success("CREATE_APPOINTMENT", message, response);
    }

    private AiAgentActionResult executeCancelAppointment(Map<String, Object> params) {
        Long appointmentId = parseLong(params.get("appointmentId"));
        if (appointmentId == null) {
            return AiAgentActionResult.failure("CANCEL_APPOINTMENT", "Vui lòng cung cấp Mã lịch hẹn (ID) cần hủy.");
        }

        appointmentService.cancelAppointment(appointmentId);
        String message = "Đã hủy thành công lịch hẹn #" + appointmentId;
        return AiAgentActionResult.success("CANCEL_APPOINTMENT", message, Map.of("appointmentId", appointmentId, "status", "CANCELLED"));
    }

    private AiAgentActionResult executeSearchDoctor(Map<String, Object> params) {
        String keyword = (String) params.getOrDefault("keyword", "");
        List<Doctor> doctors = doctorRepository.findAll();

        List<Map<String, Object>> result = doctors.stream()
                .filter(d -> keyword.isBlank() ||
                             d.getFullName().toLowerCase().contains(keyword.toLowerCase()) ||
                             d.getSpecialization().toLowerCase().contains(keyword.toLowerCase()))
                .map(d -> Map.<String, Object>of(
                        "id", d.getId(),
                        "fullName", d.getFullName(),
                        "specialization", d.getSpecialization(),
                        "experienceYears", d.getExperienceYears() != null ? d.getExperienceYears() : 0
                ))
                .toList();

        String message = String.format("Tìm thấy %d Bác sĩ phù hợp với từ khóa '%s'", result.size(), keyword);
        return AiAgentActionResult.success("SEARCH_DOCTOR", message, result);
    }

    private Long parseLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
