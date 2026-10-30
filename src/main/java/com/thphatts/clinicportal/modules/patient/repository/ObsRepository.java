package com.thphatts.clinicportal.modules.patient.repository;

import com.thphatts.clinicportal.modules.patient.entity.Obs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObsRepository extends JpaRepository<Obs, Long> {
}
