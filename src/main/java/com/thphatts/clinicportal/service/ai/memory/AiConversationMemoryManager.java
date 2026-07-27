package com.thphatts.clinicportal.service.ai.memory;

import com.thphatts.clinicportal.entity.AiChatMessage;
import com.thphatts.clinicportal.repository.AiChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiConversationMemoryManager {

    private final AiChatMessageRepository chatMessageRepository;

    /**
     * Thêm một lượt hội thoại vào bộ nhớ PostgreSQL bền vững
     */
    @Transactional
    public void addMessage(String sessionId, String role, String content) {
        if (sessionId == null || sessionId.isBlank()) return;

        AiChatMessage message = AiChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .build();

        chatMessageRepository.save(message);
        log.debug("💾 Đã lưu tin nhắn AI vào PostgreSQL DB cho session [{}]", sessionId);
    }

    /**
     * Lấy lịch sử hội thoại dưới dạng chuỗi văn bản từ PostgreSQL DB cho Prompt
     */
    @Transactional(readOnly = true)
    public String getFormattedHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "Chưa có lịch sử hội thoại trước đó.";
        }

        List<AiChatMessage> history = chatMessageRepository.findTop6BySessionIdOrderByCreatedAtAsc(sessionId);
        if (history.isEmpty()) {
            return "Chưa có lịch sử hội thoại trước đó.";
        }

        return history.stream()
                .map(m -> m.getRole() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Dọn dẹp bộ nhớ hội thoại theo sessionId khi cần
     */
    @Transactional
    public void clearHistory(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            chatMessageRepository.deleteBySessionId(sessionId);
            log.info("🗑️ Đã dọn dẹp toàn bộ lịch sử chat của session [{}] trong DB", sessionId);
        }
    }
}
