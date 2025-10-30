package com.opennova.repository;

import com.opennova.model.ChatMessage;
import com.opennova.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(String chatRoomId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoomId = :chatRoomId ORDER BY cm.createdAt ASC")
    List<ChatMessage> findChatHistory(@Param("chatRoomId") String chatRoomId);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE (cm.sender = :user OR cm.recipient = :user) ORDER BY cm.createdAt DESC")
    List<ChatMessage> findUserChats(@Param("user") User user);
    
    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.recipient = :user AND cm.isRead = false")
    Long countUnreadMessages(@Param("user") User user);
    
    @Query("SELECT DISTINCT cm.chatRoomId FROM ChatMessage cm WHERE cm.sender = :user OR cm.recipient = :user")
    List<String> findUserChatRooms(@Param("user") User user);
    
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.recipient = :recipient AND cm.isRead = false")
    List<ChatMessage> findUnreadMessages(@Param("recipient") User recipient);
    
    @Query("SELECT DISTINCT cm.chatRoomId FROM ChatMessage cm")
    List<String> findDistinctChatRoomIds();
    
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(String chatRoomId);
}