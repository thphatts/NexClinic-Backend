package com.thphatts.clinicportal.mapper;

import com.thphatts.clinicportal.dto.request.DoctorReviewRequest;
import com.thphatts.clinicportal.dto.response.DoctorReviewResponse;
import com.thphatts.clinicportal.entity.DoctorReview;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DoctorReviewMapper {
    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "doctor.id", target = "doctorId")
    @Mapping(source = "appointment.id", target = "appointmentId")
    @Mapping(source = "patient.fullName", target = "patientName")
    @Mapping(source = "doctor.fullName", target = "doctorName")
    @Mapping(target = "verified", expression = "java(doctorReview.getAppointment() != null)")
    DoctorReviewResponse toResponse(DoctorReview doctorReview);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "appointment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "visitCountSnapshot", ignore = true)
    DoctorReview toEntity(DoctorReviewRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "appointment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "visitCountSnapshot", ignore = true)
    void updateEntityFromRequest(DoctorReviewRequest request, @MappingTarget DoctorReview entity);
}

