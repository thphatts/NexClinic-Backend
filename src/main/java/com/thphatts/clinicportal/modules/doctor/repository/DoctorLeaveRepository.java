package com.thphatts.clinicportal.modules.doctor.repository;

import com.thphatts.clinicportal.modules.doctor.entity.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {

    boolean existsByDoctorIdAndLeaveDate(Long doctorId, LocalDate leaveDate);

    List<DoctorLeave> findByDoctorIdAndLeaveDateBetween(Long doctorId, LocalDate from, LocalDate to);
}