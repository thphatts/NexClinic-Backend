package com.thphatts.clinicportal.mapper;

import com.thphatts.clinicportal.dto.response.MedicalRecordResponse;
import com.thphatts.clinicportal.dto.response.PrescriptionItemResponse;
import com.thphatts.clinicportal.dto.response.PrescriptionResponse;
import com.thphatts.clinicportal.entity.MedicalRecord;
import com.thphatts.clinicportal.entity.Prescription;
import com.thphatts.clinicportal.entity.PrescriptionItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MedicalRecordMapper {

    @Mapping(source = "appointment.id", target = "appointmentId")
    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "patient.fullName", target = "patientName")
    @Mapping(source = "patient.phone", target = "patientPhone")
    @Mapping(source = "patient.citizenId", target = "citizenId")
    @Mapping(source = "doctor.id", target = "doctorId")
    @Mapping(source = "doctor.fullName", target = "doctorName")
    @Mapping(source = "doctor.specialization", target = "doctorSpecialization")
    MedicalRecordResponse toResponse(MedicalRecord medicalRecord);

    PrescriptionResponse toPrescriptionResponse(Prescription prescription);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    PrescriptionItemResponse toPrescriptionItemResponse(PrescriptionItem item);
}
