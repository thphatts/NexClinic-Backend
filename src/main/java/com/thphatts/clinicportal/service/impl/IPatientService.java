package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.dto.request.PatientRequest;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.dto.response.PatientResponse;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.mapper.PatientMapper;
import com.thphatts.clinicportal.repository.PatientRepository;
import com.thphatts.clinicportal.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IPatientService implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        if (patientRepository.existsByCitizenId(request.getCitizenId())) {
            throw new RuntimeException("Số CCCD/CMND đã tồn tại trong hệ thống: " + request.getCitizenId());
        }
        if (patientRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Số điện thoại đã tồn tại trong hệ thống: " + request.getPhone());
        }

        Patient patient = patientMapper.toEntity(request);
        Patient savedPatient = patientRepository.save(patient);
        return patientMapper.toResponse(savedPatient);
    }

    @Override
    @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân với ID: " + id));

        if (patientRepository.existsByCitizenIdAndIdNot(request.getCitizenId(), id)) {
            throw new RuntimeException("Số CCCD/CMND đã được sử dụng bởi bệnh nhân khác: " + request.getCitizenId());
        }
        if (patientRepository.existsByPhoneAndIdNot(request.getPhone(), id)) {
            throw new RuntimeException("Số điện thoại đã được sử dụng bởi bệnh nhân khác: " + request.getPhone());
        }

        patientMapper.updateEntityFromRequest(request, patient);
        Patient updatedPatient = patientRepository.save(patient);
        return patientMapper.toResponse(updatedPatient);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân với ID: " + id));
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientByCitizenId(String citizenId) {
        Patient patient = patientRepository.findByCitizenId(citizenId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân với số CCCD/CMND: " + citizenId));
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PatientResponse> getAllPatients(String search, Pageable pageable) {
        Page<Patient> patientPage;
        if (search != null && !search.isBlank()) {
            patientPage = patientRepository.searchPatients(search.trim(), pageable);
        } else {
            patientPage = patientRepository.findAll(pageable);
        }

        List<PatientResponse> items = patientPage.getContent().stream()
                .map(patientMapper::toResponse)
                .toList();

        return new PagedResponse<>(
                items,
                patientPage.getNumber() + 1,
                patientPage.getSize(),
                patientPage.getTotalElements(),
                patientPage.getTotalPages(),
                patientPage.isLast()
        );
    }

    @Override
    @Transactional
    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy bệnh nhân với ID: " + id);
        }
        patientRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> getPatientsWithNPlusOne() {
        List<Patient> patients = patientRepository.findAllWithCheckUpOptimized();
        return patients.stream()
                .map(patientMapper::toResponse)
                .toList();
    }
}
