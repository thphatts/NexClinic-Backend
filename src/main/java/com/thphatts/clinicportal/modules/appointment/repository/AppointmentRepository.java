package com.thphatts.clinicportal.modules.appointment.repository;

import com.thphatts.clinicportal.modules.appointment.entity.Appointment;
import com.thphatts.clinicportal.modules.appointment.entity.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    // Chống trùng ca khám của cùng một Bác sĩ (trừ những lịch hẹn bị CANCELLED)
    boolean existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatusNot(
            Long doctorId, LocalDate appointmentDate, String timeSlot, AppointmentStatus status
    );

    // Chống trùng ca khám của cùng một Bác sĩ khi cập nhật (loại trừ chính appointment ID)
    boolean existsByDoctorIdAndAppointmentDateAndTimeSlotAndStatusNotAndIdNot(
            Long doctorId, LocalDate appointmentDate, String timeSlot, AppointmentStatus status, Long id
    );

    @EntityGraph(attributePaths = {"patient", "doctor"})
    Page<Appointment> findAll(Specification<Appointment> specification, Pageable pageable);

    default Page<Appointment> filterAppointments(Long doctorId, Long patientId, AppointmentStatus status,
                                                 LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return findAll((root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (doctorId != null) {
                predicates.add(builder.equal(root.get("doctor").get("id"), doctorId));
            }
            if (patientId != null) {
                predicates.add(builder.equal(root.get("patient").get("id"), patientId));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.<LocalDate>get("appointmentDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(builder.lessThanOrEqualTo(root.<LocalDate>get("appointmentDate"), toDate));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.patient.id = :patientId AND a.status = 'COMPLETED'")
    int visitsCountSnapshot(@Param("patientId") Long patientId, @Param("doctorId") Long doctorId);


    @EntityGraph(attributePaths = {"patient", "doctor"})
    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate appointmentDate);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a WHERE a.doctor.user.id = :doctorUserId AND a.patient.id = :patientId")
    boolean existsByDoctorUserIdAndPatientId(@Param("doctorUserId") String doctorUserId, @Param("patientId") Long patientId);
}

