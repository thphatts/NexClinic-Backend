package com.thphatts.clinicportal.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        String message,

        String sessionId,

        String context
) {
}
