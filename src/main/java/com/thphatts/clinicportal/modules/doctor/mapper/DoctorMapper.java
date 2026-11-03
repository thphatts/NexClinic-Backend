package com.thphatts.clinicportal.modules.doctor.mapper;

import com.thphatts.clinicportal.modules.doctor.dto.DoctorRequest;
import com.thphatts.clinicportal.modules.doctor.dto.DoctorResponse;
import com.thphatts.clinicportal.modules.doctor.entity.Doctor;
import com.thphatts.clinicportal.modules.person.entity.Person;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {
    public Doctor toEntity(DoctorRequest request) {
        Person person = new Person();
        applyPerson(request, person);
        Doctor doctor = new Doctor();
        doctor.setPerson(person);
        doctor.setSpecialization(request.specialization());
        doctor.setDegree(request.degree());
        doctor.setExperienceYears(request.experienceYears() == null ? 0 : request.experienceYears());
        doctor.setConsultationFee(request.consultationFee());
        return doctor;
    }

    public DoctorResponse toResponse(Doctor doctor) {
        Person person = doctor.getPerson();
        return new DoctorResponse(doctor.getId(), person == null ? null : person.getFullName(),
                doctor.getSpecialization(), doctor.getDegree(), person == null ? null : person.getPhoneNumber(),
                person == null ? null : person.getEmail(), doctor.getExperienceYears(), doctor.getConsultationFee(),
                doctor.getUser() == null ? null : doctor.getUser().getId(), doctor.getCreatedAt(), doctor.getUpdatedAt());
    }

    public void updateEntityFromRequest(DoctorRequest request, Doctor doctor) {
        if (doctor.getPerson() == null) doctor.setPerson(new Person());
        applyPerson(request, doctor.getPerson());
        doctor.setSpecialization(request.specialization());
        doctor.setDegree(request.degree());
        doctor.setExperienceYears(request.experienceYears() == null ? 0 : request.experienceYears());
        doctor.setConsultationFee(request.consultationFee());
    }

    private void applyPerson(DoctorRequest request, Person person) {
        person.setFullName(request.fullName());
        person.setPhoneNumber(request.phone());
        person.setEmail(request.email());
    }
}
