package com.thphatts.clinicportal.modules.doctor.service.impl;

import com.thphatts.clinicportal.config.security.DoctorScheduleSecurity;
import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.modules.doctor.dto.DoctorScheduleRequest;
import com.thphatts.clinicportal.modules.appointment.dto.AvailableSlotResponse;
import com.thphatts.clinicportal.modules.doctor.dto.DoctorScheduleResponse;
import com.thphatts.clinicportal.modules.doctor.entity.Doctor;
import com.thphatts.clinicportal.modules.doctor.entity.DoctorLeave;
import com.thphatts.clinicportal.modules.doctor.entity.DoctorSchedule;
import com.thphatts.clinicportal.modules.appointment.entity.enums.AppointmentStatus;
import com.thphatts.clinicportal.modules.appointment.repository.AppointmentRepository;
import com.thphatts.clinicportal.modules.doctor.repository.DoctorLeaveRepository;
import com.thphatts.clinicportal.modules.doctor.repository.DoctorRepository;
import com.thphatts.clinicportal.modules.doctor.repository.DoctorScheduleRepository;
import com.thphatts.clinicportal.modules.doctor.service.DoctorScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern SLOT_LABEL = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d - ([01]\\d|2[0-3]):[0-5]\\d");

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

        DoctorSchedule schedule = DoctorSchedule.builder().doctor(doctor).build();
        applySchedule(schedule, request);
        return toResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorScheduleResponse> getSchedulesByDoctor(Long doctorId) {
        return scheduleRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public DoctorScheduleResponse updateSchedule(Long doctorId, Long scheduleId, DoctorScheduleRequest request,
                                                 UserPrincipal currentUser) {
        checkWriteAccess(doctorId, currentUser);
        DoctorSchedule schedule = getDoctorSchedule(doctorId, scheduleId);
        applySchedule(schedule, request);
        return toResponse(schedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long doctorId, Long scheduleId, UserPrincipal currentUser) {
        checkWriteAccess(doctorId, currentUser);
        scheduleRepository.delete(getDoctorSchedule(doctorId, scheduleId));
    }

    private DoctorSchedule getDoctorSchedule(Long doctorId, Long scheduleId) {
        return scheduleRepository.findByIdAndDoctorId(scheduleId, doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca trực của bác sĩ này"));
    }

    private void applySchedule(DoctorSchedule schedule, DoctorScheduleRequest request) {
        LocalTime start = request.startTime();
        LocalTime end = request.endTime();
        int duration = request.slotDurationMinutes() != null ? request.slotDurationMinutes() : 30;
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        if (duration > Duration.between(start, end).toMinutes()) {
            throw new IllegalArgumentException("Thời lượng mỗi lượt khám không được dài hơn ca trực");
        }
        if (Boolean.TRUE.equals(schedule.getActive()) && scheduleRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(schedule.getDoctor().getId(), request.dayOfWeek()).stream()
                .anyMatch(other -> !other.getId().equals(schedule.getId())
                        && start.isBefore(other.getEndTime()) && end.isAfter(other.getStartTime()))) {
            throw new IllegalArgumentException("Khung giờ này trùng với một ca trực khác của bác sĩ");
        }

        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        schedule.setSlotDurationMinutes(duration);
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

        List<String> bookedSlots = appointmentRepository
                .findByDoctorIdAndAppointmentDate(doctorId, date)
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .map(a -> a.getTimeSlot())
                .toList();

        List<AvailableSlotResponse> result = new ArrayList<>();
        LocalTime now = LocalTime.now();
        boolean isToday = date.isEqual(LocalDate.now());

        for (DoctorSchedule schedule : schedules) {
            LocalTime cursor = schedule.getStartTime();
            int duration = schedule.getSlotDurationMinutes();

            while (duration > 0 && duration <= Duration.between(cursor, schedule.getEndTime()).toMinutes()) {
                LocalTime slotEnd = cursor.plusMinutes(duration);
                String startLabel = cursor.format(HHMM);
                String endLabel = slotEnd.format(HHMM);

                boolean isPast = isToday && cursor.isBefore(now);
                if (!isPast && bookedSlots.stream().noneMatch(booked -> overlapsBookedSlot(booked, startLabel, endLabel))) {
                    result.add(new AvailableSlotResponse(date, cursor, slotEnd, startLabel + " - " + endLabel));
                }

                cursor = slotEnd;
            }
        }

        return result;
    }

    private boolean overlapsBookedSlot(String booked, String start, String end) {
        // Stored appointment labels use fixed-width HH:mm times, so text order is time order.
        return SLOT_LABEL.matcher(booked).matches()
                && start.compareTo(booked.substring(8)) < 0
                && end.compareTo(booked.substring(0, 5)) > 0;
    }

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
