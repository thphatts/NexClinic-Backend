package com.thphatts.clinicportal.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        String message,

        String sessionId,

        String context
) {
}
