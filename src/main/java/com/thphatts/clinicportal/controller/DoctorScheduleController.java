
package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.common.BaseController;
import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.dto.request.DoctorScheduleRequest;
import com.thphatts.clinicportal.dto.response.AvailableSlotResponse;
import com.thphatts.clinicportal.dto.response.DoctorScheduleResponse;
import com.thphatts.clinicportal.service.DoctorScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors/{doctorId}/schedules")
@RequiredArgsConstructor
public class DoctorScheduleController extends BaseController {

    private final DoctorScheduleService doctorScheduleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DoctorScheduleResponse> createSchedule(
            @PathVariable Long doctorId,
            @Valid @RequestBody DoctorScheduleRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return createdSuccessResponse(doctorScheduleService.createSchedule(doctorId, request, currentUser));
    }

    @GetMapping
    public ApiResponse<List<DoctorScheduleResponse>> getSchedules(@PathVariable Long doctorId) {
        return ApiResponse.success(doctorScheduleService.getSchedulesByDoctor(doctorId));
    }

    @DeleteMapping("/{scheduleId}")
    public ApiResponse<String> deleteSchedule(
            @PathVariable Long doctorId,
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        doctorScheduleService.deleteSchedule(doctorId, scheduleId, currentUser);
        return ApiResponse.success("Đã xóa lịch làm việc thành công");
    }

    @PostMapping("/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<String> addLeave(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        doctorScheduleService.addLeave(doctorId, date, reason, currentUser);
        return ApiResponse.success("Đã đăng ký ngày nghỉ thành công");
    }

    @GetMapping("/available-slots")
    public ApiResponse<List<AvailableSlotResponse>> getAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(doctorScheduleService.getAvailableSlots(doctorId, date));
    }
}