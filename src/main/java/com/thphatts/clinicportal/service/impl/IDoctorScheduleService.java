
package com.thphatts.clinicportal.service.impl;

import com.thphatts.clinicportal.config.security.DoctorScheduleSecurity;
import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.dto.request.DoctorScheduleRequest;
import com.thphatts.clinicportal.dto.response.AvailableSlotResponse;
import com.thphatts.clinicportal.dto.response.DoctorScheduleResponse;
import com.thphatts.clinicportal.entity.Doctor;
import com.thphatts.clinicportal.entity.DoctorLeave;
import com.thphatts.clinicportal.entity.DoctorSchedule;
import com.thphatts.clinicportal.entity.enums.AppointmentStatus;
import com.thphatts.clinicportal.repository.AppointmentRepository;
import com.thphatts.clinicportal.repository.DoctorLeaveRepository;
import com.thphatts.clinicportal.repository.DoctorRepository;
import com.thphatts.clinicportal.repository.DoctorScheduleRepository;
import com.thphatts.clinicportal.service.DoctorScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IDoctorScheduleService implements DoctorScheduleService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final DoctorScheduleRepository scheduleRepository;
    private final DoctorLeaveRepository leaveRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorScheduleSecurity doctorScheduleSecurity;

    @Override
    @Transactional
    public DoctorScheduleResponse createSchedule(Long doctorId, DoctorScheduleRequest request, UserPrincipal currentUser) {
        checkWriteAccess(doctorId, currentUser);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ với ID: " + doctorId));

        if (!request.endTime().isAfter(request.startTime())) {
            throw new RuntimeException("Giờ kết thúc phải sau giờ bắt đầu");
        }

        DoctorSchedule schedule = DoctorSchedule.builder()
                .doctor(doctor)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .slotDurationMinutes(request.slotDurationMinutes() != null ? request.slotDurationMinutes() : 30)
                .active(true)
                .build();

        DoctorSchedule saved = scheduleRepository.save(schedule);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorScheduleResponse> getSchedulesByDoctor(Long doctorId) {
        return scheduleRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void deleteSchedule(Long doctorId, Long scheduleId, UserPrincipal currentUser) {
        checkWriteAccess(doctorId, currentUser);

        DoctorSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch làm việc với ID: " + scheduleId));
        if (!schedule.getDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Lịch làm việc không thuộc về bác sĩ này");
        }
        scheduleRepository.delete(schedule);
    }

    @Override
    @Transactional
    public void addLeave(Long doctorId, LocalDate date, String reason, UserPrincipal currentUser) {
        checkWriteAccess(doctorId, currentUser);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ với ID: " + doctorId));

        if (leaveRepository.existsByDoctorIdAndLeaveDate(doctorId, date)) {
            throw new RuntimeException("Bác sĩ đã đăng ký nghỉ vào ngày này rồi");
        }

        DoctorLeave leave = DoctorLeave.builder()
                .doctor(doctor)
                .leaveDate(date)
                .reason(reason)
                .build();
        leaveRepository.save(leave);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new RuntimeException("Không thể tra cứu lịch trống cho ngày trong quá khứ");
        }

        if (leaveRepository.existsByDoctorIdAndLeaveDate(doctorId, date)) {
            return List.of();
        }

        int dayOfWeek = date.getDayOfWeek().getValue();
        List<DoctorSchedule> schedules = scheduleRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, dayOfWeek);

        if (schedules.isEmpty()) {
            return List.of();
        }

        Set<String> bookedSlots = appointmentRepository
                .findByDoctorIdAndAppointmentDate(doctorId, date)
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .map(a -> a.getTimeSlot())
                .collect(Collectors.toSet());

        List<AvailableSlotResponse> result = new ArrayList<>();
        LocalTime now = LocalTime.now();
        boolean isToday = date.isEqual(LocalDate.now());

        for (DoctorSchedule schedule : schedules) {
            LocalTime cursor = schedule.getStartTime();
            int duration = schedule.getSlotDurationMinutes();

            while (cursor.plusMinutes(duration).compareTo(schedule.getEndTime()) <= 0) {
                LocalTime slotEnd = cursor.plusMinutes(duration);
                String label = cursor.format(HHMM) + " - " + slotEnd.format(HHMM);

                boolean isPast = isToday && cursor.isBefore(now);
                boolean isBooked = bookedSlots.contains(label);

                if (!isPast && !isBooked) {
                    result.add(new AvailableSlotResponse(date, cursor, slotEnd, label));
                }

                cursor = slotEnd;
            }
        }

        return result;
    }

    /**
     * Kiểm tra quyền ghi (create/delete schedule, add leave) — giống pattern checkReadAccess
     * bên IMedicalRecordService.
     */
    private void checkWriteAccess(Long doctorId, UserPrincipal currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Vui lòng đăng nhập để thực hiện thao tác");
        }
        if (!doctorScheduleSecurity.isOwner(doctorId, currentUser)) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên lịch làm việc của bác sĩ này");
        }
    }

    private DoctorScheduleResponse toResponse(DoctorSchedule s) {
        return new DoctorScheduleResponse(
                s.getId(), s.getDoctor().getId(), s.getDayOfWeek(),
                s.getStartTime(), s.getEndTime(), s.getSlotDurationMinutes(), s.getActive()
        );
    }
}