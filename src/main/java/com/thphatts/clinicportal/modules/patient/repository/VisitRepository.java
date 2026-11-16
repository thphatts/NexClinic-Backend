package com.thphatts.clinicportal.modules.patient.repository;

import com.thphatts.clinicportal.modules.patient.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import com.thphatts.clinicportal.modules.patient.entity.enums.VisitStatus;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    boolean existsByPatient_IdAndStatus(Long patientId, VisitStatus status);
    boolean existsByAppointmentId(Long appointmentId);
    List<Visit> findByStatusOrderByStartTimeDesc(VisitStatus status);
    List<Visit> findByPatient_IdOrderByStartTimeDesc(Long patientId);
}
