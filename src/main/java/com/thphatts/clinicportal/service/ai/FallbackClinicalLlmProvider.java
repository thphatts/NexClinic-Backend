package com.thphatts.clinicportal.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FallbackClinicalLlmProvider implements LlmProvider {

    @Override
    public String generateText(String prompt) {
        String lowerPrompt = prompt.toLowerCase();

        if (lowerPrompt.contains("sốt") || lowerPrompt.contains("ho") || lowerPrompt.contains("đau họng")) {
            return "Dựa trên mô tả triệu chứng hô hấp/sốt của bạn: Đây có thể là dấu hiệu của Cảm cúm hoặc Viêm đường hô hấp trên. " +
                   "Bạn nên giữ ấm cơ thể, uống đủ 2 lít nước ấm mỗi ngày, theo dõi sát thân nhiệt. " +
                   "Nếu sốt kéo dài trên 2 ngày hoặc sốt > 38.5 độ C không hạ, bạn nên đặt lịch hẹn thăm khám với Bác sĩ Chuyên khoa Hô Hấp.";
        } else if (lowerPrompt.contains("đau ngực") || lowerPrompt.contains("khó thở") || lowerPrompt.contains("co giật")) {
            return "🚨 CẢNH BÁO NGUY HIỂM CẤP TÍNH: Bạn đang có dấu hiệu liên quan đến cấp cứu tim mạch/hô hấp. " +
                   "Hãy đến ngay phòng cấp cứu bệnh viện gần nhất hoặc gọi 115 để được hỗ trợ khẩn cấp!";
        } else if (lowerPrompt.contains("đặt lịch") || lowerPrompt.contains("bác sĩ") || lowerPrompt.contains("giờ")) {
            return "Phòng khám làm việc từ 07:30 - 20:00 hàng ngày. Bạn có thể sử dụng ứng dụng để tìm kiếm Bác sĩ theo Chuyên khoa và đăng ký chọn khung giờ khám còn trống.";
        }

        return "Cảm ơn bạn đã liên hệ Trợ lý AI Y tế của Phòng khám Clinic Portal. " +
               "Để nhận tư vấn chi tiết hơn, bạn hãy mô tả rõ các triệu chứng bất thường, số ngày mắc bệnh hoặc xem danh sách các Bác sĩ chuyên khoa của chúng tôi.";
    }

    @Override
    public boolean isAvailable() {
        return true; // Luôn luôn sẵn sàng chạy 100% Offline
    }

    @Override
    public String getProviderName() {
        return "fallback";
    }
}
