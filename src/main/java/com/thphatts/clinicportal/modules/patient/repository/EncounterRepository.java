package com.thphatts.clinicportal.modules.patient.repository;

import com.thphatts.clinicportal.modules.patient.entity.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EncounterRepository extends JpaRepository<Encounter, Long> {
    List<Encounter> findByVisit_Id(Long visitId);
    boolean existsByVisit_IdAndEncounterType(Long visitId, com.thphatts.clinicportal.modules.patient.entity.enums.EncounterType type);
}
