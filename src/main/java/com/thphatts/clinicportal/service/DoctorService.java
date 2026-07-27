package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.request.DoctorRequest;
import com.thphatts.clinicportal.dto.response.DoctorResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
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
