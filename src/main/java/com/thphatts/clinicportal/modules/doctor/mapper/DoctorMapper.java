package com.thphatts.clinicportal.modules.doctor.mapper;

import com.thphatts.clinicportal.modules.doctor.dto.DoctorRequest;
import com.thphatts.clinicportal.modules.doctor.dto.DoctorResponse;
import com.thphatts.clinicportal.modules.doctor.entity.Doctor;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DoctorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Doctor toEntity(DoctorRequest request);

    @Mapping(target = "userId", source = "user.id")
    DoctorResponse toResponse(Doctor doctor);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(DoctorRequest request, @MappingTarget Doctor doctor);
}
