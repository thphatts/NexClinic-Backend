package com.thphatts.clinicportal.controller;

import com.thphatts.clinicportal.common.ApiResponse;
import com.thphatts.clinicportal.common.BaseController;
import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.dto.request.CreateChatRoomRequest;
import com.thphatts.clinicportal.dto.request.SendMessageRequest;
import com.thphatts.clinicportal.dto.response.ChatMessageResponse;
import com.thphatts.clinicportal.dto.response.ChatRoomResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;
import com.thphatts.clinicportal.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController extends BaseController {

    private final ChatService chatService;

    // ── WebSocket STOMP endpoint ──────────────────────────────────────────────

    /**
     * Client gửi đến: /app/chat.send
     * Server broadcast đến: /topic/chat.{roomId}  (qua PG NOTIFY → listener)
     */
    @MessageMapping("/chat.send")
    public void sendMessageViaSocket(@Payload SendMessageRequest request, Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            chatService.sendMessage(request, userPrincipal);
        }
    }

    // ── REST endpoints ────────────────────────────────────────────────────────

    /**
     * Lấy danh sách phòng chat của user hiện tại.
     */
    @GetMapping("/rooms")
    public ApiResponse<List<ChatRoomResponse>> getMyRooms(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(chatService.getMyRooms(currentUser));
    }

    /**
     * Tạo hoặc lấy phòng chat (idempotent).
     */
    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatRoomResponse> getOrCreateRoom(
            @Valid @RequestBody CreateChatRoomRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return createdSuccessResponse(chatService.getOrCreateRoom(request, currentUser));
    }

    /**
     * Lấy lịch sử tin nhắn trong phòng (phân trang, mặc định 50/trang).
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<PagedResponse<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        return ApiResponse.success(chatService.getMessageHistory(roomId, page, size, currentUser));
    }

    /**
     * Gửi tin nhắn qua REST (fallback khi WebSocket không khả dụng).
     */
    @PostMapping("/rooms/{roomId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChatMessageResponse> sendMessageViaRest(
            @PathVariable Long roomId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        request.setRoomId(roomId);
        return createdSuccessResponse(chatService.sendMessage(request, currentUser));
    }

    /**
     * Đánh dấu tất cả tin nhắn trong phòng là đã đọc.
     */
    @PostMapping("/rooms/{roomId}/read")
    public ApiResponse<String> markAsRead(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        chatService.markAsRead(roomId, currentUser);
        return ApiResponse.success("Đã đánh dấu đã đọc cho phòng chat " + roomId);
    }
}
