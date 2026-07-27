package com.thphatts.clinicportal.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AiChatResponse(
        String reply,
        String sessionId,
        List<String> suggestedActions,
        String disclaimer,
        LocalDateTime timestamp
) {
}
