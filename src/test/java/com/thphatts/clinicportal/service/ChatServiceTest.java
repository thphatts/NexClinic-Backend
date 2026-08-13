package com.thphatts.clinicportal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.dto.request.CreateChatRoomRequest;
import com.thphatts.clinicportal.dto.request.SendMessageRequest;
import com.thphatts.clinicportal.dto.response.ChatMessageResponse;
import com.thphatts.clinicportal.dto.response.ChatRoomResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.entity.ChatMessage;
import com.thphatts.clinicportal.entity.ChatRoom;
import com.thphatts.clinicportal.entity.Doctor;
import com.thphatts.clinicportal.entity.Patient;
import com.thphatts.clinicportal.entity.User;
import com.thphatts.clinicportal.entity.enums.ChatRoomStatus;
import com.thphatts.clinicportal.entity.enums.Role;
import com.thphatts.clinicportal.repository.ChatMessageRepository;
import com.thphatts.clinicportal.repository.ChatRoomRepository;
import com.thphatts.clinicportal.repository.DoctorRepository;
import com.thphatts.clinicportal.repository.PatientRepository;
import com.thphatts.clinicportal.repository.UserRepository;
import com.thphatts.clinicportal.service.impl.IChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatService Unit Tests")
class ChatServiceTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private DataSource dataSource;
    @Mock private ObjectMapper objectMapper;

    // Mocks cho JDBC LISTEN/NOTIFY — cần để pgNotify() không throw NullPointer
    @Mock private Connection mockConnection;
    @Mock private Statement mockStatement;

    @InjectMocks
    private IChatService chatService;

    // ── Test fixtures ─────────────────────────────────────────────────────────
    private UserPrincipal doctorPrincipal;
    private UserPrincipal patientPrincipal;
    private UserPrincipal adminPrincipal;

    private Doctor mockDoctor;
    private Patient mockPatient;
    private ChatRoom mockRoom;
    private ChatMessage mockMessage;

    @BeforeEach
    void setUp() throws Exception {
        // Users
        User doctorUser = User.builder()
                .id("doctor-uuid")
                .username("bs.an")
                .name("BS. Nguyễn Văn An")
                .role(Role.ROLE_DOCTOR)
                .build();
        User patientUser = User.builder()
                .id("patient-uuid")
                .username("patient01")
                .name("Trần Thị Bình")
                .role(Role.ROLE_PATIENT)
                .build();
        User adminUser = User.builder()
                .id("admin-uuid")
                .username("admin")
                .name("Admin")
                .role(Role.ROLE_ADMIN)
                .build();

        doctorPrincipal  = new UserPrincipal(doctorUser);
        patientPrincipal = new UserPrincipal(patientUser);
        adminPrincipal   = new UserPrincipal(adminUser);

        // Doctor & Patient entities
        mockDoctor = Doctor.builder()
                .id(1L)
                .fullName("BS. Nguyễn Văn An")
                .specialization("Nội Khoa")
                .user(doctorUser)
                .build();

        mockPatient = Patient.builder()
                .id(2L)
                .fullName("Trần Thị Bình")
                .phone("0900000001")
                .userId("patient-uuid")
                .build();

        mockRoom = ChatRoom.builder()
                .id(10L)
                .doctorId(1L)
                .patientId(2L)
                .status(ChatRoomStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mockMessage = ChatMessage.builder()
                .id(100L)
                .roomId(10L)
                .senderId("doctor-uuid")
                .senderName("BS. Nguyễn Văn An")
                .senderRole("ROLE_DOCTOR")
                .content("Bạn cảm thấy thế nào hôm nay?")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // Stub JDBC để pgNotify() không throw khi test sendMessage
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"id\":100}");
    }

    // =========================================================
    //  1. getOrCreateRoom
    // =========================================================
    @Nested
    @DisplayName("Tạo/Lấy Phòng Chat (getOrCreateRoom)")
    class GetOrCreateRoomTests {

        @Test
        @DisplayName("Trả về phòng chat đã tồn tại nếu bác sĩ và bệnh nhân đã có phòng")
        void getOrCreateRoom_ExistingRoom_ReturnsExistingRoom() {
            CreateChatRoomRequest request = new CreateChatRoomRequest();
            request.setDoctorId(1L);
            request.setPatientId(2L);

            when(chatRoomRepository.findByDoctorIdAndPatientId(1L, 2L))
                    .thenReturn(Optional.of(mockRoom));
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));
            when(chatMessageRepository.findLastMessageByRoomId(10L)).thenReturn(Optional.empty());
            when(chatMessageRepository.countByRoomIdAndIsReadFalseAndSenderIdNot(10L, "doctor-uuid"))
                    .thenReturn(0L);

            ChatRoomResponse result = chatService.getOrCreateRoom(request, doctorPrincipal);

            assertNotNull(result);
            assertEquals(10L, result.getId());
            assertEquals(1L, result.getDoctorId());
            assertEquals(2L, result.getPatientId());
            assertEquals("ACTIVE", result.getStatus());

            // Không tạo phòng mới
            verify(chatRoomRepository, never()).save(any());
        }

        @Test
        @DisplayName("Tạo phòng chat mới khi chưa có phòng giữa 2 người")
        void getOrCreateRoom_NoExistingRoom_CreatesNewRoom() {
            CreateChatRoomRequest request = new CreateChatRoomRequest();
            request.setDoctorId(1L);
            request.setPatientId(2L);

            ChatRoom savedRoom = ChatRoom.builder()
                    .id(99L)
                    .doctorId(1L)
                    .patientId(2L)
                    .status(ChatRoomStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(chatRoomRepository.findByDoctorIdAndPatientId(1L, 2L))
                    .thenReturn(Optional.empty());
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(savedRoom);
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));
            when(chatMessageRepository.findLastMessageByRoomId(99L)).thenReturn(Optional.empty());
            when(chatMessageRepository.countByRoomIdAndIsReadFalseAndSenderIdNot(99L, "doctor-uuid"))
                    .thenReturn(0L);

            ChatRoomResponse result = chatService.getOrCreateRoom(request, doctorPrincipal);

            assertNotNull(result);
            assertEquals(99L, result.getId());
            // Đảm bảo phòng mới được lưu vào DB
            verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("Phòng chat liên kết với appointment được tạo đúng")
        void getOrCreateRoom_WithAppointmentId_SetsAppointmentId() {
            CreateChatRoomRequest request = new CreateChatRoomRequest();
            request.setDoctorId(1L);
            request.setPatientId(2L);
            request.setAppointmentId(55L);

            ChatRoom savedRoom = ChatRoom.builder()
                    .id(11L)
                    .doctorId(1L)
                    .patientId(2L)
                    .appointmentId(55L)
                    .status(ChatRoomStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(chatRoomRepository.findByDoctorIdAndPatientId(1L, 2L))
                    .thenReturn(Optional.empty());
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(savedRoom);
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));
            when(chatMessageRepository.findLastMessageByRoomId(11L)).thenReturn(Optional.empty());
            when(chatMessageRepository.countByRoomIdAndIsReadFalseAndSenderIdNot(11L, "doctor-uuid"))
                    .thenReturn(0L);

            ChatRoomResponse result = chatService.getOrCreateRoom(request, doctorPrincipal);

            assertEquals(55L, result.getAppointmentId());
        }
    }

    // =========================================================
    //  2. sendMessage
    // =========================================================
    @Nested
    @DisplayName("Gửi Tin Nhắn (sendMessage)")
    class SendMessageTests {

        @Test
        @DisplayName("Gửi tin nhắn thành công — bác sĩ gửi vào phòng ACTIVE")
        void sendMessage_Success_DoctorSends() throws Exception {
            SendMessageRequest request = new SendMessageRequest();
            request.setRoomId(10L);
            request.setContent("Bạn cảm thấy thế nào hôm nay?");

            when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(mockRoom));
            when(userRepository.findById("doctor-uuid"))
                    .thenReturn(Optional.of(mockDoctor.getUser()));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(mockRoom);

            ChatMessageResponse result = chatService.sendMessage(request, doctorPrincipal);

            assertNotNull(result);
            assertEquals(100L, result.getId());
            assertEquals(10L, result.getRoomId());
            assertEquals("Bạn cảm thấy thế nào hôm nay?", result.getContent());
            assertEquals("ROLE_DOCTOR", result.getSenderRole());

            // Phải lưu message vào DB
            verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
            // Phải update updatedAt của phòng
            verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("Gửi tin nhắn thành công — bệnh nhân gửi vào phòng ACTIVE")
        void sendMessage_Success_PatientSends() throws Exception {
            SendMessageRequest request = new SendMessageRequest();
            request.setRoomId(10L);
            request.setContent("Dạ con vẫn bị ho ạ");

            ChatMessage patientMessage = ChatMessage.builder()
                    .id(101L)
                    .roomId(10L)
                    .senderId("patient-uuid")
                    .senderName("Trần Thị Bình")
                    .senderRole("ROLE_PATIENT")
                    .content("Dạ con vẫn bị ho ạ")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(mockRoom));
            when(userRepository.findById("patient-uuid"))
                    .thenReturn(Optional.of(User.builder()
                            .id("patient-uuid")
                            .name("Trần Thị Bình")
                            .username("patient01")
                            .role(Role.ROLE_PATIENT)
                            .build()));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(patientMessage);
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(mockRoom);

            ChatMessageResponse result = chatService.sendMessage(request, patientPrincipal);

            assertNotNull(result);
            assertEquals("ROLE_PATIENT", result.getSenderRole());
            assertEquals("Trần Thị Bình", result.getSenderName());
        }

        @Test
        @DisplayName("Gửi tin nhắn thất bại — phòng chat không tồn tại")
        void sendMessage_Fail_RoomNotFound() {
            SendMessageRequest request = new SendMessageRequest();
            request.setRoomId(999L);
            request.setContent("Xin chào");

            when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> chatService.sendMessage(request, doctorPrincipal));

            assertTrue(ex.getMessage().contains("999"));
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Gửi tin nhắn thất bại — phòng chat đã đóng (CLOSED)")
        void sendMessage_Fail_RoomClosed() {
            ChatRoom closedRoom = ChatRoom.builder()
                    .id(10L)
                    .doctorId(1L)
                    .patientId(2L)
                    .status(ChatRoomStatus.CLOSED)
                    .build();

            SendMessageRequest request = new SendMessageRequest();
            request.setRoomId(10L);
            request.setContent("Xin chào");

            when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(closedRoom));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> chatService.sendMessage(request, doctorPrincipal));

            assertTrue(ex.getMessage().contains("đã đóng"));
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("pgNotify được gọi sau khi lưu tin nhắn thành công")
        void sendMessage_CallsPgNotify_AfterSave() throws Exception {
            SendMessageRequest request = new SendMessageRequest();
            request.setRoomId(10L);
            request.setContent("Test NOTIFY");

            when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(mockRoom));
            when(userRepository.findById("doctor-uuid"))
                    .thenReturn(Optional.of(mockDoctor.getUser()));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(mockRoom);

            chatService.sendMessage(request, doctorPrincipal);

            // Xác nhận đã gọi pg_notify qua JDBC connection
            verify(dataSource, atLeastOnce()).getConnection();
            verify(mockStatement, atLeastOnce()).execute(contains("pg_notify"));
        }
    }

    // =========================================================
    //  3. getMessageHistory
    // =========================================================
    @Nested
    @DisplayName("Lịch Sử Tin Nhắn (getMessageHistory)")
    class GetMessageHistoryTests {

        @Test
        @DisplayName("Admin lấy lịch sử tin nhắn bất kỳ phòng nào")
        void getMessageHistory_AdminCanAccessAnyRoom() {
            Page<ChatMessage> page = new PageImpl<>(
                    List.of(mockMessage),
                    PageRequest.of(0, 50),
                    1
            );

            when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(mockRoom));
            when(userRepository.findById("admin-uuid"))
                    .thenReturn(Optional.of(User.builder()
                            .id("admin-uuid")
                            .role(Role.ROLE_ADMIN)
                            .build()));
            when(doctorRepository.findByUserId("admin-uuid")).thenReturn(Optional.empty());
            when(patientRepository.findByUserId("admin-uuid")).thenReturn(Optional.empty());
            when(chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(eq(10L), any()))
                    .thenReturn(page);

            PagedResponse<ChatMessageResponse> result =
                    chatService.getMessageHistory(10L, 0, 50, adminPrincipal);

            assertNotNull(result);
            assertEquals(1, result.totalElements());
            assertEquals(1, result.items().size());
            assertEquals("Bạn cảm thấy thế nào hôm nay?", result.items().get(0).getContent());
        }

        @Test
        @DisplayName("Bác sĩ lấy lịch sử phòng của mình — thành công")
        void getMessageHistory_DoctorAccessOwnRoom_Success() {
            Page<ChatMessage> page = new PageImpl<>(List.of(mockMessage),
                    PageRequest.of(0, 50), 1);

            when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(mockRoom));
            when(doctorRepository.findByUserId("doctor-uuid"))
                    .thenReturn(Optional.of(mockDoctor));
            when(chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(eq(10L), any()))
                    .thenReturn(page);

            PagedResponse<ChatMessageResponse> result =
                    chatService.getMessageHistory(10L, 0, 50, doctorPrincipal);

            assertNotNull(result);
            assertEquals(1, result.items().size());
        }

        @Test
        @DisplayName("Bác sĩ không thuộc phòng này bị từ chối truy cập")
        void getMessageHistory_DoctorAccessOtherRoom_Forbidden() {
            Doctor anotherDoctor = Doctor.builder()
                    .id(99L) // khác với mockRoom.doctorId = 1L
                    .fullName("BS. Khác")
                    .build();

            when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(mockRoom));
            when(doctorRepository.findByUserId("doctor-uuid"))
                    .thenReturn(Optional.of(anotherDoctor));
            when(patientRepository.findByUserId("doctor-uuid")).thenReturn(Optional.empty());
            when(userRepository.findById("doctor-uuid")).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> chatService.getMessageHistory(10L, 0, 50, doctorPrincipal));

            assertTrue(ex.getMessage().contains("không có quyền"));
            verify(chatMessageRepository, never()).findByRoomIdOrderByCreatedAtAsc(any(), any());
        }

        @Test
        @DisplayName("Trả về danh sách rỗng khi phòng chưa có tin nhắn nào")
        void getMessageHistory_EmptyRoom_ReturnsEmptyList() {
            Page<ChatMessage> emptyPage = new PageImpl<>(
                    List.of(), PageRequest.of(0, 50), 0);

            when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(mockRoom));
            when(doctorRepository.findByUserId("doctor-uuid"))
                    .thenReturn(Optional.of(mockDoctor));
            when(chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(eq(10L), any()))
                    .thenReturn(emptyPage);

            PagedResponse<ChatMessageResponse> result =
                    chatService.getMessageHistory(10L, 0, 50, doctorPrincipal);

            assertNotNull(result);
            assertTrue(result.items().isEmpty());
            assertEquals(0, result.totalElements());
        }

        @Test
        @DisplayName("Phòng chat không tồn tại — ném RuntimeException")
        void getMessageHistory_RoomNotFound_Throws() {
            when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> chatService.getMessageHistory(999L, 0, 50, doctorPrincipal));

            assertTrue(ex.getMessage().contains("999"));
        }
    }

    // =========================================================
    //  4. getMyRooms
    // =========================================================
    @Nested
    @DisplayName("Lấy Danh Sách Phòng Chat (getMyRooms)")
    class GetMyRoomsTests {

        @Test
        @DisplayName("Bác sĩ chỉ thấy các phòng của mình")
        void getMyRooms_Doctor_ReturnsOnlyDoctorRooms() {
            when(doctorRepository.findByUserId("doctor-uuid"))
                    .thenReturn(Optional.of(mockDoctor));
            when(chatRoomRepository.findByDoctorIdOrderByUpdatedAtDesc(1L))
                    .thenReturn(List.of(mockRoom));
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));
            when(chatMessageRepository.findLastMessageByRoomId(10L)).thenReturn(Optional.empty());
            when(chatMessageRepository.countByRoomIdAndIsReadFalseAndSenderIdNot(10L, "doctor-uuid"))
                    .thenReturn(3L);

            List<ChatRoomResponse> result = chatService.getMyRooms(doctorPrincipal);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(3L, result.get(0).getUnreadCount());
            // Dùng đúng query theo doctorId
            verify(chatRoomRepository).findByDoctorIdOrderByUpdatedAtDesc(1L);
            verify(chatRoomRepository, never()).findByPatientIdOrderByUpdatedAtDesc(anyLong());
        }

        @Test
        @DisplayName("Bệnh nhân chỉ thấy các phòng của mình")
        void getMyRooms_Patient_ReturnsOnlyPatientRooms() {
            when(patientRepository.findByUserId("patient-uuid"))
                    .thenReturn(Optional.of(mockPatient));
            when(chatRoomRepository.findByPatientIdOrderByUpdatedAtDesc(2L))
                    .thenReturn(List.of(mockRoom));
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));
            when(chatMessageRepository.findLastMessageByRoomId(10L)).thenReturn(Optional.empty());
            when(chatMessageRepository.countByRoomIdAndIsReadFalseAndSenderIdNot(10L, "patient-uuid"))
                    .thenReturn(0L);

            List<ChatRoomResponse> result = chatService.getMyRooms(patientPrincipal);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(chatRoomRepository).findByPatientIdOrderByUpdatedAtDesc(2L);
            verify(chatRoomRepository, never()).findByDoctorIdOrderByUpdatedAtDesc(anyLong());
        }

        @Test
        @DisplayName("Admin thấy tất cả phòng chat")
        void getMyRooms_Admin_ReturnsAllRooms() {
            when(chatRoomRepository.findAll()).thenReturn(List.of(mockRoom));
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(mockDoctor));
            when(patientRepository.findById(2L)).thenReturn(Optional.of(mockPatient));
            when(chatMessageRepository.findLastMessageByRoomId(10L)).thenReturn(Optional.empty());
            when(chatMessageRepository.countByRoomIdAndIsReadFalseAndSenderIdNot(10L, "admin-uuid"))
                    .thenReturn(0L);

            List<ChatRoomResponse> result = chatService.getMyRooms(adminPrincipal);

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(chatRoomRepository).findAll();
        }

        @Test
        @DisplayName("Bác sĩ chưa có hồ sơ bác sĩ — ném RuntimeException")
        void getMyRooms_DoctorProfileNotFound_Throws() {
            when(doctorRepository.findByUserId("doctor-uuid")).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> chatService.getMyRooms(doctorPrincipal));

            assertTrue(ex.getMessage().contains("Không tìm thấy hồ sơ bác sĩ"));
        }
    }

    // =========================================================
    //  5. markAsRead
    // =========================================================
    @Nested
    @DisplayName("Đánh Dấu Đã Đọc (markAsRead)")
    class MarkAsReadTests {

        @Test
        @DisplayName("Bác sĩ đánh dấu đã đọc — gọi markAllAsRead đúng roomId và senderId")
        void markAsRead_Doctor_CallsMarkAllAsRead() {
            when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(mockRoom));
            when(doctorRepository.findByUserId("doctor-uuid"))
                    .thenReturn(Optional.of(mockDoctor));
            when(chatMessageRepository.markAllAsRead(eq(10L), eq("doctor-uuid"), any()))
                    .thenReturn(3);

            assertDoesNotThrow(() -> chatService.markAsRead(10L, doctorPrincipal));

            verify(chatMessageRepository, times(1))
                    .markAllAsRead(eq(10L), eq("doctor-uuid"), any());
        }

        @Test
        @DisplayName("Phòng không tồn tại — ném RuntimeException")
        void markAsRead_RoomNotFound_Throws() {
            when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> chatService.markAsRead(999L, doctorPrincipal));

            assertTrue(ex.getMessage().contains("999"));
            verify(chatMessageRepository, never()).markAllAsRead(any(), any(), any());
        }

        @Test
        @DisplayName("Người dùng không thuộc phòng chat — bị từ chối")
        void markAsRead_UserNotInRoom_Throws() {
            Doctor anotherDoctor = Doctor.builder().id(99L).fullName("BS. Khác").build();

            when(chatRoomRepository.findById(10L)).thenReturn(Optional.of(mockRoom));
            when(doctorRepository.findByUserId("doctor-uuid"))
                    .thenReturn(Optional.of(anotherDoctor)); // không phải mockRoom.doctorId=1L
            when(patientRepository.findByUserId("doctor-uuid")).thenReturn(Optional.empty());
            when(userRepository.findById("doctor-uuid")).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> chatService.markAsRead(10L, doctorPrincipal));

            assertTrue(ex.getMessage().contains("không có quyền"));
            verify(chatMessageRepository, never()).markAllAsRead(any(), any(), any());
        }
    }
}
