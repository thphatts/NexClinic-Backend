package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.dto.response.PatientResponse;
import com.thphatts.clinicportal.entity.Checkup;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.repository.PatientRepository;
import com.thphatts.clinicportal.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IPatientService implements PatientService {
    @Autowired
    private PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public List<PatientResponse> getPatientsWithNPlusOne() {
        List<Patient> patients = patientRepository.findAllWithCheckUpOptimized();

        return patients.stream().map(p -> {
            List<String> diagnoses = p.getCheckups().stream()
                    .map(Checkup::getDiagnoses).toList();
            return new PatientResponse(p.getId(), p.getFullName(),diagnoses);
        }).toList();
    }
}
