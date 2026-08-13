package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.ChatRoom;
import com.thphatts.clinicportal.entity.enums.ChatRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByDoctorIdOrderByUpdatedAtDesc(Long doctorId);

    List<ChatRoom> findByPatientIdOrderByUpdatedAtDesc(Long patientId);

    Optional<ChatRoom> findByDoctorIdAndPatientId(Long doctorId, Long patientId);

    Optional<ChatRoom> findByAppointmentId(Long appointmentId);

    boolean existsByDoctorIdAndPatientIdAndStatus(Long doctorId, Long patientId, ChatRoomStatus status);
}
