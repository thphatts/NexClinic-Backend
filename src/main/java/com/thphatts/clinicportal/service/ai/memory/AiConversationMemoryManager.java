package com.thphatts.clinicportal.service.ai.memory;

import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiConversationMemoryManager {

    private static final int MAX_HISTORY_SIZE = 6; // Lưu tối đa 6 lượt câu thoại gần nhất
    private final Map<String, Queue<String>> conversationMap = new ConcurrentHashMap<>();

    /**
     * Thêm một lượt hội thoại vào bộ nhớ theo sessionId
     */
    public void addMessage(String sessionId, String role, String content) {
        if (sessionId == null || sessionId.isBlank()) return;

        Queue<String> history = conversationMap.computeIfAbsent(sessionId, k -> new LinkedList<>());
        synchronized (history) {
            if (history.size() >= MAX_HISTORY_SIZE) {
                history.poll(); // Xóa tin nhắn cũ nhất khi vượt ngưỡng MAX
            }
            history.add(role + ": " + content);
        }
    }

    /**
     * Lấy lịch sử hội thoại dưới dạng chuỗi văn bản cho Prompt
     */
    public String getFormattedHistory(String sessionId) {
        if (sessionId == null || !conversationMap.containsKey(sessionId)) {
            return "Chưa có lịch sử hội thoại trước đó.";
        }

        Queue<String> history = conversationMap.get(sessionId);
        synchronized (history) {
            return String.join("\n", history);
        }
    }

    /**
     * Dọn dẹp bộ nhớ theo sessionId khi cần
     */
    public void clearHistory(String sessionId) {
        if (sessionId != null) {
            conversationMap.remove(sessionId);
        }
    }
}
