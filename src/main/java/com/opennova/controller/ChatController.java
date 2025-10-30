package com.opennova.controller;

import com.opennova.model.ChatMessage;
import com.opennova.model.User;
import com.opennova.service.ChatService;
import com.opennova.service.UserService;
import com.opennova.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/guest/send")
    public ResponseEntity<?> sendGuestMessage(@RequestBody Map<String, String> messageData) {
        try {
            String message = messageData.get("message");
            String guestName = messageData.get("guestName");
            
            if (message == null || message.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Message cannot be empty");
                return ResponseEntity.badRequest().body(error);
            }

            if (guestName == null || guestName.trim().isEmpty()) {
                guestName = "Guest User";
            }

            // Log guest message for admin review (optional - could store in database)
            System.out.println("Guest message from " + guestName + ": " + message);

            // Create a helpful response for guest users
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message received successfully");
            
            Map<String, Object> chatMessage = new HashMap<>();
            chatMessage.put("id", "guest-response-" + System.currentTimeMillis());
            chatMessage.put("message", "Thank you for reaching out! To get personalized support and continue this conversation, please log in to your account. Our support team will be able to assist you better once you're authenticated.");
            chatMessage.put("sender", "admin");
            chatMessage.put("senderName", "Support Team");
            chatMessage.put("timestamp", java.time.LocalDateTime.now().toString());
            
            response.put("chatMessage", chatMessage);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error handling guest message: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to send message. Please try again.");
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> messageData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            System.out.println("=== Chat Send Authentication Debug ===");
            System.out.println("Authentication: " + authentication);
            System.out.println("Is authenticated: " + (authentication != null ? authentication.isAuthenticated() : "null"));
            System.out.println("Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));
            System.out.println("Name: " + (authentication != null ? authentication.getName() : "null"));
            
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                System.err.println("Authentication failed for chat send - returning 401");
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated email: " + email);
            
            if (email == null || email.trim().isEmpty() || "anonymousUser".equals(email)) {
                System.err.println("Invalid email from authentication: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "Invalid authentication");
                return ResponseEntity.status(401).body(error);
            }
            
            User sender = userService.findByEmailSafe(email);

            if (sender == null) {
                System.err.println("User not found for email: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }

            System.out.println("User found: " + sender.getName() + " with role: " + sender.getRole());

            String message = messageData.get("message");
            if (message == null || message.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Message cannot be empty");
                return ResponseEntity.badRequest().body(error);
            }

            String chatRoomId = messageData.get("chatRoomId");
            ChatMessage chatMessage;
            
            // Check if this is an admin/owner replying to a specific chat room
            if (chatRoomId != null && !chatRoomId.trim().isEmpty() && 
                ("ADMIN".equals(sender.getRole().toString()) || 
                 "OWNER".equals(sender.getRole().toString()) ||
                 "HOTEL_OWNER".equals(sender.getRole().toString()) ||
                 "HOSPITAL_OWNER".equals(sender.getRole().toString()) ||
                 "SHOP_OWNER".equals(sender.getRole().toString()))) {
                
                System.out.println("Admin/Owner sending message to chat room: " + chatRoomId);
                
                // Find the other user in this chat room
                String[] emails = chatRoomId.split("_");
                String recipientEmail = null;
                for (String emailInRoom : emails) {
                    if (!emailInRoom.equals(sender.getEmail())) {
                        recipientEmail = emailInRoom;
                        break;
                    }
                }
                
                if (recipientEmail != null) {
                    System.out.println("Sending admin message to recipient: " + recipientEmail);
                    chatMessage = chatService.sendMessage(sender, message.trim(), recipientEmail);
                } else {
                    System.out.println("Could not find recipient in chat room, falling back to support");
                    // Fallback to sendMessageToSupport if we can't find recipient
                    chatMessage = chatService.sendMessageToSupport(sender, message.trim());
                }
            } else {
                // Regular user sending message to support
                chatMessage = chatService.sendMessageToSupport(sender, message.trim());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message sent successfully");
            response.put("chatMessage", formatChatMessage(chatMessage));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in sendMessage: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to send message: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(@RequestParam(required = false) String chatRoomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            System.out.println("=== Chat History Authentication Debug ===");
            System.out.println("Authentication: " + authentication);
            System.out.println("Is authenticated: " + (authentication != null ? authentication.isAuthenticated() : "null"));
            System.out.println("Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));
            System.out.println("Name: " + (authentication != null ? authentication.getName() : "null"));
            
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                System.err.println("Authentication failed for chat history - returning 401");
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated email: " + email);
            
            if (email == null || email.trim().isEmpty() || "anonymousUser".equals(email)) {
                System.err.println("Invalid email from authentication: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "Invalid authentication");
                return ResponseEntity.status(401).body(error);
            }
            
            User user = userService.findByEmailSafe(email);

            if (user == null) {
                System.err.println("User not found for email: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }

            System.out.println("User found: " + user.getName() + " with role: " + user.getRole());

            // If no chatRoomId provided, get user's default chat room
            if (chatRoomId == null || chatRoomId.trim().isEmpty()) {
                try {
                    chatRoomId = chatService.getUserChatRoomId(user);
                    System.out.println("Generated chat room ID: " + chatRoomId);
                } catch (Exception e) {
                    System.err.println("Error getting user chat room ID: " + e.getMessage());
                    return ResponseEntity.ok(new java.util.ArrayList<>());
                }
            }

            List<ChatMessage> messages = chatService.getChatHistory(chatRoomId);
            System.out.println("Retrieved " + messages.size() + " messages for chat room: " + chatRoomId);

            // Mark messages as read
            try {
                chatService.markMessagesAsRead(chatRoomId, user);
            } catch (Exception e) {
                System.err.println("Error marking messages as read: " + e.getMessage());
                // Continue even if marking as read fails
            }

            List<Map<String, Object>> formattedMessages = messages.stream()
                .map(this::formatChatMessage)
                .collect(Collectors.toList());

            return ResponseEntity.ok(formattedMessages);
        } catch (Exception e) {
            System.err.println("Error in getChatHistory: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", "Failed to load chat history: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/rooms")
    public ResponseEntity<?> getUserChatRooms() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Enhanced authentication debugging
            System.out.println("=== Chat Rooms Authentication Debug ===");
            System.out.println("Authentication object: " + authentication);
            System.out.println("Is authenticated: " + (authentication != null ? authentication.isAuthenticated() : "null"));
            System.out.println("Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));
            System.out.println("Name: " + (authentication != null ? authentication.getName() : "null"));
            System.out.println("Authorities: " + (authentication != null ? authentication.getAuthorities() : "null"));
            
            // Check if user is authenticated
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("Authentication failed: " + (authentication == null ? "null authentication" : "not authenticated"));
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "Full authentication is required to access this resource");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            if (email == null || email.trim().isEmpty()) {
                System.err.println("Authentication failed: empty email");
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "Invalid authentication token");
                return ResponseEntity.status(401).body(error);
            }
            
            System.out.println("Authenticated user email: " + email);
            
            User user = userService.findByEmailSafe(email);
            if (user == null) {
                System.err.println("User not found for email: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }

            System.out.println("User found: " + user.getName() + " with role: " + user.getRole());

            // Check if user has appropriate role for chat management
            if (!"ADMIN".equals(user.getRole().toString()) && 
                !"OWNER".equals(user.getRole().toString()) &&
                !"HOTEL_OWNER".equals(user.getRole().toString()) &&
                !"HOSPITAL_OWNER".equals(user.getRole().toString()) &&
                !"SHOP_OWNER".equals(user.getRole().toString())) {
                System.out.println("User role " + user.getRole() + " not authorized for chat management");
                return ResponseEntity.ok(new java.util.ArrayList<>());
            }

            // Check if user is admin - if so, return all chat rooms
            if ("ADMIN".equals(user.getRole().toString())) {
                try {
                    List<String> allChatRoomIds = chatService.getAllChatRoomIds();
                    System.out.println("Admin user - returning " + allChatRoomIds.size() + " chat rooms");
                    return ResponseEntity.ok(allChatRoomIds);
                } catch (Exception e) {
                    System.err.println("Error getting admin chat rooms: " + e.getMessage());
                    // If chat service fails, return empty list for admin
                    return ResponseEntity.ok(new java.util.ArrayList<>());
                }
            }

            // For owner users, return their chat rooms
            try {
                List<String> chatRooms = chatService.getUserChatRooms(user);
                System.out.println("Owner user - returning " + chatRooms.size() + " chat rooms");
                return ResponseEntity.ok(chatRooms);
            } catch (Exception e) {
                System.err.println("Error getting user chat rooms: " + e.getMessage());
                return ResponseEntity.ok(new java.util.ArrayList<>());
            }
        } catch (Exception e) {
            System.err.println("Error in getUserChatRooms: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", "Failed to load chat rooms: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/debug")
    public ResponseEntity<?> debugAuth(HttpServletRequest request) {
        Map<String, Object> debug = new HashMap<>();
        
        // Check headers
        String authHeader = request.getHeader("Authorization");
        debug.put("authHeader", authHeader != null ? "Bearer ***" : "null");
        debug.put("hasAuthHeader", authHeader != null);
        debug.put("authHeaderLength", authHeader != null ? authHeader.length() : 0);
        
        // Check authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        debug.put("hasAuthentication", authentication != null);
        debug.put("isAuthenticated", authentication != null ? authentication.isAuthenticated() : false);
        debug.put("principal", authentication != null ? authentication.getPrincipal().toString() : "null");
        debug.put("name", authentication != null ? authentication.getName() : "null");
        debug.put("authorities", authentication != null ? authentication.getAuthorities().toString() : "null");
        
        return ResponseEntity.ok(debug);
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("valid", false);
                response.put("error", "Missing or invalid Authorization header");
                return ResponseEntity.ok(response);
            }
            
            String token = authHeader.substring(7);
            
            // Try to extract username from token
            try {
                String username = jwtUtil.extractUsername(token);
                Date expiration = jwtUtil.extractExpiration(token);
                
                response.put("valid", true);
                response.put("username", username);
                response.put("expiration", expiration.toString());
                response.put("isExpired", expiration.before(new Date()));
                
                // Try to load user details
                User user = userService.findByEmailSafe(username);
                if (user != null) {
                    response.put("userExists", true);
                    response.put("userName", user.getName());
                    response.put("userRole", user.getRole().toString());
                } else {
                    response.put("userExists", false);
                }
                
            } catch (Exception e) {
                response.put("valid", false);
                response.put("error", "Token parsing failed: " + e.getMessage());
            }
            
        } catch (Exception e) {
            response.put("valid", false);
            response.put("error", "Validation failed: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/auth-status")
    public ResponseEntity<?> getAuthStatus(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            System.out.println("=== Auth Status Check ===");
            System.out.println("Request URI: " + request.getRequestURI());
            System.out.println("Authorization Header: " + (request.getHeader("Authorization") != null ? "Present" : "Missing"));
            System.out.println("Authentication: " + authentication);
            System.out.println("Is authenticated: " + (authentication != null ? authentication.isAuthenticated() : "null"));
            System.out.println("Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));
            System.out.println("Name: " + (authentication != null ? authentication.getName() : "null"));
            
            Map<String, Object> response = new HashMap<>();
            
            // Check if Authorization header is present
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("Auth status: Missing or invalid Authorization header");
                response.put("authenticated", false);
                response.put("message", "Missing or invalid Authorization header");
                response.put("hasAuthHeader", false);
                return ResponseEntity.ok(response);
            }
            
            response.put("hasAuthHeader", true);
            
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                System.out.println("Auth status: Not authenticated");
                response.put("authenticated", false);
                response.put("message", "Not authenticated");
                return ResponseEntity.ok(response);
            }
            
            String email = authentication.getName();
            System.out.println("Auth status: Checking user for email: " + email);
            
            if (email == null || email.trim().isEmpty() || "anonymousUser".equals(email)) {
                System.out.println("Auth status: Invalid email");
                response.put("authenticated", false);
                response.put("message", "Invalid email");
                return ResponseEntity.ok(response);
            }
            
            User user = userService.findByEmailSafe(email);

            if (user == null) {
                System.out.println("Auth status: User not found for email: " + email);
                response.put("authenticated", false);
                response.put("message", "User not found");
                return ResponseEntity.ok(response);
            }

            System.out.println("Auth status: User authenticated - " + user.getName() + " (" + user.getRole() + ")");
            response.put("authenticated", true);
            response.put("user", user.getName());
            response.put("role", user.getRole().toString());
            response.put("email", user.getEmail());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Auth status check failed: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            response.put("message", "Authentication check failed: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadMessageCount() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, Object> response = new HashMap<>();
                response.put("unreadCount", 0);
                return ResponseEntity.ok(response);
            }
            
            String email = authentication.getName();
            User user = userService.findByEmailSafe(email);

            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("unreadCount", 0);
                return ResponseEntity.ok(response);
            }

            Long unreadCount = chatService.getUnreadMessageCount(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("unreadCount", unreadCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("unreadCount", 0);
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/mark-read")
    public ResponseEntity<?> markMessagesAsRead(@RequestBody Map<String, String> data) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.findByEmailSafe(email);

            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }

            String chatRoomId = data.get("chatRoomId");
            if (chatRoomId == null) {
                chatRoomId = chatService.getUserChatRoomId(user);
            }

            chatService.markMessagesAsRead(chatRoomId, user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Messages marked as read");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to mark messages as read: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private Map<String, Object> formatChatMessage(ChatMessage message) {
        Map<String, Object> formatted = new HashMap<>();
        formatted.put("id", message.getId());
        formatted.put("message", message.getMessage());
        formatted.put("sender", message.getSender().getRole().toString().toLowerCase());
        formatted.put("senderName", message.getSender().getName());
        formatted.put("senderEmail", message.getSender().getEmail());
        formatted.put("timestamp", message.getCreatedAt().toString());
        formatted.put("isRead", message.getIsRead());
        formatted.put("chatRoomId", message.getChatRoomId());
        return formatted;
    }
}