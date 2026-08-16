package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.dto.request.DoctorRequest;
import com.thphatts.clinicportal.dto.response.DoctorResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.entity.Doctor;
import com.thphatts.clinicportal.entity.User;
import com.thphatts.clinicportal.mapper.DoctorMapper;
import com.thphatts.clinicportal.repository.DoctorRepository;
import com.thphatts.clinicportal.repository.UserRepository;
import com.thphatts.clinicportal.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IDoctorService implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final DoctorMapper doctorMapper;

    @Override
    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        if (doctorRepository.existsByPhone(request.phone())) {
            throw new RuntimeException("Số điện thoại bác sĩ đã tồn tại: " + request.phone());
        }
        if (doctorRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email bác sĩ đã tồn tại: " + request.email());
        }

        Doctor doctor = doctorMapper.toEntity(request);

        if (request.userId() != null && !request.userId().isBlank()) {
            User user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản User với ID: " + request.userId()));
            doctor.setUser(user);
        }

        Doctor savedDoctor = doctorRepository.save(doctor);
        return doctorMapper.toResponse(savedDoctor);
    }

    @Override
    @Transactional
    @CacheEvict(value = "doctors", key = "#id")
    public DoctorResponse updateDoctor(Long id, DoctorRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ với ID: " + id));

        if (doctorRepository.existsByPhoneAndIdNot(request.phone(), id)) {
            throw new RuntimeException("Số điện thoại đã được sử dụng bởi bác sĩ khác: " + request.phone());
        }
        if (doctorRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new RuntimeException("Email đã được sử dụng bởi bác sĩ khác: " + request.email());
        }

        doctorMapper.updateEntityFromRequest(request, doctor);

        if (request.userId() != null && !request.userId().isBlank()) {
            User user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản User với ID: " + request.userId()));
            doctor.setUser(user);
        }

        Doctor updatedDoctor = doctorRepository.save(doctor);
        return doctorMapper.toResponse(updatedDoctor);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "doctors", key = "#id")
    public DoctorResponse getDoctorById(Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ với ID: " + id));
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DoctorResponse> getAllDoctors(String search, Pageable pageable) {
        Page<Doctor> doctorPage;
        if (search != null && !search.isBlank()) {
            doctorPage = doctorRepository.searchDoctors(search.trim(), pageable);
        } else {
            doctorPage = doctorRepository.findAll(pageable);
        }

        List<DoctorResponse> items = doctorPage.getContent().stream()
                .map(doctorMapper::toResponse)
                .toList();

        return new PagedResponse<>(
                items,
                doctorPage.getNumber() + 1,
                doctorPage.getSize(),
                doctorPage.getTotalElements(),
                doctorPage.getTotalPages(),
                doctorPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctorsBySpecialization(String specialization) {
        List<Doctor> doctors = doctorRepository.findBySpecialization(specialization);
        return doctors.stream()
                .map(doctorMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "doctors", key = "#id")
    public void deleteDoctor(Long id) {
        if (!doctorRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy bác sĩ với ID: " + id);
        }
        doctorRepository.deleteById(id);
    }
}
