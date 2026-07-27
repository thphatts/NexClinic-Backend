package com.thphatts.clinicportal.service.ai.agent;

import com.thphatts.clinicportal.dto.response.AiAgentActionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AiAgentService - Phân tích ý định (Intent Recognition) & Điều phối thực thi Tool (Tool Execution Coordinator).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentService {

    private final AiToolRegistry toolRegistry;

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern TIMESLOT_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2}\\s*-\\s*\\d{1,2}:\\d{2})");
    private static final Pattern HOUR_PATTERN = Pattern.compile("(?:lúc|vào|ca)?\\s*(\\d{1,2})\\s*(?:h|giờ|:00)");
    private static final Pattern ID_PATTERN = Pattern.compile("(?:lịch hẹn|id|mã)\\s*#?(\\d+)", Pattern.CASE_INSENSITIVE);

    /**
     * Kiểm tra tin nhắn người dùng có chứa Ý định Hành động (Action Intent) hay không.
     */
    public boolean isActionIntent(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return false;
        String lower = userMessage.toLowerCase();

        return lower.contains("đặt lịch") || lower.contains("tạo lịch") || lower.contains("đăng ký khám") ||
               lower.contains("hủy lịch") || lower.contains("hủy hẹn") ||
               (lower.contains("tìm") && lower.contains("bác sĩ"));
    }

    /**
     * Phân tích tin nhắn, bóc tách tham số và thực thi Tool tương ứng
     */
    public AiAgentActionResult processUserAction(String userMessage) {
        String lower = userMessage.toLowerCase();

        if (lower.contains("hủy lịch") || lower.contains("hủy hẹn")) {
            Map<String, Object> params = new HashMap<>();
            Matcher idMatcher = ID_PATTERN.matcher(userMessage);
            if (idMatcher.find()) {
                params.put("appointmentId", Long.parseLong(idMatcher.group(1)));
            }
            return toolRegistry.executeTool("CANCEL_APPOINTMENT", params);

        } else if (lower.contains("đặt lịch") || lower.contains("tạo lịch") || lower.contains("đăng ký khám")) {
            Map<String, Object> params = extractAppointmentParams(userMessage);
            return toolRegistry.executeTool("CREATE_APPOINTMENT", params);

        } else if (lower.contains("tìm bác sĩ") || (lower.contains("tìm") && lower.contains("bác sĩ"))) {
            Map<String, Object> params = new HashMap<>();
            String keyword = userMessage.replaceAll("(?i).*?(tìm bác sĩ|tìm)\\s*", "").trim();
            params.put("keyword", keyword);
            return toolRegistry.executeTool("SEARCH_DOCTOR", params);
        }

        return AiAgentActionResult.failure("UNKNOWN", "Không thể xác định tác vụ từ câu lệnh.");
    }

    /**
     * Bóc tách các tham số cho tác vụ Đặt lịch hẹn từ câu thoại tự nhiên
     */
    private Map<String, Object> extractAppointmentParams(String userMessage) {
        Map<String, Object> params = new HashMap<>();

        // 1. Match Ngày (yyyy-MM-dd) hoặc từ khóa ngày mai/hôm nay
        Matcher dateMatcher = DATE_PATTERN.matcher(userMessage);
        if (dateMatcher.find()) {
            params.put("appointmentDate", dateMatcher.group(1));
        } else if (userMessage.toLowerCase().contains("ngày mai") || userMessage.toLowerCase().contains("sáng mai") || userMessage.toLowerCase().contains("chiều mai")) {
            params.put("appointmentDate", LocalDate.now().plusDays(1).toString());
        } else if (userMessage.toLowerCase().contains("hôm nay")) {
            params.put("appointmentDate", LocalDate.now().toString());
        } else {
            params.put("appointmentDate", LocalDate.now().plusDays(1).toString());
        }

        // 2. Match Ca khám/Khung giờ (HH:mm - HH:mm hoặc lúc 9 giờ, 9h, 14:00)
        Matcher timeMatcher = TIMESLOT_PATTERN.matcher(userMessage);
        if (timeMatcher.find()) {
            params.put("timeSlot", timeMatcher.group(1));
        } else {
            Matcher hourMatcher = HOUR_PATTERN.matcher(userMessage.toLowerCase());
            if (hourMatcher.find()) {
                int hour = Integer.parseInt(hourMatcher.group(1));
                if (userMessage.toLowerCase().contains("chiều") && hour < 12) {
                    hour += 12;
                }
                String startHour = String.format("%02d:00", hour);
                String endHour = String.format("%02d:00", hour + 1);
                params.put("timeSlot", startHour + " - " + endHour);
            } else {
                params.put("timeSlot", "09:00 - 10:00");
            }
        }

        // 3. Match tên Bác sĩ
        Pattern docPattern = Pattern.compile("(?:bác sĩ|bs\\.?)\\s+([a-zA-ZÀ-ỹ\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher docMatcher = docPattern.matcher(userMessage);
        if (docMatcher.find()) {
            String docName = docMatcher.group(1).trim();
            // Lọc bỏ các từ thời gian/địa điểm thừa
            docName = docName.split("(?i)(vào|lúc|ngày|ca|sáng|chiều|tại|hôm|mai)")[0].trim();
            params.put("doctorName", docName);
        }

        params.put("reason", "Đặt lịch tư vấn qua AI Agent Chatbot");
        return params;
    }
}
