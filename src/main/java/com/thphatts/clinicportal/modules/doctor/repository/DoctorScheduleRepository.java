package com.thphatts.clinicportal.modules.doctor.repository;

import com.thphatts.clinicportal.modules.doctor.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndActiveTrue(Long doctorId, Integer dayOfWeek);

    List<DoctorSchedule> findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(Long doctorId);

    Optional<DoctorSchedule> findByIdAndDoctorId(Long id, Long doctorId);
}
