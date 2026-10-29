package com.thphatts.clinicportal.modules.patient.mapper;

import com.thphatts.clinicportal.modules.patient.dto.PatientRequest;
import com.thphatts.clinicportal.modules.patient.dto.PatientResponse;
import com.thphatts.clinicportal.modules.appointment.entity.Checkup;
import com.thphatts.clinicportal.modules.patient.entity.Patient;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PatientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "checkups", ignore = true)
    Patient toEntity(PatientRequest request);

    @Mapping(target = "diagnoses", source = "checkups", qualifiedByName = "mapCheckupsToDiagnoses")
    PatientResponse toResponse(Patient patient);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "checkups", ignore = true)
    void updateEntityFromRequest(PatientRequest request, @MappingTarget Patient patient);

    @Named("mapCheckupsToDiagnoses")
    default List<String> mapCheckupsToDiagnoses(List<Checkup> checkups) {
        if (checkups == null || checkups.isEmpty()) {
            return Collections.emptyList();
        }
        return checkups.stream()
                .map(Checkup::getDiagnoses)
                .filter(d -> d != null && !d.isBlank())
                .toList();
    }
}
