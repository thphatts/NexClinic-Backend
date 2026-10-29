package com.thphatts.clinicportal.modules.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateChatRoomRequest {

    @NotNull(message = "doctorId không được để trống")
    private Long doctorId;

    @NotNull(message = "patientId không được để trống")
    private Long patientId;

    private Long appointmentId;
}
