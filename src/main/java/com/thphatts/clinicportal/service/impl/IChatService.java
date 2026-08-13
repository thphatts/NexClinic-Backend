package com.thphatts.clinicportal.service.impl;

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
import com.thphatts.clinicportal.repository.ChatMessageRepository;
import com.thphatts.clinicportal.repository.ChatRoomRepository;
import com.thphatts.clinicportal.repository.DoctorRepository;
import com.thphatts.clinicportal.repository.PatientRepository;
import com.thphatts.clinicportal.repository.UserRepository;
import com.thphatts.clinicportal.service.ChatService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class IChatService implements ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    private final AtomicBoolean listenerRunning = new AtomicBoolean(false);
    private final ExecutorService listenerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "pg-chat-listener");
        t.setDaemon(true);
        return t;
    });

    // ─── PostgreSQL LISTEN/NOTIFY ─────────────────────────────────────────────

    /**
     * Khởi động thread lắng nghe kênh "clinic_chat" từ PostgreSQL.
     * Khi có NOTIFY, relay tin nhắn đến STOMP broker → client WebSocket.
     */
    @PostConstruct
    public void startPgListener() {
        listenerRunning.set(true);
        listenerExecutor.submit(() -> {
            while (listenerRunning.get()) {
                try (Connection conn = dataSource.getConnection()) {
                    PGConnection pgConn = conn.unwrap(PGConnection.class);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("LISTEN clinic_chat");
                    }
                    log.info("[Chat] PostgreSQL LISTEN 'clinic_chat' đã sẵn sàng.");

                    while (listenerRunning.get()) {
                        // getNotifications() block trong tối đa 1s, tránh spin CPU
                        PGNotification[] notifications = pgConn.getNotifications(1000);
                        if (notifications != null) {
                            for (PGNotification notification : notifications) {
                                handleNotification(notification.getParameter());
                            }
                        }
                    }
                } catch (Exception e) {
                    if (listenerRunning.get()) {
                        log.warn("[Chat] PG Listener mất kết nối, thử lại sau 3s: {}", e.getMessage());
                        try { Thread.sleep(3000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                }
            }
            log.info("[Chat] PG Listener đã dừng.");
        });
    }

    @PreDestroy
    public void stopPgListener() {
        listenerRunning.set(false);
        listenerExecutor.shutdown();
    }

    /**
     * Xử lý payload JSON từ NOTIFY và đẩy đến STOMP topic của phòng tương ứng.
     */
    private void handleNotification(String payload) {
        try {
            ChatMessageResponse msg = objectMapper.readValue(payload, ChatMessageResponse.class);
            messagingTemplate.convertAndSend("/topic/chat." + msg.getRoomId(), msg);
            log.debug("[Chat] Relay NOTIFY → STOMP /topic/chat.{}", msg.getRoomId());
        } catch (Exception e) {
            log.error("[Chat] Không parse được NOTIFY payload: {}", payload, e);
        }
    }

    /**
     * Gửi NOTIFY đến PostgreSQL sau khi lưu tin nhắn.
     * Dùng pg_notify() để tránh phải dùng Statement riêng.
     */
    private void pgNotify(ChatMessageResponse msg) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String json = objectMapper.writeValueAsString(msg);
            // Escape single quotes trong JSON
            String safeJson = json.replace("'", "''");
            stmt.execute("SELECT pg_notify('clinic_chat', '" + safeJson + "')");
        } catch (Exception e) {
            log.error("[Chat] Không gửi được pg_notify: {}", e.getMessage(), e);
        }
    }

    // ─── Service Methods ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public ChatRoomResponse getOrCreateRoom(CreateChatRoomRequest request, UserPrincipal currentUser) {
        // Nếu đã có phòng giữa bác sĩ và bệnh nhân này → trả về phòng cũ
        ChatRoom room = chatRoomRepository
                .findByDoctorIdAndPatientId(request.getDoctorId(), request.getPatientId())
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.builder()
                            .doctorId(request.getDoctorId())
                            .patientId(request.getPatientId())
                            .appointmentId(request.getAppointmentId())
                            .status(ChatRoomStatus.ACTIVE)
                            .build();
                    return chatRoomRepository.save(newRoom);
                });

        return toRoomResponse(room, currentUser.getUserId());
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(SendMessageRequest request, UserPrincipal currentUser) {
        // Kiểm tra phòng chat tồn tại
        ChatRoom room = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng chat: " + request.getRoomId()));

        if (room.getStatus() == ChatRoomStatus.CLOSED) {
            throw new RuntimeException("Phòng chat đã đóng, không thể gửi tin nhắn.");
        }

        // Lấy tên người gửi
        String senderName = resolveSenderName(currentUser.getUserId());

        // Lưu tin nhắn
        ChatMessage message = ChatMessage.builder()
                .roomId(room.getId())
                .senderId(currentUser.getUserId())
                .senderName(senderName)
                .senderRole(currentUser.getRole().name())
                .content(request.getContent())
                .isRead(false)
                .build();
        ChatMessage saved = chatMessageRepository.save(message);

        // Cập nhật updatedAt của phòng chat
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        ChatMessageResponse response = toMessageResponse(saved);

        // Gửi NOTIFY đến PostgreSQL → listener sẽ relay qua STOMP
        pgNotify(response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ChatMessageResponse> getMessageHistory(Long roomId, int page, int size, UserPrincipal currentUser) {
        // Kiểm tra quyền truy cập
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng chat: " + roomId));
        validateRoomAccess(room, currentUser.getUserId());

        Page<ChatMessage> msgPage = chatMessageRepository
                .findByRoomIdOrderByCreatedAtAsc(roomId, PageRequest.of(page, size));

        List<ChatMessageResponse> items = msgPage.getContent()
                .stream().map(this::toMessageResponse).toList();

        return new PagedResponse<>(
                items,
                msgPage.getNumber() + 1,
                msgPage.getSize(),
                msgPage.getTotalElements(),
                msgPage.getTotalPages(),
                msgPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms(UserPrincipal currentUser) {
        String userId = currentUser.getUserId();
        String roleName = currentUser.getRole().name();

        List<ChatRoom> rooms;
        if ("ROLE_DOCTOR".equals(roleName)) {
            Doctor doctor = doctorRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ bác sĩ."));
            rooms = chatRoomRepository.findByDoctorIdOrderByUpdatedAtDesc(doctor.getId());
        } else if ("ROLE_PATIENT".equals(roleName)) {
            Patient patient = patientRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ bệnh nhân."));
            rooms = chatRoomRepository.findByPatientIdOrderByUpdatedAtDesc(patient.getId());
        } else {
            // Admin/Staff: trả về tất cả
            rooms = chatRoomRepository.findAll();
        }

        return rooms.stream().map(r -> toRoomResponse(r, userId)).toList();
    }

    @Override
    @Transactional
    public void markAsRead(Long roomId, UserPrincipal currentUser) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng chat: " + roomId));
        validateRoomAccess(room, currentUser.getUserId());
        chatMessageRepository.markAllAsRead(roomId, currentUser.getUserId(), LocalDateTime.now());
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private void validateRoomAccess(ChatRoom room, String userId) {
        // Tìm doctorId và patientId tương ứng với userId
        boolean isDoctor = doctorRepository.findByUserId(userId)
                .map(d -> d.getId().equals(room.getDoctorId())).orElse(false);
        boolean isPatient = patientRepository.findByUserId(userId)
                .map(p -> p.getId().equals(room.getPatientId())).orElse(false);
        boolean isAdmin = userRepository.findById(userId)
                .map(u -> u.getRole() != null &&
                        (u.getRole().name().equals("ROLE_ADMIN") || u.getRole().name().equals("ROLE_STAFF")))
                .orElse(false);

        if (!isDoctor && !isPatient && !isAdmin) {
            throw new RuntimeException("Bạn không có quyền truy cập phòng chat này.");
        }
    }

    private String resolveSenderName(String userId) {
        return userRepository.findById(userId)
                .map(u -> u.getName() != null && !u.getName().isBlank() ? u.getName() : u.getUsername())
                .orElse("Người dùng");
    }

    private ChatMessageResponse toMessageResponse(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .roomId(msg.getRoomId())
                .senderId(msg.getSenderId())
                .senderName(msg.getSenderName())
                .senderRole(msg.getSenderRole())
                .content(msg.getContent())
                .isRead(msg.isRead())
                .readAt(msg.getReadAt())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room, String currentUserId) {
        // Lấy tên bác sĩ
        String doctorName = doctorRepository.findById(room.getDoctorId())
                .map(Doctor::getFullName).orElse("Bác sĩ");

        // Lấy tên bệnh nhân
        String patientName = patientRepository.findById(room.getPatientId())
                .map(Patient::getFullName).orElse("Bệnh nhân");

        // Tin nhắn cuối cùng
        String lastMessage = chatMessageRepository.findLastMessageByRoomId(room.getId())
                .map(ChatMessage::getContent).orElse(null);

        LocalDateTime lastMessageAt = chatMessageRepository.findLastMessageByRoomId(room.getId())
                .map(ChatMessage::getCreatedAt).orElse(null);

        // Số tin nhắn chưa đọc
        long unread = chatMessageRepository.countByRoomIdAndIsReadFalseAndSenderIdNot(room.getId(), currentUserId);

        return ChatRoomResponse.builder()
                .id(room.getId())
                .appointmentId(room.getAppointmentId())
                .doctorId(room.getDoctorId())
                .doctorName(doctorName)
                .patientId(room.getPatientId())
                .patientName(patientName)
                .status(room.getStatus().name())
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unread)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
