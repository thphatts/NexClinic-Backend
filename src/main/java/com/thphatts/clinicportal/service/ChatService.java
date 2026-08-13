package com.thphatts.clinicportal.service;

import com.thphatts.clinicportal.config.security.UserPrincipal;
import com.thphatts.clinicportal.dto.request.CreateChatRoomRequest;
import com.thphatts.clinicportal.dto.request.SendMessageRequest;
import com.thphatts.clinicportal.dto.response.ChatMessageResponse;
import com.thphatts.clinicportal.dto.response.ChatRoomResponse;
import com.thphatts.clinicportal.dto.response.PagedResponse;

import java.util.List;

public interface ChatService {

    /**
     * Lấy hoặc tạo mới phòng chat giữa bác sĩ và bệnh nhân.
     * Nếu đã tồn tại phòng giữa 2 người, trả về phòng đó.
     */
    ChatRoomResponse getOrCreateRoom(CreateChatRoomRequest request, UserPrincipal currentUser);

    /**
     * Lưu tin nhắn vào DB và broadcast qua STOMP + PostgreSQL NOTIFY.
     */
    ChatMessageResponse sendMessage(SendMessageRequest request, UserPrincipal currentUser);

    /**
     * Lấy lịch sử tin nhắn theo phòng, phân trang.
     */
    PagedResponse<ChatMessageResponse> getMessageHistory(Long roomId, int page, int size, UserPrincipal currentUser);

    /**
     * Lấy tất cả phòng chat mà user hiện tại tham gia.
     */
    List<ChatRoomResponse> getMyRooms(UserPrincipal currentUser);

    /**
     * Đánh dấu tất cả tin nhắn trong phòng là đã đọc.
     */
    void markAsRead(Long roomId, UserPrincipal currentUser);
}
