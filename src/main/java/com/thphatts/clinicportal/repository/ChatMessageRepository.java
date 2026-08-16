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
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findFirstByRoomIdOrderByCreatedAtDesc(Long roomId);

    default Optional<ChatMessage> findLastMessageByRoomId(Long roomId) {
        return findFirstByRoomIdOrderByCreatedAtDesc(roomId);
    }

    Page<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId, Pageable pageable);

    long countByRoomIdAndIsReadFalseAndSenderIdNot(Long roomId, String senderId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true, m.readAt = :readAt " +
           "WHERE m.roomId = :roomId AND m.isRead = false AND m.senderId <> :senderId")
    int markAllAsRead(@Param("roomId") Long roomId,
                      @Param("senderId") String senderId,
                      @Param("readAt") LocalDateTime readAt);

    @Query(value = "SELECT DISTINCT ON (room_id) * FROM clinic_chat_messages " + "WHERE room_id IN (:roomIds) ORDER BY room_id, created_at DESC", nativeQuery = true)
    List<ChatMessage> findLastMessagesByRoomIds(@Param("roomIds") List<Long> roomIds);


    @Query("SELECT m.roomId as roomId, COUNT(m) as unreadCount FROM ChatMessage m "
            + "WHERE m.roomId IN :roomIds AND m.isRead = false AND m.senderId <> :senderId "
            + "GROUP BY m.roomId")
    List<UnreadCountProjection> countUnreadByRoomIds(
            @Param("roomIds") List<Long> roomIds, @Param("senderId") String senderId);

    interface UnreadCountProjection {
        Long getRoomId();
        Long getUnreadCount();
    }
}
