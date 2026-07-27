package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.dto.response.PatientResponse;

import java.util.List;

public interface PatientService {

    List<PatientResponse> getPatientsWithNPlusOne();
}
