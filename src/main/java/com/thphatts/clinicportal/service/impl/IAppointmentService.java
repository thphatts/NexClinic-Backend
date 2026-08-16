package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.dto.request.AppointmentRequest;
import com.thphatts.clinicportal.dto.response.AppointmentResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.entity.Appointment;
import com.thphatts.clinicportal.entity.enums.AppointmentStatus;
import com.thphatts.clinicportal.entity.Doctor;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.mapper.AppointmentMapper;
import com.thphatts.clinicportal.repository.AppointmentRepository;
import com.thphatts.clinicportal.repository.DoctorRepository;
import com.thphatts.clinicportal.repository.PatientRepository;
import com.thphatts.clinicportal.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IAppointmentService implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentMapper appointmentMapper;

    @Override
    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân với ID: " + request.patientId()));

        Doctor doctor = doctorRepository.findById(request.doctorId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ với ID: " + request.doctorId()));

        // Chống trùng ca khám của Bác sĩ trên cùng một ngày và khung giờ (Application Check)
        boolean isConflict = appointmentRepository.existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatusNot(
                request.doctorId(), request.appointmentDate(), request.timeSlot(), AppointmentStatus.CANCELLED
        );

        String doctorName = (doctor.getFullName() != null && doctor.getFullName().startsWith("Bác sĩ"))
                ? doctor.getFullName()
                : "Bác sĩ " + doctor.getFullName();

        if (isConflict) {
            throw new RuntimeException(doctorName + " đã có lịch hẹn khác vào khung giờ "
                    + request.timeSlot() + " ngày " + request.appointmentDate() + ". Vui lòng chọn khung giờ khác!");
        }

        Appointment appointment = appointmentMapper.toEntity(request);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setAmount(doctor.getConsultationFee());

        try {
            // Dùng saveAndFlush để đẩy INSERT SQL xuống DB ngay lập tức nhằm bắt lỗi Unique Partial Index Constraint nếu xảy ra Race Condition
            Appointment savedAppointment = appointmentRepository.saveAndFlush(appointment);
            return appointmentMapper.toResponse(savedAppointment);
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException(doctorName + " vừa có lịch hẹn mới được đăng ký vào khung giờ "
                    + request.timeSlot() + " ngày " + request.appointmentDate() + ". Vui lòng chọn khung giờ khác!", ex);
        }
    }

    @Override
    @Transactional
    public AppointmentResponse updateAppointmentStatus(Long id, AppointmentStatus status, String notes) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn với ID: " + id));

        appointment.setStatus(status);
        if (notes != null && !notes.isBlank()) {
            appointment.setNotes(notes);
        }

        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(updatedAppointment);
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn với ID: " + id));
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AppointmentResponse> filterAppointments(
            Long doctorId, Long patientId, AppointmentStatus status, LocalDate fromDate, LocalDate toDate, Pageable pageable
    ) {
        Page<Appointment> appointmentPage = appointmentRepository.filterAppointments(
                doctorId, patientId, status, fromDate, toDate, pageable
        );

        List<AppointmentResponse> items = appointmentPage.getContent().stream()
                .map(appointmentMapper::toResponse)
                .toList();

        return new PagedResponse<>(
                items,
                appointmentPage.getNumber() + 1,
                appointmentPage.getSize(),
                appointmentPage.getTotalElements(),
                appointmentPage.getTotalPages(),
                appointmentPage.isLast()
        );
    }

    @Override
    @Transactional
    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn với ID: " + id));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }
}
