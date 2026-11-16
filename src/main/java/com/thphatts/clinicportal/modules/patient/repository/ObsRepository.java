package com.thphatts.clinicportal.modules.patient.repository;

import com.thphatts.clinicportal.modules.patient.entity.Obs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ObsRepository extends JpaRepository<Obs, Long> {
    List<Obs> findByEncounter_Visit_IdOrderByEncounter_EncounterDatetimeAsc(Long visitId);

    @Query("select o from Obs o join fetch o.concept c left join fetch o.valueCoded " +
            "where o.encounter.visit.patient.id = :patientId and c.id = :conceptId " +
            "and o.valueNumeric is not null and o.encounter.encounterDatetime >= :fromTime " +
            "and o.encounter.encounterDatetime < :toTime " +
            "order by o.encounter.encounterDatetime asc")
    List<Obs> findNumericTrend(@Param("patientId") Long patientId, @Param("conceptId") Long conceptId,
                               @Param("fromTime") LocalDateTime fromTime, @Param("toTime") LocalDateTime toTime);
}
