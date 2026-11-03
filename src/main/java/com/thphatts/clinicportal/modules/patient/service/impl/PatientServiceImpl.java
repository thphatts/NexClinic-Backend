package com.thphatts.clinicportal.modules.patient.service.impl;

import com.thphatts.clinicportal.common.dto.PagedResponse;
import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.modules.patient.dto.PatientRequest;
import com.thphatts.clinicportal.modules.patient.dto.PatientResponse;
import com.thphatts.clinicportal.modules.patient.entity.Patient;
import com.thphatts.clinicportal.modules.patient.mapper.PatientMapper;
import com.thphatts.clinicportal.modules.patient.repository.PatientRepository;
import com.thphatts.clinicportal.modules.patient.service.PatientService;
import com.thphatts.clinicportal.modules.person.entity.Person;
import com.thphatts.clinicportal.modules.person.repository.PersonRepository;
import com.thphatts.clinicportal.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    private final PatientMapper patientMapper;

    @Override @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        String phone = normalizePhone(request.getPhone());
        Person person = personRepository.findByPhoneNumber(phone)
                .orElseGet(() -> request.getCitizenId() == null ? null :
                        personRepository.findByCitizenId(request.getCitizenId().trim()).orElse(null));
        if (person != null && patientRepository.findByPerson_Id(person.getId()).isPresent())
            throw new IllegalArgumentException("Người này đã có hồ sơ bệnh nhân.");
        if (person == null) person = new Person();
        if (request.getCitizenId() != null && person.getCitizenId() != null
                && !person.getCitizenId().equals(request.getCitizenId().trim()))
            throw new IllegalArgumentException("Số CCCD/CMND không khớp với hồ sơ đã có.");
        person.setPhoneNumber(phone);
        patientMapper.updateEntityFromRequest(request, Patient.builder().person(person).build());
        Person savedPerson = personRepository.save(person);
        Patient patient = new Patient();
        patient.setPerson(savedPerson);
        patient.setPatientCode("PT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân với ID: " + id));
        String phone = normalizePhone(request.getPhone());
        Person person = patient.getPerson();
        if (personRepository.findByPhoneNumber(phone).filter(p -> !p.getId().equals(person.getId())).isPresent())
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng bởi người khác.");
        if (request.getCitizenId() != null && personRepository.findByCitizenId(request.getCitizenId().trim())
                .filter(p -> !p.getId().equals(person.getId())).isPresent())
            throw new IllegalArgumentException("Số CCCD/CMND đã được sử dụng bởi người khác.");
        patientMapper.updateEntityFromRequest(request, patient);
        personRepository.save(person);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        return patientMapper.toResponse(patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân với ID: " + id)));
    }

    @Override @Transactional(readOnly = true)
    public PatientResponse getPatientByCitizenId(String citizenId) {
        Person person = personRepository.findByCitizenId(citizenId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân với CCCD/CMND đã cung cấp."));
        Patient patient = patientRepository.findByPerson_Id(person.getId())
                .orElseThrow(() -> new IllegalArgumentException("Người này chưa có hồ sơ bệnh nhân."));
        return patientMapper.toResponse(patient);
    }

    @Override @Transactional(readOnly = true)
    public PatientResponse getMyPatientProfile(UserPrincipal currentUser) {
        if (currentUser == null || currentUser.getUserId() == null)
            throw new IllegalArgumentException("Phiên làm việc không hợp lệ.");
        var user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại."));
        Patient patient = patientRepository.findByPerson_Id(user.getPerson().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ bệnh nhân cho tài khoản này."));
        return patientMapper.toResponse(patient);
    }

    @Override @Transactional(readOnly = true)
    public PagedResponse<PatientResponse> getAllPatients(String search, Pageable pageable) {
        Page<Patient> page = search == null || search.isBlank()
                ? patientRepository.findAll(pageable) : patientRepository.searchPatients(search.trim(), pageable);
        return new PagedResponse<>(page.getContent().stream().map(patientMapper::toResponse).toList(),
                page.getNumber() + 1, page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override @Transactional
    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) throw new IllegalArgumentException("Không tìm thấy bệnh nhân với ID: " + id);
        patientRepository.deleteById(id);
    }

    @Override @Transactional(readOnly = true)
    public List<PatientResponse> getPatientsWithNPlusOne() {
        return patientRepository.findAllWithCheckUpOptimized().stream().map(patientMapper::toResponse).toList();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("Số điện thoại không được để trống.");
        return phone.replaceAll("[\\s()-]", "").trim();
    }
}
