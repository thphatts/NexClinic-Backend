package com.thphatts.clinicportal.modules.patient.repository;

import com.thphatts.clinicportal.modules.patient.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {
}
