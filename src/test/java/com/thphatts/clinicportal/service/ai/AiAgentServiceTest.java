package com.thphatts.clinicportal.service.ai;

import com.thphatts.clinicportal.dto.response.AiAgentActionResult;
import com.thphatts.clinicportal.service.ai.agent.AiAgentService;
import com.thphatts.clinicportal.service.ai.agent.AiToolRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiAgentService Unit Tests")
class AiAgentServiceTest {

    @Mock
    private AiToolRegistry toolRegistry;

    @InjectMocks
    private AiAgentService aiAgentService;

    @Nested
    @DisplayName("Kiểm tra Ý định Hành động (isActionIntent)")
    class ActionIntentDetectionTests {

        @Test
        @DisplayName("Nhận diện đúng câu lệnh đặt lịch hẹn")
        void isActionIntent_CreateAppointment() {
            assertTrue(aiAgentService.isActionIntent("Tôi muốn đặt lịch khám vào ngày mai"));
            assertTrue(aiAgentService.isActionIntent("Đăng ký khám bác sĩ An"));
            assertTrue(aiAgentService.isActionIntent("Tạo lịch tư vấn chiều nay"));
        }

        @Test
        @DisplayName("Nhận diện đúng câu lệnh hủy lịch hẹn")
        void isActionIntent_CancelAppointment() {
            assertTrue(aiAgentService.isActionIntent("Hủy lịch hẹn #123 giúp tôi"));
            assertTrue(aiAgentService.isActionIntent("Tôi muốn hủy hẹn khám"));
        }

        @Test
        @DisplayName("Nhận diện đúng câu lệnh tìm bác sĩ")
        void isActionIntent_SearchDoctor() {
            assertTrue(aiAgentService.isActionIntent("Tìm bác sĩ chuyên khoa hô hấp"));
            assertTrue(aiAgentService.isActionIntent("Tìm bác sĩ Nguyễn Văn A"));
        }

        @Test
        @DisplayName("Trả về false cho câu hỏi tư vấn sức khỏe thông thường")
        void isActionIntent_GeneralQuery() {
            assertFalse(aiAgentService.isActionIntent("Tôi bị ho và đau họng thì nên làm gì?"));
            assertFalse(aiAgentService.isActionIntent("Cho tôi hỏi giờ làm việc của phòng khám"));
            assertFalse(aiAgentService.isActionIntent(null));
            assertFalse(aiAgentService.isActionIntent("   "));
        }
    }

    @Nested
    @DisplayName("Xử lý Hành động Người dùng (processUserAction)")
    class ProcessUserActionTests {

        @Test
        @DisplayName("Xử lý lệnh hủy lịch hẹn và bóc tách ID thành công")
        void processUserAction_CancelAppointment() {
            AiAgentActionResult expectedResult = AiAgentActionResult.success(
                    "CANCEL_APPOINTMENT",
                    "Đã hủy thành công lịch hẹn #100",
                    Map.of("appointmentId", 100L)
            );

            when(toolRegistry.executeTool(eq("CANCEL_APPOINTMENT"), anyMap()))
                    .thenReturn(expectedResult);

            AiAgentActionResult result = aiAgentService.processUserAction("Hủy lịch hẹn #100");

            assertNotNull(result);
            assertTrue(result.success());
            assertEquals("CANCEL_APPOINTMENT", result.actionType());
            verify(toolRegistry).executeTool(eq("CANCEL_APPOINTMENT"), argThat(map ->
                    Long.valueOf(100L).equals(map.get("appointmentId"))
            ));
        }

        @Test
        @DisplayName("Xử lý lệnh đặt lịch hẹn và trích xuất tham số thành công")
        void processUserAction_CreateAppointment() {
            AiAgentActionResult expectedResult = AiAgentActionResult.success(
                    "CREATE_APPOINTMENT",
                    "Đã tạo thành công lịch hẹn",
                    Map.of()
            );

            when(toolRegistry.executeTool(eq("CREATE_APPOINTMENT"), anyMap()))
                    .thenReturn(expectedResult);

            AiAgentActionResult result = aiAgentService.processUserAction("Đặt lịch khám bác sĩ An lúc 9 giờ sáng mai");

            assertNotNull(result);
            assertTrue(result.success());
            verify(toolRegistry).executeTool(eq("CREATE_APPOINTMENT"), argThat(map ->
                    map.containsKey("appointmentDate") && map.containsKey("timeSlot")
            ));
        }

        @Test
        @DisplayName("Xử lý lệnh tìm kiếm bác sĩ thành công")
        void processUserAction_SearchDoctor() {
            AiAgentActionResult expectedResult = AiAgentActionResult.success(
                    "SEARCH_DOCTOR",
                    "Tìm thấy 1 Bác sĩ",
                    Map.of()
            );

            when(toolRegistry.executeTool(eq("SEARCH_DOCTOR"), anyMap()))
                    .thenReturn(expectedResult);

            AiAgentActionResult result = aiAgentService.processUserAction("Tìm bác sĩ Nội khoa");

            assertNotNull(result);
            assertTrue(result.success());
            verify(toolRegistry).executeTool(eq("SEARCH_DOCTOR"), argThat(map ->
                    map.containsKey("keyword")
            ));
        }

        @Test
        @DisplayName("Trả về thất bại khi không nhận diện được lệnh")
        void processUserAction_UnknownCommand() {
            AiAgentActionResult result = aiAgentService.processUserAction("Thời tiết hôm nay thế nào");

            assertNotNull(result);
            assertFalse(result.success());
            assertEquals("UNKNOWN", result.actionType());
            verify(toolRegistry, never()).executeTool(anyString(), anyMap());
        }
    }
}
