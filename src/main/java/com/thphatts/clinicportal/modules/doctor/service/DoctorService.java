package com.thphatts.clinicportal.modules.doctor.service;

import com.thphatts.clinicportal.modules.doctor.dto.DoctorRequest;
import com.thphatts.clinicportal.modules.doctor.dto.DoctorResponse;
import com.thphatts.clinicportal.common.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DoctorService {

    DoctorResponse createDoctor(DoctorRequest request);

    DoctorResponse updateDoctor(Long id, DoctorRequest request);

    DoctorResponse getDoctorById(Long id);

    PagedResponse<DoctorResponse> getAllDoctors(String search, Pageable pageable);

    List<DoctorResponse> getDoctorsBySpecialization(String specialization);

    void deleteDoctor(Long id);
}
