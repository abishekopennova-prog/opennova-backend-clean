package com.opennova.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.opennova.service.UserService;
import com.opennova.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3002", "http://127.0.0.1:3002", "http://localhost:3003", "http://127.0.0.1:3003"})
public class NotificationController {

    @Autowired
    private UserService userService;

    @GetMapping("/user")
    public ResponseEntity<?> getUserNotifications() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // For now, return empty notifications list
            // In a real implementation, you would fetch from a notifications table
            List<Map<String, Object>> notifications = new ArrayList<>();
            
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch notifications: " + e.getMessage());
        }
    }

    @PostMapping("/mark-read/{id}")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id) {
        try {
            // In a real implementation, you would update the notification status in database
            Map<String, String> response = new HashMap<>();
            response.put("message", "Notification marked as read");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to mark notification as read: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> getUnreadNotificationCount() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // For now, return 0 unread notifications
            Map<String, Integer> response = new HashMap<>();
            response.put("count", 0);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch notification count: " + e.getMessage());
        }
    }
}