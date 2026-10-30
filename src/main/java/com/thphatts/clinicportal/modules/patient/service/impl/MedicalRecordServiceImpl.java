package com.thphatts.clinicportal.modules.patient.service.impl;

import com.thphatts.clinicportal.config.security.MedicalRecordSecurity;
import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.modules.patient.dto.MedicalRecordRequest;
import com.thphatts.clinicportal.modules.patient.dto.PrescriptionItemRequest;
import com.thphatts.clinicportal.modules.patient.dto.MedicalRecordResponse;
import com.thphatts.clinicportal.common.dto.PagedResponse;
import com.thphatts.clinicportal.modules.appointment.entity.Appointment;
import com.thphatts.clinicportal.modules.patient.entity.MedicalRecord;
import com.thphatts.clinicportal.modules.patient.entity.Patient;
import com.thphatts.clinicportal.modules.patient.entity.Prescription;
import com.thphatts.clinicportal.modules.patient.entity.PrescriptionItem;
import com.thphatts.clinicportal.modules.patient.entity.Visit;
import com.thphatts.clinicportal.modules.patient.entity.Encounter;
import com.thphatts.clinicportal.modules.patient.entity.Obs;
import com.thphatts.clinicportal.modules.patient.entity.enums.EncounterType;
import com.thphatts.clinicportal.modules.patient.entity.enums.VisitStatus;
import com.thphatts.clinicportal.modules.patient.dto.ObsRequest;
import com.thphatts.clinicportal.modules.doctor.entity.Doctor;
import com.thphatts.clinicportal.modules.concept.entity.Concept;
import com.thphatts.clinicportal.modules.concept.enums.DataType;
import com.thphatts.clinicportal.modules.concept.enums.ConceptClass;
import com.thphatts.clinicportal.modules.concept.repository.ConceptRepository;
import com.thphatts.clinicportal.modules.product.entity.Product;
import com.thphatts.clinicportal.modules.appointment.entity.enums.AppointmentStatus;
import com.thphatts.clinicportal.modules.patient.mapper.MedicalRecordMapper;
import com.thphatts.clinicportal.modules.product.repository.ProductRepository;
import com.thphatts.clinicportal.modules.appointment.repository.AppointmentRepository;
import com.thphatts.clinicportal.modules.patient.repository.MedicalRecordRepository;
import com.thphatts.clinicportal.modules.patient.repository.ObsRepository;
import com.thphatts.clinicportal.modules.patient.repository.VisitRepository;
import com.thphatts.clinicportal.modules.patient.repository.EncounterRepository;
import com.thphatts.clinicportal.modules.patient.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProductRepository productRepository;
    private final MedicalRecordMapper medicalRecordMapper;
    private final MedicalRecordSecurity medicalRecordSecurity;
    private final VisitRepository visitRepository;
    private final EncounterRepository encounterRepository;
    private final ObsRepository obsRepository;
    private final ConceptRepository conceptRepository;

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

        LocalDateTime encounterTime = savedRecord.getCreatedAt() != null
                ? savedRecord.getCreatedAt()
                : LocalDateTime.now();
        Visit visit = visitRepository.save(Visit.builder()
                .patient(savedRecord.getPatient())
                .startTime(encounterTime)
                .endTime(encounterTime)
                .status(VisitStatus.CLOSED)
                .build());
        Encounter encounter = encounterRepository.save(Encounter.builder()
                .visit(visit)
                .patient(savedRecord.getPatient())
                .doctor(savedRecord.getDoctor())
                .encounterType(EncounterType.CONSULTATION)
                .encounterDatetime(encounterTime)
                .build());

        List<Obs> observations = new ArrayList<>();
        addLegacyTextObservation(observations, encounter, savedRecord.getPatient(), "Clinical diagnosis", ConceptClass.DIAGNOSIS, savedRecord.getDiagnosis());
        addLegacyTextObservation(observations, encounter, savedRecord.getPatient(), "Symptoms", ConceptClass.SYMPTOM, savedRecord.getSymptoms());
        addLegacyTextObservation(observations, encounter, savedRecord.getPatient(), "Clinical notes", ConceptClass.DIAGNOSIS, savedRecord.getNotes());
        if (request.observations() != null) {
            request.observations().stream()
                    .map(obsRequest -> toObservation(obsRequest, encounter, savedRecord.getPatient()))
                    .forEach(observations::add);
        }
        obsRepository.saveAll(observations);

        return medicalRecordMapper.toResponse(savedRecord);
    }

    private void addLegacyTextObservation(
            List<Obs> observations,
            Encounter encounter,
            Patient patient,
            String conceptName,
            ConceptClass conceptClass,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }
        Concept concept = conceptRepository.findFirstByNameAndDatatypeAndConceptClass(
                        conceptName, DataType.TEXT, conceptClass
                )
                .orElseThrow(() -> new IllegalStateException("Thiếu concept mặc định: " + conceptName));
        observations.add(Obs.builder()
                .encounter(encounter)
                .person(patient)
                .concept(concept)
                .valueText(value)
                .build());
    }

    private Obs toObservation(ObsRequest request, Encounter encounter, Patient patient) {
        Concept concept = conceptRepository.findById(request.conceptId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy concept với ID: " + request.conceptId()));

        int valueCount = (request.valueNumeric() != null ? 1 : 0)
                + (request.valueText() != null ? 1 : 0)
                + (request.valueCodedId() != null ? 1 : 0)
                + (request.valueDatetime() != null ? 1 : 0)
                + (request.valueBoolean() != null ? 1 : 0);
        if (valueCount != 1) {
            throw new IllegalArgumentException("Mỗi Obs phải có đúng một giá trị");
        }

        Obs.ObsBuilder obs = Obs.builder()
                .encounter(encounter)
                .person(patient)
                .concept(concept);
        if (request.valueDatetime() != null) {
            obs.valueDatetime(request.valueDatetime());
        } else if (request.valueNumeric() != null && concept.getDatatype() == DataType.NUMERIC) {
            obs.valueNumeric(request.valueNumeric());
        } else if (request.valueText() != null && concept.getDatatype() == DataType.TEXT) {
            obs.valueText(request.valueText());
        } else if (request.valueCodedId() != null && concept.getDatatype() == DataType.CODED) {
            Concept codedValue = conceptRepository.findById(request.valueCodedId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy concept đáp án với ID: " + request.valueCodedId()));
            obs.valueCoded(codedValue);
        } else if (request.valueBoolean() != null && concept.getDatatype() == DataType.BOOLEAN) {
            obs.valueBoolean(request.valueBoolean());
        } else {
            throw new IllegalArgumentException("Kiểu giá trị Obs không khớp datatype của concept " + concept.getId());
        }
        return obs.build();
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
        if (currentUser == null || currentUser.getRole() != com.thphatts.clinicportal.modules.user.entity.enums.Role.ROLE_ADMIN) {
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
