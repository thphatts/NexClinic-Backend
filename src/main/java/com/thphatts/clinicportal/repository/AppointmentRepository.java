package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.Appointment;
import com.thphatts.clinicportal.entity.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Chống trùng ca khám của cùng một Bác sĩ (trừ những lịch hẹn bị CANCELLED)
    boolean existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatusNot(
            Long doctorId, LocalDate appointmentDate, String timeSlot, AppointmentStatus status
    );

    // Chống trùng ca khám của cùng một Bác sĩ khi cập nhật (loại trừ chính appointment ID)
    boolean existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatusNotAndIdNot(
            Long doctorId, LocalDate appointmentDate, String timeSlot, AppointmentStatus status, Long id
    );

    @EntityGraph(attributePaths = {"patient", "doctor"})
    @Query("SELECT a FROM Appointment a WHERE " +
           "(:doctorId IS NULL OR a.doctor.id = :doctorId) AND " +
           "(:patientId IS NULL OR a.patient.id = :patientId) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:fromDate IS NULL OR a.appointmentDate >= :fromDate) AND " +
           "(:toDate IS NULL OR a.appointmentDate <= :toDate)")
    Page<Appointment> filterAppointments(
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("status") AppointmentStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.patient.id = :patientId AND a.status = 'COMPLETED'")
    int visitsCountSnapshot(@Param("patientId") Long patientId, @Param("doctorId") Long doctorId);


    @EntityGraph(attributePaths = {"patient", "doctor"})
    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate appointmentDate);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a WHERE a.doctor.user.id = :doctorUserId AND a.patient.id = :patientId")
    boolean existsByDoctorUserIdAndPatientId(@Param("doctorUserId") String doctorUserId, @Param("patientId") Long patientId);
}

