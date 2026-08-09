package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.config.security.MedicalRecordSecurity;
import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.dto.request.MedicalRecordRequest;
import com.thphatts.clinicportal.dto.request.PrescriptionItemRequest;
import com.thphatts.clinicportal.dto.response.MedicalRecordResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.entity.*;
import com.thphatts.clinicportal.entity.enums.AppointmentStatus;
import com.thphatts.clinicportal.mapper.MedicalRecordMapper;
import com.thphatts.clinicportal.repository.*;
import com.thphatts.clinicportal.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IMedicalRecordService implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProductRepository productRepository;
    private final MedicalRecordMapper medicalRecordMapper;
    private final MedicalRecordSecurity medicalRecordSecurity;

    @Override
    @Transactional
    public MedicalRecordResponse createMedicalRecord(MedicalRecordRequest request) {
        Appointment appointment = appointmentRepository.findById(request.appointmentId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn với ID: " + request.appointmentId()));

        if (medicalRecordRepository.existsByAppointmentId(request.appointmentId())) {
            throw new RuntimeException("Hồ sơ bệnh án cho lịch hẹn này đã tồn tại!");
        }

        MedicalRecord medicalRecord = MedicalRecord.builder()
                .appointment(appointment)
                .patient(appointment.getPatient())
                .doctor(appointment.getDoctor())
                .diagnosis(request.diagnosis())
                .symptoms(request.symptoms())
                .notes(request.notes())
                .reexaminationDate(request.reexaminationDate())
                .build();

        // Xử lý kê đơn thuốc nếu có truyền danh sách thuốc
        if (request.prescription() != null && request.prescription().items() != null && !request.prescription().items().isEmpty()) {
            Prescription prescription = Prescription.builder()
                    .medicalRecord(medicalRecord)
                    .notes(request.prescription().notes())
                    .build();

            List<PrescriptionItem> items = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (PrescriptionItemRequest itemReq : request.prescription().items()) {
                Product product = productRepository.findById(itemReq.productId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc/sản phẩm với ID: " + itemReq.productId()));

                BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
                BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));

                totalAmount = totalAmount.add(totalPrice);

                PrescriptionItem item = PrescriptionItem.builder()
                        .prescription(prescription)
                        .product(product)
                        .quantity(itemReq.quantity())
                        .dosage(itemReq.dosage())
                        .unitPrice(unitPrice)
                        .totalPrice(totalPrice)
                        .build();

                items.add(item);
            }

            prescription.setItems(items);
            prescription.setTotalAmount(totalAmount);
            medicalRecord.setPrescription(prescription);
        }

        // Tự động chuyển lịch hẹn thành COMPLETED
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        MedicalRecord savedRecord = medicalRecordRepository.save(medicalRecord);
        return medicalRecordMapper.toResponse(savedRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponse getMedicalRecordById(Long id, UserPrincipal currentUser) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ bệnh án với ID: " + id));
        checkReadAccess(medicalRecord, currentUser);
        return medicalRecordMapper.toResponse(medicalRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponse getMedicalRecordByAppointmentId(Long appointmentId, UserPrincipal currentUser) {
        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ bệnh án cho lịch hẹn ID: " + appointmentId));
        checkReadAccess(medicalRecord, currentUser);
        return medicalRecordMapper.toResponse(medicalRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getMedicalRecordsByPatientId(Long patientId, UserPrincipal currentUser) {
        if (!medicalRecordSecurity.canAccessPatientRecords(patientId, currentUser)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xem danh sách hồ sơ bệnh án của bệnh nhân này");
        }
        List<MedicalRecord> list = medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        return list.stream().map(medicalRecordMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getMedicalRecordsByDoctorId(Long doctorId, UserPrincipal currentUser) {
        if (!medicalRecordSecurity.canAccessDoctorRecords(doctorId, currentUser)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xem danh sách hồ sơ bệnh án này");
        }
        List<MedicalRecord> list = medicalRecordRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
        return list.stream().map(medicalRecordMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MedicalRecordResponse> getAllMedicalRecords(Pageable pageable, UserPrincipal currentUser) {
        if (currentUser == null || currentUser.getRole() != com.thphatts.clinicportal.entity.enums.Role.ROLE_ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ ADMIN mới có quyền xem toàn bộ danh sách hồ sơ bệnh án");
        }
        Page<MedicalRecord> page = medicalRecordRepository.findAll(pageable);
        List<MedicalRecordResponse> items = page.getContent().stream()
                .map(medicalRecordMapper::toResponse)
                .toList();

        return new PagedResponse<>(
                items,
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Override
    public void checkReadAccess(MedicalRecord record, UserPrincipal currentUser) {
        if (record == null) {
            throw new RuntimeException("Hồ sơ bệnh án không tồn tại");
        }
        if (currentUser == null) {
            throw new org.springframework.security.access.AccessDeniedException("Vui lòng đăng nhập để thực hiện thao tác");
        }
        if (!medicalRecordSecurity.canAccessRecord(record.getId(), currentUser)) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền truy cập hồ sơ bệnh án này");
        }
    }

}
