package com.thphatts.clinicportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotNull(message = "roomId không được để trống")
    private Long roomId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;
}
