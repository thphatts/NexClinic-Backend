package com.thphatts.clinicportal.repository;

import com.thphatts.clinicportal.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId, Pageable pageable);

    long countByRoomIdAndIsReadFalseAndSenderIdNot(Long roomId, String senderId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true, m.readAt = :readAt " +
           "WHERE m.roomId = :roomId AND m.isRead = false AND m.senderId <> :senderId")
    int markAllAsRead(@Param("roomId") Long roomId,
                      @Param("senderId") String senderId,
                      @Param("readAt") LocalDateTime readAt);

    @Query("SELECT m FROM ChatMessage m WHERE m.roomId = :roomId ORDER BY m.createdAt DESC LIMIT 1")
    java.util.Optional<ChatMessage> findLastMessageByRoomId(@Param("roomId") Long roomId);
}
