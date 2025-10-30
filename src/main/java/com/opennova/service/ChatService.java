package com.opennova.service;

import com.opennova.model.ChatMessage;
import com.opennova.model.User;
import com.opennova.model.UserRole;
import com.opennova.repository.ChatMessageRepository;
import com.opennova.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    public ChatMessage sendMessage(User sender, String message, String recipientEmail) {
        // Generate chat room ID
        String chatRoomId = generateChatRoomId(sender.getEmail(), recipientEmail);
        
        // Find recipient
        User recipient = userRepository.findByEmail(recipientEmail).orElse(null);
        
        ChatMessage chatMessage = new ChatMessage(sender, recipient, message, chatRoomId);
        return chatMessageRepository.save(chatMessage);
    }

    public ChatMessage sendMessageToSupport(User sender, String message) {
        // Find admin or owner to send message to
        User supportUser = findAvailableSupport();
        
        String chatRoomId = generateChatRoomId(sender.getEmail(), supportUser.getEmail());
        
        ChatMessage chatMessage = new ChatMessage(sender, supportUser, message, chatRoomId);
        return chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> getChatHistory(String chatRoomId) {
        return chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId);
    }

    public List<ChatMessage> getUserChats(User user) {
        return chatMessageRepository.findUserChats(user);
    }

    public String generateChatRoomId(String email1, String email2) {
        // Create consistent chat room ID regardless of order
        return email1.compareTo(email2) < 0 ? 
            email1 + "_" + email2 : 
            email2 + "_" + email1;
    }

    public String getUserChatRoomId(User user) {
        // For users, create chat room with support
        User supportUser = findAvailableSupport();
        return generateChatRoomId(user.getEmail(), supportUser.getEmail());
    }

    public List<String> getUserChatRooms(User user) {
        return chatMessageRepository.findUserChatRooms(user);
    }

    public Long getUnreadMessageCount(User user) {
        return chatMessageRepository.countUnreadMessages(user);
    }

    public void markMessagesAsRead(String chatRoomId, User recipient) {
        List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessages(recipient);
        unreadMessages.stream()
            .filter(msg -> msg.getChatRoomId().equals(chatRoomId))
            .forEach(msg -> {
                msg.setIsRead(true);
                chatMessageRepository.save(msg);
            });
    }

    private User findAvailableSupport() {
        // First try to find an admin
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        if (!admins.isEmpty()) {
            return admins.get(0);
        }
        
        // If no admin, find any owner
        List<User> owners = userRepository.findByRoleIn(List.of(
            UserRole.OWNER, 
            UserRole.HOTEL_OWNER, 
            UserRole.HOSPITAL_OWNER, 
            UserRole.SHOP_OWNER
        ));
        
        if (!owners.isEmpty()) {
            return owners.get(0);
        }
        
        // Fallback - this shouldn't happen in production
        throw new RuntimeException("No support staff available");
    }

    public List<User> getSupportStaff() {
        List<User> supportStaff = userRepository.findByRole(UserRole.ADMIN);
        supportStaff.addAll(userRepository.findByRoleIn(List.of(
            UserRole.OWNER, 
            UserRole.HOTEL_OWNER, 
            UserRole.HOSPITAL_OWNER, 
            UserRole.SHOP_OWNER
        )));
        return supportStaff;
    }

    public List<String> getAllChatRoomIds() {
        try {
            // Get all distinct chat rooms
            List<String> chatRoomIds = chatMessageRepository.findDistinctChatRoomIds();
            System.out.println("Found " + chatRoomIds.size() + " chat rooms");
            return chatRoomIds;
        } catch (Exception e) {
            System.err.println("Error getting all chat room IDs: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }

    public List<java.util.Map<String, Object>> getAllChatRooms() {
        try {
            // Get all distinct chat rooms
            List<String> chatRoomIds = chatMessageRepository.findDistinctChatRoomIds();
            
            return chatRoomIds.stream().map(chatRoomId -> {
                java.util.Map<String, Object> roomInfo = new java.util.HashMap<>();
                roomInfo.put("chatRoomId", chatRoomId);
                
                // Get latest message for this room
                List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId);
                if (!messages.isEmpty()) {
                    ChatMessage latestMessage = messages.get(0);
                    roomInfo.put("latestMessage", latestMessage.getMessage());
                    roomInfo.put("latestMessageTime", latestMessage.getCreatedAt());
                    roomInfo.put("senderName", latestMessage.getSender().getName());
                    roomInfo.put("senderEmail", latestMessage.getSender().getEmail());
                }
                
                // Count unread messages
                long unreadCount = messages.stream()
                    .filter(msg -> !msg.getIsRead())
                    .count();
                roomInfo.put("unreadCount", unreadCount);
                
                return roomInfo;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting all chat rooms: " + e.getMessage());
            // Return empty list if there's an error
            return new java.util.ArrayList<>();
        }
    }
}