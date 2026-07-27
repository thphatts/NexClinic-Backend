package com.thphatts.clinicportal.dto.response;

import java.time.LocalDateTime;

/**
 * Kết quả thực thi tác vụ (Tool Action) của AI Agent
 */
public record AiAgentActionResult(
        boolean success,
        String actionType,
        String message,
        Object resultData,
        LocalDateTime executedAt
) {
    public static AiAgentActionResult success(String actionType, String message, Object resultData) {
        return new AiAgentActionResult(true, actionType, message, resultData, LocalDateTime.now());
    }

    public static AiAgentActionResult failure(String actionType, String message) {
        return new AiAgentActionResult(false, actionType, message, null, LocalDateTime.now());
    }
}
