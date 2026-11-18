package com.thphatts.clinicportal.modules.doctor.service;

import com.thphatts.clinicportal.config.security.DoctorScheduleSecurity;
import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.modules.appointment.dto.AvailableSlotResponse;
import com.thphatts.clinicportal.modules.appointment.entity.Appointment;
import com.thphatts.clinicportal.modules.appointment.entity.enums.AppointmentStatus;
import com.thphatts.clinicportal.modules.appointment.repository.AppointmentRepository;
import com.thphatts.clinicportal.modules.doctor.dto.DoctorScheduleRequest;
import com.thphatts.clinicportal.modules.doctor.dto.DoctorScheduleResponse;
import com.thphatts.clinicportal.modules.doctor.entity.Doctor;
import com.thphatts.clinicportal.modules.doctor.entity.DoctorSchedule;
import com.thphatts.clinicportal.modules.doctor.repository.DoctorLeaveRepository;
import com.thphatts.clinicportal.modules.doctor.repository.DoctorRepository;
import com.thphatts.clinicportal.modules.doctor.repository.DoctorScheduleRepository;
import com.thphatts.clinicportal.modules.doctor.service.impl.DoctorScheduleServiceImpl;
import com.thphatts.clinicportal.modules.user.entity.User;
import com.thphatts.clinicportal.modules.user.entity.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorScheduleServiceTest {

    @Mock private DoctorScheduleRepository scheduleRepository;
    @Mock private DoctorLeaveRepository leaveRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AppointmentRepository appointmentRepository;

    private final Doctor doctor = Doctor.builder().id(1L).build();
    private final UserPrincipal admin = new UserPrincipal(User.builder().id("admin").role(Role.ROLE_ADMIN).build());

    @Test
    void adminCanUpdateShiftWithoutChangingItsIdOrRejectingItsOwnTimeRange() {
        DoctorSchedule shift = shift(5L, "08:00", "10:00");
        DoctorSchedule adjacent = shift(6L, "09:30", "11:00");
        when(scheduleRepository.findByIdAndDoctorId(5L, 1L)).thenReturn(Optional.of(shift));
        when(scheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(1L, 1)).thenReturn(List.of(shift, adjacent));

        DoctorScheduleResponse updated = service().updateSchedule(1L, 5L, request("08:15", "09:30", 15), admin);

        assertEquals(5L, updated.id());
        assertEquals(LocalTime.of(8, 15), updated.startTime());
        assertEquals(LocalTime.of(9, 30), updated.endTime());
        assertEquals(15, updated.slotDurationMinutes());
    }

    @Test
    void addingShiftRejectsOverlappingHours() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(scheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(1L, 1))
                .thenReturn(List.of(shift(5L, "08:00", "10:00")));

        assertThrows(IllegalArgumentException.class,
                () -> service().createSchedule(1L, request("09:00", "11:00", 30), admin));
    }

    @Test
    void editingShiftRejectsDurationThatCannotFit() {
        DoctorSchedule shift = shift(5L, "08:00", "10:00");
        when(scheduleRepository.findByIdAndDoctorId(5L, 1L)).thenReturn(Optional.of(shift));

        assertThrows(IllegalArgumentException.class,
                () -> service().updateSchedule(1L, 5L, request("08:00", "08:20", 30), admin));
        assertEquals(LocalTime.of(10, 0), shift.getEndTime());
    }

    @Test
    void doctorCannotUpdateAnotherDoctorsShift() {
        UserPrincipal nonOwner = new UserPrincipal(User.builder().id("other-doctor").role(Role.ROLE_DOCTOR).build());
        when(doctorRepository.findUserIdByDoctorId(1L)).thenReturn(Optional.of("owner"));

        assertThrows(AccessDeniedException.class,
                () -> service().updateSchedule(1L, 5L, request("08:00", "10:00", 30), nonOwner));
        verifyNoInteractions(scheduleRepository);
    }

    @Test
    void lateShiftEndsAtMidnightBoundaryAndSkipsOverlappingBookings() {
        LocalDate date = LocalDate.now().plusDays(1);
        DoctorSchedule shift = shift(5L, "23:00", "23:55");
        shift.setSlotDurationMinutes(15);
        when(scheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(1L, date.getDayOfWeek().getValue()))
                .thenReturn(List.of(shift));
        when(appointmentRepository.findByDoctorIdAndAppointmentDate(1L, date))
                .thenReturn(List.of(Appointment.builder()
                        .timeSlot("23:00 - 23:30").status(AppointmentStatus.CONFIRMED).build()));

        List<String> slots = service().getAvailableSlots(1L, date).stream()
                .map(AvailableSlotResponse::timeSlotLabel).toList();

        assertEquals(List.of("23:30 - 23:45"), slots);
    }

    private DoctorScheduleService service() {
        return new DoctorScheduleServiceImpl(scheduleRepository, leaveRepository, doctorRepository,
                appointmentRepository, new DoctorScheduleSecurity(doctorRepository));
    }

    private DoctorSchedule shift(Long id, String start, String end) {
        return DoctorSchedule.builder().id(id).doctor(doctor).dayOfWeek(1)
                .startTime(LocalTime.parse(start)).endTime(LocalTime.parse(end)).build();
    }

    private DoctorScheduleRequest request(String start, String end, int duration) {
        return new DoctorScheduleRequest(1, LocalTime.parse(start), LocalTime.parse(end), duration);
    }
}
