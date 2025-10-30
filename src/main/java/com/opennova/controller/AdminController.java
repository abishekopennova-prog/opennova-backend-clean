package com.opennova.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.opennova.service.UserService;
import com.opennova.service.SharedStateService;
import com.opennova.service.EstablishmentService;
import com.opennova.service.ReviewService;

import com.opennova.service.EmailService;
import com.opennova.service.EstablishmentRequestService;
import com.opennova.model.User;
import java.util.ArrayList;
import com.opennova.model.UserRole;
import com.opennova.model.Establishment;
import com.opennova.model.EstablishmentStatus;
import com.opennova.model.EstablishmentType;
import com.opennova.model.EstablishmentRequest;
import com.opennova.model.RequestStatus;
import java.util.Optional;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3002", "http://127.0.0.1:3002", "http://localhost:3003", "http://127.0.0.1:3003"}, maxAge = 3600)
public class AdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private SharedStateService sharedStateService;

    @Autowired
    private EstablishmentService establishmentService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EstablishmentRequestService establishmentRequestService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // Helper method to get authenticated admin user safely
    private User getAuthenticatedAdmin() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmailSafe(email);
            
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                return null;
            }
            return admin;
        } catch (Exception e) {
            System.err.println("Error getting authenticated admin: " + e.getMessage());
            return null;
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> testAuth() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmailSafe(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Authentication successful");
            response.put("user", admin != null ? admin.getEmail() : "Unknown");
            response.put("role", admin != null ? admin.getRole().name() : "Unknown");
            
            // Add total user count for debugging
            long totalUsers = userService.getTotalUsers();
            response.put("totalUsersInDB", totalUsers);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(401).body(error);
        }
    }

    @GetMapping("/generate-password-hash")
    public ResponseEntity<?> generatePasswordHash(@RequestParam String password) {
        try {
            String hashedPassword = passwordEncoder.encode(password);
            
            Map<String, Object> response = new HashMap<>();
            response.put("password", password);
            response.put("hashedPassword", hashedPassword);
            response.put("message", "Password hash generated successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to generate password hash: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }



    @GetMapping("/stats")
    public ResponseEntity<?> getAdminStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmailSafe(email);
            
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get real data from database and services
            Map<String, Object> stats = new HashMap<>();
            
            // Get real counts from database
            long totalUsers = userService.getTotalUsers();
            long totalEstablishments = establishmentService.getTotalEstablishments();
            long totalBookings = establishmentService.getTotalBookings();
            long totalReviews = reviewService.getTotalReviews();
            long pendingRequests = establishmentRequestService.countPendingRequests();
            long activeEstablishments = establishmentService.getActiveEstablishments();
            
            stats.put("totalUsers", totalUsers);
            stats.put("totalEstablishments", totalEstablishments);
            stats.put("totalBookings", totalBookings);
            stats.put("pendingRequests", pendingRequests);
            stats.put("totalReviews", totalReviews);
            stats.put("activeEstablishments", activeEstablishments);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch admin stats: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<?> getRecentActivity() {
        try {
            // Mock data for now - replace with actual database queries
            List<Map<String, Object>> activities = new ArrayList<>();
            
            Map<String, Object> activity1 = new HashMap<>();
            activity1.put("icon", "🏢");
            activity1.put("title", "New establishment registered");
            activity1.put("description", "Hotel Paradise submitted registration request");
            activity1.put("time", "2 hours ago");
            activities.add(activity1);
            
            Map<String, Object> activity2 = new HashMap<>();
            activity2.put("icon", "👤");
            activity2.put("title", "New user registered");
            activity2.put("description", "John Doe created a new account");
            activity2.put("time", "4 hours ago");
            activities.add(activity2);
            
            Map<String, Object> activity3 = new HashMap<>();
            activity3.put("icon", "📅");
            activity3.put("title", "Booking completed");
            activity3.put("description", "Booking #1234 was successfully completed");
            activity3.put("time", "6 hours ago");
            activities.add(activity3);
            
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch recent activity");
            return ResponseEntity.badRequest().body(error);
        }
    }



    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            System.out.println("=== Admin Users Endpoint Called ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated user email: " + email);
            
            // Check if user has admin role
            boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!hasAdminRole) {
                System.err.println("User does not have ADMIN role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get admin user with better error handling
            User admin = null;
            try {
                admin = userService.findByEmailSafe(email);
            } catch (Exception e) {
                System.err.println("Error finding admin user: " + e.getMessage());
            }
            
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                System.err.println("Admin user not found or not admin role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            System.out.println("Admin user found: " + admin.getName() + " with role: " + admin.getRole().name());
            
            // Get real users from database with error handling
            List<User> realUsers = null;
            try {
                realUsers = userService.findAll();
                System.out.println("Found " + realUsers.size() + " users in database");
            } catch (Exception e) {
                System.err.println("Error fetching users from database: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Failed to fetch users: " + e.getMessage());
                return ResponseEntity.status(500).body(error);
            }
            
            List<Map<String, Object>> users = new ArrayList<>();
            
            // Convert to response format with null checks
            for (User user : realUsers) {
                try {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("name", user.getName() != null ? user.getName() : "Unknown");
                    userMap.put("email", user.getEmail() != null ? user.getEmail() : "Unknown");
                    userMap.put("role", user.getRole() != null ? user.getRole().name() : "USER");
                    userMap.put("status", user.getIsActive() != null && user.getIsActive() ? "ACTIVE" : "SUSPENDED");
                    userMap.put("isActive", user.getIsActive() != null ? user.getIsActive() : false);
                    userMap.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : LocalDateTime.now().toString());
                    userMap.put("joinDate", user.getCreatedAt() != null ? user.getCreatedAt().toString() : LocalDateTime.now().toString());
                    userMap.put("lastLogin", null); // Can be added later if needed
                    userMap.put("emailVerified", true); // Default to true for now
                    userMap.put("passwordResetRequired", false); // Default to false
                    users.add(userMap);
                } catch (Exception e) {
                    System.err.println("Error processing user " + user.getId() + ": " + e.getMessage());
                    // Skip this user and continue with others
                }
            }
            
            System.out.println("Returning " + users.size() + " users to admin panel");
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            System.err.println("Error in getAllUsers: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch users: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    private List<Map<String, Object>> createMockUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        
        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", 1L);
        user1.put("name", "John Doe");
        user1.put("email", "john@example.com");
        user1.put("role", "USER");
        user1.put("status", "ACTIVE");
        user1.put("isActive", true);
        user1.put("createdAt", LocalDateTime.now().toString());
        user1.put("joinDate", LocalDateTime.now().toString());
        user1.put("lastLogin", null);
        user1.put("emailVerified", true);
        user1.put("passwordResetRequired", false);
        users.add(user1);
        
        Map<String, Object> user2 = new HashMap<>();
        user2.put("id", 2L);
        user2.put("name", "Jane Smith");
        user2.put("email", "jane@example.com");
        user2.put("role", "USER");
        user2.put("status", "ACTIVE");
        user2.put("isActive", true);
        user2.put("createdAt", LocalDateTime.now().toString());
        user2.put("joinDate", LocalDateTime.now().toString());
        user2.put("lastLogin", null);
        user2.put("emailVerified", true);
        user2.put("passwordResetRequired", false);
        users.add(user2);
        
        Map<String, Object> admin = new HashMap<>();
        admin.put("id", 3L);
        admin.put("name", "Admin User");
        admin.put("email", "admin@opennova.com");
        admin.put("role", "ADMIN");
        admin.put("status", "ACTIVE");
        admin.put("isActive", true);
        admin.put("createdAt", LocalDateTime.now().toString());
        admin.put("joinDate", LocalDateTime.now().toString());
        admin.put("lastLogin", null);
        admin.put("emailVerified", true);
        admin.put("passwordResetRequired", false);
        users.add(admin);
        
        return users;
    }

    @GetMapping("/establishments")
    public ResponseEntity<?> getAllEstablishments() {
        try {
            System.out.println("=== Admin Establishments Endpoint Called ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated user email: " + email);
            
            // Check if user has admin role
            boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!hasAdminRole) {
                System.err.println("User does not have ADMIN role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get admin user
            User admin = userService.findByEmailSafe(email);
            if (admin == null) {
                System.err.println("Admin user not found in database");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Admin user not found");
                return ResponseEntity.status(403).body(error);
            }
            
            System.out.println("Admin user found: " + admin.getName());
            
            // Get real establishments from database
            List<Establishment> realEstablishments = establishmentService.findAllForAdmin();
            System.out.println("Found " + realEstablishments.size() + " establishments in database");
            
            List<Map<String, Object>> establishments = new ArrayList<>();
            
            // Convert to response format
            for (Establishment est : realEstablishments) {
                Map<String, Object> estMap = new HashMap<>();
                estMap.put("id", est.getId());
                estMap.put("name", est.getName());
                estMap.put("type", est.getType().name());
                estMap.put("email", est.getEmail());
                estMap.put("address", est.getAddress());
                estMap.put("contactNumber", est.getContactNumber());
                estMap.put("status", est.getStatus().name());
                estMap.put("isActive", est.getIsActive());
                estMap.put("createdAt", est.getCreatedAt() != null ? est.getCreatedAt().toString() : LocalDateTime.now().toString());
                establishments.add(estMap);
            }
            
            // Add mock data if no real establishments exist (for testing purposes)
            if (establishments.isEmpty()) {
                System.out.println("No establishments found, adding mock data");
                Map<String, Object> est1 = new HashMap<>();
                est1.put("id", 1L);
                est1.put("name", "Grand Palace Hotel");
                est1.put("type", "HOTEL");
                est1.put("email", "hotel@example.com");
                est1.put("address", "123 Main Street, City");
                est1.put("contactNumber", "9876543210");
                est1.put("status", "OPEN");
                est1.put("isActive", true);
                est1.put("createdAt", LocalDateTime.now().toString());
                establishments.add(est1);
                
                Map<String, Object> est2 = new HashMap<>();
                est2.put("id", 2L);
                est2.put("name", "City Care Hospital");
                est2.put("type", "HOSPITAL");
                est2.put("email", "hospital@example.com");
                est2.put("address", "456 Health Avenue, City");
                est2.put("contactNumber", "9876543211");
                est2.put("status", "OPEN");
                est2.put("isActive", true);
                est2.put("createdAt", LocalDateTime.now().toString());
                establishments.add(est2);
            }
            
            System.out.println("Returning " + establishments.size() + " establishments to admin panel");
            return ResponseEntity.ok(establishments);
        } catch (Exception e) {
            System.err.println("Error in getAllEstablishments: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch establishments: " + e.getMessage());
            error.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/users/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmailSafe(email);
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get user and toggle status
            User user;
            try {
                user = userService.findById(id);
                if (user == null) {
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "User not found");
                    return ResponseEntity.badRequest().body(error);
                }
            } catch (Exception e) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found: " + e.getMessage());
                return ResponseEntity.badRequest().body(error);
            }
            
            // Toggle user active status
            boolean newStatus = !user.getIsActive();
            user.setIsActive(newStatus);
            user.setUpdatedAt(LocalDateTime.now());
            userService.save(user);
            
            // Send notification email
            try {
                String subject = newStatus ? "Account Reactivated" : "Account Suspended";
                String body = String.format(
                    "Dear %s,\n\n" +
                    "Your account status has been updated by the administrator.\n\n" +
                    "Account Status: %s\n" +
                    "Email: %s\n\n" +
                    "%s\n\n" +
                    "If you have any questions, please contact our support team.\n\n" +
                    "Best regards,\n" +
                    "OpenNova Admin Team",
                    user.getName(),
                    newStatus ? "ACTIVE" : "SUSPENDED",
                    user.getEmail(),
                    newStatus ? "Your account has been reactivated and you can now access all services." 
                              : "Your account has been suspended. Please contact support for more information."
                );
                
                emailService.sendEmail(user.getEmail(), subject, body);
            } catch (Exception emailError) {
                System.err.println("Failed to send status change email: " + emailError.getMessage());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User status updated successfully");
            response.put("userId", id.toString());
            response.put("newStatus", newStatus ? "ACTIVE" : "SUSPENDED");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update user status: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/establishments/{id}/toggle-status")
    public ResponseEntity<?> toggleEstablishmentStatus(@PathVariable Long id) {
        try {
            System.out.println("=== Admin Toggle Establishment Status Called for ID: " + id + " ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated user email: " + email);
            
            // Check if user has admin role
            boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!hasAdminRole) {
                System.err.println("User does not have ADMIN role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get admin user with better error handling
            User admin = null;
            try {
                admin = userService.findByEmailSafe(email);
            } catch (Exception e) {
                System.err.println("Error finding admin user: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Database error while finding admin user");
                return ResponseEntity.status(500).body(error);
            }
            
            if (admin == null) {
                System.err.println("Admin user not found in database");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Admin user not found");
                return ResponseEntity.status(403).body(error);
            }
            
            System.out.println("Admin user found: " + admin.getName());
            
            // Get establishment and toggle status with better error handling
            Establishment establishment = null;
            try {
                establishment = establishmentService.findById(id);
                if (establishment == null) {
                    System.err.println("Establishment not found with ID: " + id);
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "Establishment not found with ID: " + id);
                    return ResponseEntity.status(404).body(error);
                }
            } catch (Exception e) {
                System.err.println("Database error finding establishment: " + e.getMessage());
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("message", "Database error while finding establishment: " + e.getMessage());
                return ResponseEntity.status(500).body(error);
            }
            
            System.out.println("Found establishment: " + establishment.getName() + " (Current active: " + establishment.getIsActive() + ")");
            
            // Toggle establishment active status with null check
            Boolean currentStatus = establishment.getIsActive();
            boolean newActiveStatus = currentStatus == null ? true : !currentStatus;
            establishment.setIsActive(newActiveStatus);
            establishment.setUpdatedAt(LocalDateTime.now());
            
            try {
                establishmentService.save(establishment);
                System.out.println("Establishment status updated successfully. New status: " + newActiveStatus);
            } catch (Exception e) {
                System.err.println("Error saving establishment: " + e.getMessage());
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("message", "Failed to save establishment: " + e.getMessage());
                return ResponseEntity.status(500).body(error);
            }
            
            // Send notification email with error handling
            try {
                if (establishment.getEmail() != null && !establishment.getEmail().trim().isEmpty()) {
                    String subject = newActiveStatus ? "Establishment Reactivated" : "Establishment Suspended";
                    String body = String.format(
                        "Dear %s,\n\n" +
                        "Your establishment status has been updated by the administrator.\n\n" +
                        "Establishment: %s\n" +
                        "Status: %s\n" +
                        "Email: %s\n\n" +
                        "%s\n\n" +
                        "If you have any questions, please contact our support team.\n\n" +
                        "Best regards,\n" +
                        "OpenNova Admin Team",
                        establishment.getName() != null ? establishment.getName() : "Establishment",
                        establishment.getName() != null ? establishment.getName() : "Unknown",
                        newActiveStatus ? "ACTIVE" : "SUSPENDED",
                        establishment.getEmail(),
                        newActiveStatus ? "Your establishment has been reactivated and can now accept bookings." 
                                        : "Your establishment has been suspended. Please contact support for more information."
                    );
                    
                    emailService.sendEmail(establishment.getEmail(), subject, body);
                    System.out.println("Status change email sent successfully");
                } else {
                    System.out.println("Skipping email notification - no valid email address");
                }
            } catch (Exception emailError) {
                System.err.println("Failed to send status change email: " + emailError.getMessage());
                // Don't fail the request if email fails
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Establishment status updated successfully");
            response.put("establishmentId", id.toString());
            response.put("newStatus", newActiveStatus ? "ACTIVE" : "SUSPENDED");
            response.put("isActive", newActiveStatus);
            System.out.println("Returning success response");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in toggleEstablishmentStatus: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update establishment status: " + e.getMessage());
            error.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/establishments/{id}")
    public ResponseEntity<?> deleteEstablishment(@PathVariable Long id) {
        try {
            System.out.println("=== Admin Delete Establishment Called for ID: " + id + " ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated user email: " + email);
            
            // Check if user has admin role
            boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!hasAdminRole) {
                System.err.println("User does not have ADMIN role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get admin user
            User admin = null;
            try {
                admin = userService.findByEmailSafe(email);
            } catch (Exception e) {
                System.err.println("Error finding admin user: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Database error while finding admin user");
                return ResponseEntity.status(500).body(error);
            }
            
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                System.err.println("Admin user not found or not admin role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Admin user not found");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get establishment to delete
            Establishment establishmentToDelete = null;
            try {
                establishmentToDelete = establishmentService.findById(id);
                if (establishmentToDelete == null) {
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "Establishment not found with ID: " + id);
                    return ResponseEntity.status(404).body(error);
                }
            } catch (Exception e) {
                System.err.println("Error finding establishment to delete: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Database error while finding establishment: " + e.getMessage());
                return ResponseEntity.status(500).body(error);
            }
            
            System.out.println("Found establishment to delete: " + establishmentToDelete.getName());
            
            // Delete establishment with cascade
            boolean deleted = establishmentService.deleteEstablishmentWithCascade(id);
            if (!deleted) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Failed to delete establishment");
                return ResponseEntity.status(500).body(error);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Establishment deleted successfully");
            response.put("establishmentId", id.toString());
            response.put("deletedEstablishment", establishmentToDelete.getName());
            System.out.println("Returning success response");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in deleteEstablishment: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to delete establishment: " + e.getMessage());
            error.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            System.out.println("=== Admin Delete User Called for ID: " + id + " ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated user email: " + email);
            
            // Check if user has admin role
            boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!hasAdminRole) {
                System.err.println("User does not have ADMIN role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get admin user
            User admin = null;
            try {
                admin = userService.findByEmailSafe(email);
            } catch (Exception e) {
                System.err.println("Error finding admin user: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Database error while finding admin user");
                return ResponseEntity.status(500).body(error);
            }
            
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                System.err.println("Admin user not found or not admin role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Admin user not found");
                return ResponseEntity.status(403).body(error);
            }
            
            // Check if trying to delete self
            if (admin.getId().equals(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Cannot delete your own admin account");
                return ResponseEntity.status(400).body(error);
            }
            
            // Get user to delete
            User userToDelete = null;
            try {
                userToDelete = userService.findById(id);
                if (userToDelete == null) {
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "User not found with ID: " + id);
                    return ResponseEntity.status(404).body(error);
                }
            } catch (Exception e) {
                System.err.println("Error finding user to delete: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Database error while finding user: " + e.getMessage());
                return ResponseEntity.status(500).body(error);
            }
            
            System.out.println("Found user to delete: " + userToDelete.getName() + " (" + userToDelete.getEmail() + ")");
            
            // Delete user
            try {
                userService.deleteById(id);
                System.out.println("User deleted successfully");
            } catch (Exception e) {
                System.err.println("Error deleting user: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Failed to delete user: " + e.getMessage());
                return ResponseEntity.status(500).body(error);
            }
            
            // Send notification email
            try {
                String subject = "Account Deleted";
                String body = String.format(
                    "Dear %s,\n\n" +
                    "Your account has been deleted by the administrator.\n\n" +
                    "Account Details:\n" +
                    "Name: %s\n" +
                    "Email: %s\n\n" +
                    "If you believe this was done in error, please contact our support team.\n\n" +
                    "Best regards,\n" +
                    "OpenNova Admin Team",
                    userToDelete.getName(),
                    userToDelete.getName(),
                    userToDelete.getEmail()
                );
                
                emailService.sendEmail(userToDelete.getEmail(), subject, body);
                System.out.println("Deletion notification email sent successfully");
            } catch (Exception emailError) {
                System.err.println("Failed to send deletion notification email: " + emailError.getMessage());
                // Don't fail the request if email fails
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            response.put("userId", id.toString());
            response.put("deletedUser", userToDelete.getName());
            System.out.println("Returning success response");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in deleteUser: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to delete user: " + e.getMessage());
            error.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Update establishment location (Admin)
     */
    @PutMapping("/establishments/{id}/location")
    public ResponseEntity<?> updateEstablishmentLocation(@PathVariable Long id, @RequestBody Map<String, Object> locationData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmail(email);
            
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            System.out.println("Admin location update request for establishment ID: " + id);
            System.out.println("Location data: " + locationData);
            
            // Extract location data
            Double latitude = null;
            Double longitude = null;
            String address = null;
            
            if (locationData.containsKey("latitude")) {
                Object latObj = locationData.get("latitude");
                if (latObj != null && !latObj.toString().trim().isEmpty()) {
                    try {
                        latitude = Double.valueOf(latObj.toString());
                    } catch (NumberFormatException e) {
                        latitude = null;
                    }
                }
            }
            
            if (locationData.containsKey("longitude")) {
                Object lngObj = locationData.get("longitude");
                if (lngObj != null && !lngObj.toString().trim().isEmpty()) {
                    try {
                        longitude = Double.valueOf(lngObj.toString());
                    } catch (NumberFormatException e) {
                        longitude = null;
                    }
                }
            }
            
            if (locationData.containsKey("address")) {
                address = (String) locationData.get("address");
            }
            
            // Update establishment location
            Establishment updatedEstablishment = establishmentService.updateLocation(id, latitude, longitude, address);
            
            if (updatedEstablishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Location updated successfully");
            response.put("success", true);
            response.put("establishment", updatedEstablishment);
            
            System.out.println("Location update successful for establishment: " + updatedEstablishment.getName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Failed to update establishment location: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update location: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Delete establishment location coordinates (Admin)
     */
    @DeleteMapping("/establishments/{id}/location")
    public ResponseEntity<?> deleteEstablishmentLocation(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmail(email);
            
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            System.out.println("Admin location delete request for establishment ID: " + id);
            
            // Delete establishment location
            Establishment updatedEstablishment = establishmentService.deleteLocation(id);
            
            if (updatedEstablishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Location coordinates deleted successfully");
            response.put("success", true);
            response.put("establishment", updatedEstablishment);
            
            System.out.println("Location delete successful for establishment: " + updatedEstablishment.getName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Failed to delete establishment location: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to delete location: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get establishments with location data (Admin)
     */
    @GetMapping("/establishments/with-location")
    public ResponseEntity<?> getEstablishmentsWithLocation() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmail(email);
            
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            List<Establishment> establishmentsWithLocation = establishmentService.findEstablishmentsWithLocation();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("establishments", establishmentsWithLocation);
            response.put("count", establishmentsWithLocation.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Failed to fetch establishments with location: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch establishments: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/establishment-requests")
    public ResponseEntity<?> getAllEstablishmentRequests() {
        try {
            System.out.println("=== Admin Establishment Requests Endpoint Called ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated user email: " + email);
            
            // Check if user has admin role
            boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!hasAdminRole) {
                System.err.println("User does not have ADMIN role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get all establishment requests
            List<Map<String, Object>> requests = sharedStateService.getAllEstablishmentRequests();
            System.out.println("Found " + requests.size() + " establishment requests");
            
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            System.err.println("Error in getAllEstablishmentRequests: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch establishment requests: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/establishment-requests/{id}/approve")
    public ResponseEntity<?> approveEstablishmentRequest(@PathVariable Long id) {
        try {
            System.out.println("=== Admin Approve Establishment Request Called for ID: " + id + " ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated user email: " + email);
            
            // Check if user has admin role
            boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!hasAdminRole) {
                System.err.println("User does not have ADMIN role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get the establishment request
            Map<String, Object> request = sharedStateService.getEstablishmentRequest(id);
            if (request == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment request not found with ID: " + id);
                return ResponseEntity.status(404).body(error);
            }
            
            System.out.println("Found establishment request: " + request.get("name"));
            
            // Create new establishment from request
            try {
                Establishment establishment = new Establishment();
                establishment.setName((String) request.get("name"));
                establishment.setType(EstablishmentType.valueOf((String) request.get("type")));
                establishment.setEmail((String) request.get("email"));
                establishment.setAddress((String) request.get("address"));
                establishment.setStatus(EstablishmentStatus.OPEN);
                establishment.setIsActive(true);
                establishment.setCreatedAt(LocalDateTime.now());
                establishment.setUpdatedAt(LocalDateTime.now());
                
                // Save establishment
                Establishment savedEstablishment = establishmentService.save(establishment);
                System.out.println("Created establishment with ID: " + savedEstablishment.getId());
                
                // Update request status and remove from pending
                sharedStateService.updateEstablishmentRequestStatus(id, "APPROVED");
                sharedStateService.removeEstablishmentRequest(id);
                
                // Send approval email
                try {
                    String subject = "Establishment Request Approved";
                    String body = String.format(
                        "Dear %s,\n\n" +
                        "Great news! Your establishment request has been approved.\n\n" +
                        "Establishment Details:\n" +
                        "Name: %s\n" +
                        "Type: %s\n" +
                        "Email: %s\n" +
                        "Address: %s\n\n" +
                        "Your establishment is now live on our platform and can accept bookings.\n" +
                        "You can log in using the email address provided in the request.\n\n" +
                        "Welcome to OpenNova!\n\n" +
                        "Best regards,\n" +
                        "OpenNova Admin Team",
                        request.get("requestedByName"),
                        request.get("name"),
                        request.get("type"),
                        request.get("email"),
                        request.get("address")
                    );
                    
                    emailService.sendEmail((String) request.get("email"), subject, body);
                    System.out.println("Approval email sent successfully");
                } catch (Exception emailError) {
                    System.err.println("Failed to send approval email: " + emailError.getMessage());
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Establishment request approved and establishment created successfully");
                response.put("requestId", id.toString());
                response.put("establishmentId", savedEstablishment.getId());
                response.put("establishmentName", savedEstablishment.getName());
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                System.err.println("Error creating establishment: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Failed to create establishment: " + e.getMessage());
                return ResponseEntity.status(500).body(error);
            }
        } catch (Exception e) {
            System.err.println("Error in approveEstablishmentRequest: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to approve establishment request: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/establishment-requests/{id}/reject")
    public ResponseEntity<?> rejectEstablishmentRequest(@PathVariable Long id, @RequestBody Map<String, String> rejectionData) {
        try {
            System.out.println("=== Admin Reject Establishment Request Called for ID: " + id + " ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated user email: " + email);
            
            // Check if user has admin role
            boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!hasAdminRole) {
                System.err.println("User does not have ADMIN role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get the establishment request
            Map<String, Object> request = sharedStateService.getEstablishmentRequest(id);
            if (request == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment request not found with ID: " + id);
                return ResponseEntity.status(404).body(error);
            }
            
            System.out.println("Found establishment request: " + request.get("name"));
            
            String rejectionReason = rejectionData.get("reason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                rejectionReason = "Request did not meet our requirements.";
            }
            
            // Update request status and remove from pending
            sharedStateService.updateEstablishmentRequestStatus(id, "REJECTED");
            sharedStateService.removeEstablishmentRequest(id);
            
            // Send rejection email
            try {
                String subject = "Establishment Request Rejected";
                String body = String.format(
                    "Dear %s,\n\n" +
                    "Thank you for your interest in joining OpenNova. Unfortunately, we cannot approve your establishment request at this time.\n\n" +
                    "Establishment Details:\n" +
                    "Name: %s\n" +
                    "Type: %s\n" +
                    "Email: %s\n\n" +
                    "Reason for rejection:\n" +
                    "%s\n\n" +
                    "You are welcome to submit a new request after addressing the concerns mentioned above.\n\n" +
                    "If you have any questions, please contact our support team.\n\n" +
                    "Best regards,\n" +
                    "OpenNova Admin Team",
                    request.get("requestedByName"),
                    request.get("name"),
                    request.get("type"),
                    request.get("email"),
                    rejectionReason
                );
                
                emailService.sendEmail((String) request.get("requestedByEmail"), subject, body);
                System.out.println("Rejection email sent successfully");
            } catch (Exception emailError) {
                System.err.println("Failed to send rejection email: " + emailError.getMessage());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Establishment request rejected successfully");
            response.put("requestId", id.toString());
            response.put("reason", rejectionReason);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in rejectEstablishmentRequest: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to reject establishment request: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/establishments/{id}/reset-password")
    public ResponseEntity<?> resetEstablishmentPassword(@PathVariable Long id, @RequestBody Map<String, Object> requestData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            System.out.println("Establishment password reset request from user: " + email);
            
            User admin;
            try {
                admin = userService.findByEmail(email);
                System.out.println("Admin user found: " + admin.getName() + " with role: " + admin.getRole().name());
            } catch (Exception e) {
                System.err.println("Failed to find admin user: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Admin user not found");
                return ResponseEntity.status(403).body(error);
            }
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                System.err.println("Access denied - user is not admin: " + (admin != null ? admin.getRole().name() : "null"));
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get establishment
            Establishment establishment;
            try {
                establishment = establishmentService.findById(id);
                if (establishment == null) {
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "Establishment not found");
                    return ResponseEntity.badRequest().body(error);
                }
            } catch (Exception e) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment not found: " + e.getMessage());
                return ResponseEntity.badRequest().body(error);
            }
            
            // Generate or use provided password
            String newPassword = (String) requestData.get("newPassword");
            System.out.println("Received password reset request for establishment ID: " + id);
            System.out.println("Provided password: " + (newPassword != null ? "[PROVIDED]" : "[NONE - WILL GENERATE]"));
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                newPassword = generateRandomPassword();
                System.out.println("Generated new password: " + newPassword);
            }
            
            // Hash the password before storing
            String hashedPassword = passwordEncoder.encode(newPassword);
            System.out.println("Password hashed successfully");
            
            // Update establishment password
            establishment.setPassword(hashedPassword);
            establishment.setUpdatedAt(LocalDateTime.now());
            Establishment savedEstablishment = establishmentService.save(establishment);
            System.out.println("Establishment password updated successfully for: " + establishment.getName());
            
            // Also update owner user password if exists
            User ownerUser = userService.findByEmailSafe(establishment.getEmail());
            if (ownerUser != null) {
                ownerUser.setPassword(hashedPassword);
                ownerUser.setUpdatedAt(LocalDateTime.now());
                userService.save(ownerUser);
                System.out.println("Updated owner user password for: " + ownerUser.getEmail());
            } else {
                System.out.println("No owner user found for establishment email: " + establishment.getEmail());
            }
            
            // Send email with new password
            try {
                String subject = "Password Reset - OpenNova";
                String body = String.format(
                    "Dear %s,\n\n" +
                    "Your password has been reset by the administrator.\n\n" +
                    "New Login Credentials:\n" +
                    "Email: %s\n" +
                    "New Password: %s\n\n" +
                    "Please log in with these credentials and change your password immediately for security purposes.\n\n" +
                    "Login URL: http://localhost:3000/login\n\n" +
                    "If you have any questions, please contact our support team.\n\n" +
                    "Best regards,\n" +
                    "OpenNova Admin Team",
                    establishment.getName(),
                    establishment.getEmail(),
                    newPassword
                );
                
                emailService.sendEmail(establishment.getEmail(), subject, body);
            } catch (Exception emailError) {
                System.err.println("Failed to send password reset email: " + emailError.getMessage());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password reset successfully");
            response.put("establishmentId", id.toString());
            response.put("newPassword", newPassword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to reset password: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id, @RequestBody Map<String, Object> requestData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmail(email);
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get user
            User user;
            try {
                user = userService.findById(id);
                if (user == null) {
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "User not found");
                    return ResponseEntity.badRequest().body(error);
                }
            } catch (Exception e) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found: " + e.getMessage());
                return ResponseEntity.badRequest().body(error);
            }
            
            // Generate or use provided password
            String newPassword = (String) requestData.get("newPassword");
            System.out.println("Received password reset request for user ID: " + id);
            System.out.println("User: " + user.getName() + " (" + user.getEmail() + ")");
            System.out.println("Provided password: " + (newPassword != null ? "[PROVIDED]" : "[NONE - WILL GENERATE]"));
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                newPassword = generateRandomPassword();
                System.out.println("Generated new password: " + newPassword);
            }
            
            // Hash the password before storing
            String hashedPassword = passwordEncoder.encode(newPassword);
            System.out.println("Password hashed successfully");
            
            // Update user password
            user.setPassword(hashedPassword);
            user.setUpdatedAt(LocalDateTime.now());
            User savedUser = userService.save(user);
            System.out.println("User password updated successfully for: " + user.getName());
            
            // Send email with new password
            try {
                String subject = "Password Reset - OpenNova";
                String body = String.format(
                    "Dear %s,\n\n" +
                    "Your password has been reset by the administrator.\n\n" +
                    "New Login Credentials:\n" +
                    "Email: %s\n" +
                    "New Password: %s\n\n" +
                    "Please log in with these credentials and change your password immediately for security purposes.\n\n" +
                    "Login URL: http://localhost:3000/login\n\n" +
                    "If you have any questions, please contact our support team.\n\n" +
                    "Best regards,\n" +
                    "OpenNova Admin Team",
                    user.getName(),
                    user.getEmail(),
                    newPassword
                );
                
                emailService.sendEmail(user.getEmail(), subject, body);
            } catch (Exception emailError) {
                System.err.println("Failed to send password reset email: " + emailError.getMessage());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Password reset successfully");
            response.put("userId", id.toString());
            response.put("newPassword", newPassword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to reset password: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmail(email);
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Delete review using review service
            boolean deleted = reviewService.deleteReview(id, admin.getId(), "ADMIN");
            
            if (!deleted) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Review not found or already deleted");
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Review deleted successfully");
            response.put("deletedId", id.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to delete review");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmail(email);
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get the request from database
            Optional<EstablishmentRequest> requestOpt = establishmentRequestService.findById(id);
            if (!requestOpt.isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Request not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            EstablishmentRequest request = requestOpt.get();
            
            // Extract request data
            String establishmentName = request.getName();
            String establishmentType = request.getType().name();
            String establishmentEmail = request.getEmail();
            String establishmentAddress = request.getAddress();
            String notes = request.getNotes();
            
            // Check if establishment already exists
            Establishment existingEstablishment = establishmentService.findByEmail(establishmentEmail);
            if (existingEstablishment != null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment with this email already exists");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Generate password for owner account
            String generatedPassword = generateRandomPassword();
            String hashedPassword = passwordEncoder.encode(generatedPassword);
            
            // Create establishment
            Establishment establishment = new Establishment();
            establishment.setName(establishmentName);
            establishment.setType(EstablishmentType.valueOf(establishmentType));
            establishment.setEmail(establishmentEmail);
            establishment.setPassword(hashedPassword); // Properly hashed password
            establishment.setAddress(establishmentAddress);
            establishment.setStatus(EstablishmentStatus.OPEN); // Automatically approved
            establishment.setIsActive(true);
            establishment.setCreatedAt(LocalDateTime.now());
            establishment.setUpdatedAt(LocalDateTime.now());
            
            // Save establishment
            Establishment savedEstablishment = establishmentService.save(establishment);
            
            // Create owner user account
            User ownerUser = userService.findByEmailSafe(establishmentEmail);
            if (ownerUser == null) {
                ownerUser = new User();
                ownerUser.setName(establishmentName + " Owner");
                ownerUser.setEmail(establishmentEmail);
                ownerUser.setPassword(hashedPassword); // Properly hashed password
                ownerUser.setRole(UserRole.OWNER);
                ownerUser.setIsActive(true);
                ownerUser.setCreatedAt(LocalDateTime.now());
                ownerUser.setUpdatedAt(LocalDateTime.now());
                
                // Set the establishment as owner
                savedEstablishment.setOwner(ownerUser);
                
                userService.save(ownerUser);
                establishmentService.save(savedEstablishment); // Update with owner reference
            }
            
            // Send approval email with credentials
            try {
                emailService.sendEstablishmentRequestApproval(establishmentEmail, establishmentName, establishmentEmail, generatedPassword);
                System.out.println("Approval email sent successfully to: " + establishmentEmail);
            } catch (Exception emailError) {
                System.err.println("Failed to send approval email: " + emailError.getMessage());
                // Continue with approval even if email fails
            }
            
            // Update request status to approved in database
            establishmentRequestService.updateStatus(id, RequestStatus.APPROVED, "Approved by admin");
            
            // Also update shared state for backward compatibility
            sharedStateService.updateEstablishmentRequestStatus(id, "APPROVED");
            sharedStateService.removeEstablishmentRequest(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Request approved successfully! Establishment created and credentials sent to " + establishmentEmail);
            response.put("requestId", id.toString());
            response.put("establishmentId", savedEstablishment.getId());
            response.put("establishmentName", establishmentName);
            response.put("ownerEmail", establishmentEmail);
            response.put("generatedPassword", generatedPassword);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to approve request: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Helper method to generate random password
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return "OpenNova" + password.toString();
    }



    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id, @RequestBody Map<String, String> rejectionData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmail(email);
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get the request from database
            Optional<EstablishmentRequest> requestOpt = establishmentRequestService.findById(id);
            if (!requestOpt.isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Request not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Update request status in database
            String reason = rejectionData.get("reason");
            String adminNotes = reason != null && !reason.trim().isEmpty() ? reason.trim() : "Rejected by admin";
            establishmentRequestService.updateStatus(id, RequestStatus.REJECTED, adminNotes);
            
            // Also update shared state for backward compatibility
            sharedStateService.updateEstablishmentRequestStatus(id, "REJECTED");
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Request rejected successfully");
            response.put("requestId", id.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to reject request");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<?> deleteRequest(@PathVariable Long id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmail(email);
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Debug: Log what we're trying to delete
            System.out.println("DEBUG: Attempting to delete request with ID=" + id);
            System.out.println("DEBUG: Current requests before deletion:");
            List<Map<String, Object>> currentRequests = sharedStateService.getAllEstablishmentRequests();
            for (Map<String, Object> req : currentRequests) {
                System.out.println("DEBUG: - Request ID=" + req.get("id") + " (type: " + req.get("id").getClass().getSimpleName() + "), Name=" + req.get("name"));
            }
            
            // Check if request exists in database
            Optional<EstablishmentRequest> requestOpt = establishmentRequestService.findById(id);
            if (!requestOpt.isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Request not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Delete from database
            establishmentRequestService.deleteRequest(id);
            
            // Also remove from shared state for backward compatibility
            boolean removed = sharedStateService.removeEstablishmentRequest(id);
            System.out.println("DEBUG: Removal result from shared state: " + removed);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Request deleted successfully");
            response.put("deletedId", id.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to delete request");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/locations")
    public ResponseEntity<?> getAllLocations() {
        try {
            System.out.println("=== Admin Locations Endpoint Called ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated user email: " + email);
            
            // Check if user has admin role
            boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!hasAdminRole) {
                System.err.println("User does not have ADMIN role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get admin user
            User admin = userService.findByEmailSafe(email);
            if (admin == null) {
                System.err.println("Admin user not found in database");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Admin user not found");
                return ResponseEntity.status(403).body(error);
            }
            
            System.out.println("Admin user found: " + admin.getName());
            
            // Get all establishments with location data
            List<Establishment> establishments = establishmentService.findAll();
            System.out.println("Found " + establishments.size() + " establishments");
            
            List<Map<String, Object>> locations = new ArrayList<>();
            
            for (Establishment est : establishments) {
                if (est.getLatitude() != null && est.getLongitude() != null) {
                    Map<String, Object> location = new HashMap<>();
                    location.put("id", est.getId());
                    location.put("establishmentName", est.getName());
                    location.put("establishmentType", est.getType().name());
                    location.put("address", est.getAddress());
                    location.put("latitude", est.getLatitude());
                    location.put("longitude", est.getLongitude());
                    location.put("status", est.getStatus().name());
                    location.put("isActive", est.getIsActive());
                    locations.add(location);
                }
            }
            
            System.out.println("Returning " + locations.size() + " locations with coordinates");
            return ResponseEntity.ok(locations);
        } catch (Exception e) {
            System.err.println("Error in getAllLocations: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch locations: " + e.getMessage());
            error.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/locations")
    public ResponseEntity<?> createLocation(@RequestBody Map<String, Object> locationData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmail(email);
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Extract location data
            Long establishmentId = Long.valueOf(locationData.get("establishmentId").toString());
            Double latitude = Double.valueOf(locationData.get("latitude").toString());
            Double longitude = Double.valueOf(locationData.get("longitude").toString());
            
            // Update establishment with location
            Establishment establishment = establishmentService.findById(establishmentId);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            establishment.setLatitude(latitude);
            establishment.setLongitude(longitude);
            establishment.setUpdatedAt(LocalDateTime.now());
            establishmentService.save(establishment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Location updated successfully");
            response.put("establishmentId", establishmentId);
            response.put("latitude", latitude);
            response.put("longitude", longitude);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to create/update location: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/locations/{id}")
    public ResponseEntity<?> updateLocation(@PathVariable Long id, @RequestBody Map<String, Object> locationData) {
        try {
            System.out.println("=== Admin Update Location Endpoint Called for ID: " + id + " ===");
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmailSafe(email);
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Find establishment
            Establishment establishment = establishmentService.findById(id);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment not found");
                return ResponseEntity.status(404).body(error);
            }
            
            // Extract and validate location data
            Double latitude = null;
            Double longitude = null;
            
            try {
                if (locationData.get("latitude") != null) {
                    latitude = Double.valueOf(locationData.get("latitude").toString());
                }
                if (locationData.get("longitude") != null) {
                    longitude = Double.valueOf(locationData.get("longitude").toString());
                }
            } catch (NumberFormatException e) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Invalid latitude or longitude format");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Update establishment location
            establishment.setLatitude(latitude);
            establishment.setLongitude(longitude);
            establishment.setUpdatedAt(LocalDateTime.now());
            establishmentService.save(establishment);
            
            System.out.println("Location updated successfully for establishment: " + establishment.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Location updated successfully");
            response.put("establishmentId", id);
            response.put("establishmentName", establishment.getName());
            response.put("latitude", latitude);
            response.put("longitude", longitude);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error updating location: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update location: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/locations/{id}")
    public ResponseEntity<?> deleteLocation(@PathVariable Long id) {
        try {
            System.out.println("=== Admin Delete Location Endpoint Called for ID: " + id + " ===");
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmailSafe(email);
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Remove location data from establishment
            Establishment establishment = establishmentService.findById(id);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment not found");
                return ResponseEntity.status(404).body(error);
            }
            
            String establishmentName = establishment.getName();
            establishment.setLatitude(null);
            establishment.setLongitude(null);
            establishment.setUpdatedAt(LocalDateTime.now());
            establishmentService.save(establishment);
            
            System.out.println("Location data removed successfully for establishment: " + establishmentName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Location data removed successfully");
            response.put("establishmentId", id);
            response.put("establishmentName", establishmentName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error deleting location: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to delete location: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getAllRequests() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmail(email);
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get actual establishment requests from database
            List<EstablishmentRequest> dbRequests = establishmentRequestService.getAllRequests();
            List<Map<String, Object>> requests = new ArrayList<>();
            
            // Convert to response format
            for (EstablishmentRequest req : dbRequests) {
                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("id", req.getId());
                requestMap.put("name", req.getName());
                requestMap.put("type", req.getType().name());
                requestMap.put("email", req.getEmail());
                requestMap.put("address", req.getAddress());
                requestMap.put("notes", req.getNotes());
                requestMap.put("status", req.getStatus().name());
                requestMap.put("requestedBy", req.getUser().getId());
                requestMap.put("requestedByName", req.getUser().getName());
                requestMap.put("requestedByEmail", req.getUser().getEmail());
                requestMap.put("createdAt", req.getCreatedAt().toString());
                requestMap.put("adminNotes", req.getAdminNotes());
                requests.add(requestMap);
            }
            
            // Also get from shared state for backward compatibility and merge
            List<Map<String, Object>> sharedRequests = sharedStateService.getAllEstablishmentRequests();
            for (Map<String, Object> sharedReq : sharedRequests) {
                // Check if this request is not already in database results
                Long sharedId = (Long) sharedReq.get("id");
                boolean existsInDb = requests.stream().anyMatch(r -> r.get("id").equals(sharedId));
                if (!existsInDb) {
                    requests.add(sharedReq);
                }
            }
            
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch requests: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/reviews")
    public ResponseEntity<?> getAllReviews() {
        try {
            System.out.println("=== Admin Reviews Endpoint Called ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("Authenticated user email: " + email);
            
            // Check if user has admin role
            boolean hasAdminRole = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (!hasAdminRole) {
                System.err.println("User does not have ADMIN role");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get admin user
            User admin = userService.findByEmailSafe(email);
            if (admin == null) {
                System.err.println("Admin user not found in database");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Admin user not found");
                return ResponseEntity.status(403).body(error);
            }
            
            System.out.println("Admin user found: " + admin.getName());
            
            // Get all reviews from review service
            List<Map<String, Object>> allReviews = new ArrayList<>();
            
            // Get real reviews from database
            try {
                allReviews = reviewService.getAllReviewsForAdmin();
                System.out.println("Found " + allReviews.size() + " reviews in database");
            } catch (Exception e) {
                System.err.println("Failed to fetch reviews from database: " + e.getMessage());
                // Return empty list if database query fails
                allReviews = new ArrayList<>();
            }
            
            System.out.println("Returning " + allReviews.size() + " reviews to admin panel");
            return ResponseEntity.ok(allReviews);
        } catch (Exception e) {
            System.err.println("Error in getAllReviews: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch reviews: " + e.getMessage());
            error.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
        }
    }





    @PostMapping("/establishments")
    public ResponseEntity<?> createEstablishment(@RequestBody Map<String, Object> establishmentData) {
        try {
            System.out.println("=== Admin Create Establishment Called ===");
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User admin = userService.findByEmailSafe(email);
            
            // Check if user is admin
            if (admin == null || !admin.getRole().name().equals("ADMIN")) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Access denied. Admin privileges required.");
                return ResponseEntity.status(403).body(error);
            }
            
            // Extract establishment data
            String name = (String) establishmentData.get("name");
            String establishmentEmail = (String) establishmentData.get("email");
            String address = (String) establishmentData.get("address");
            String contactNumber = (String) establishmentData.get("contactNumber");
            String typeStr = (String) establishmentData.get("type");
            String operatingHours = (String) establishmentData.get("operatingHours");
            String upiId = (String) establishmentData.get("upiId");
            String providedPassword = (String) establishmentData.get("password");
            
            // Validate required fields
            if (name == null || name.trim().isEmpty() ||
                establishmentEmail == null || establishmentEmail.trim().isEmpty() ||
                address == null || address.trim().isEmpty() ||
                typeStr == null || typeStr.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Missing required fields: name, email, address, and type are required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Check if establishment with this email already exists
            Establishment existingEst = establishmentService.findByEmail(establishmentEmail);
            if (existingEst != null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment with this email already exists");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Parse establishment type
            EstablishmentType type;
            try {
                type = EstablishmentType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Invalid establishment type. Must be HOTEL, HOSPITAL, or SHOP");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Create establishment
            Establishment establishment = new Establishment();
            establishment.setName(name.trim());
            establishment.setEmail(establishmentEmail.trim().toLowerCase());
            establishment.setAddress(address.trim());
            establishment.setType(type);
            establishment.setStatus(EstablishmentStatus.OPEN);
            establishment.setIsActive(true);
            establishment.setCreatedAt(LocalDateTime.now());
            establishment.setUpdatedAt(LocalDateTime.now());
            
            if (contactNumber != null && !contactNumber.trim().isEmpty()) {
                establishment.setContactNumber(contactNumber.trim());
            }
            if (operatingHours != null && !operatingHours.trim().isEmpty()) {
                establishment.setOperatingHours(operatingHours.trim());
            }
            if (upiId != null && !upiId.trim().isEmpty()) {
                establishment.setUpiId(upiId.trim());
            }
            
            // Use provided password or generate temporary password for owner user account
            String tempPassword;
            if (providedPassword != null && !providedPassword.trim().isEmpty()) {
                tempPassword = providedPassword.trim();
                System.out.println("✅ Using admin-provided password for owner account: " + establishmentEmail);
            } else {
                tempPassword = generateTemporaryPassword();
                System.out.println("🔐 Generated secure temporary password for owner account: " + establishmentEmail);
                System.out.println("Generated password: " + tempPassword);
            }
            
            // Set a placeholder password for establishment entity (required by @NotBlank)
            establishment.setPassword(passwordEncoder.encode("establishment_" + tempPassword));
            
            // Create owner user account
            User ownerUser = createOwnerAccount(establishment, tempPassword);
            if (ownerUser != null) {
                establishment.setOwner(ownerUser);
            }
            
            // Save establishment
            Establishment savedEstablishment = establishmentService.save(establishment);
            System.out.println("Establishment created successfully: " + savedEstablishment.getName());
            
            // Auto-approve any pending requests for this establishment email
            try {
                List<EstablishmentRequest> pendingRequests = establishmentRequestService.getAllRequests()
                    .stream()
                    .filter(req -> req.getEmail().equalsIgnoreCase(establishmentEmail) && 
                                  req.getStatus() == RequestStatus.PENDING)
                    .collect(Collectors.toList());
                
                for (EstablishmentRequest request : pendingRequests) {
                    establishmentRequestService.updateStatus(
                        request.getId(), 
                        RequestStatus.APPROVED, 
                        "Auto-approved: Establishment created by admin"
                    );
                    System.out.println("Auto-approved request ID: " + request.getId());
                }
                
                if (!pendingRequests.isEmpty()) {
                    System.out.println("Auto-approved " + pendingRequests.size() + " pending requests");
                }
            } catch (Exception e) {
                System.err.println("Error auto-approving requests: " + e.getMessage());
                // Don't fail the establishment creation if request approval fails
            }
            
            // Send credentials email to owner
            boolean emailSent = false;
            String emailError = null;
            if (ownerUser != null) {
                try {
                    System.out.println("Attempting to send credentials email to: " + establishmentEmail);
                    System.out.println("Owner user details - Name: " + ownerUser.getName() + ", Email: " + ownerUser.getEmail());
                    System.out.println("Temporary password: " + tempPassword);
                    
                    emailService.sendOwnerCredentials(ownerUser, tempPassword);
                    emailSent = true;
                    System.out.println("✅ Credentials email sent successfully to: " + establishmentEmail);
                } catch (Exception e) {
                    emailError = e.getMessage();
                    System.err.println("❌ Failed to send credentials email: " + e.getMessage());
                    e.printStackTrace();
                    // Don't fail the establishment creation if email fails
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            String message = "Establishment created successfully";
            if (emailSent) {
                message += " and owner credentials sent via email";
            } else if (emailError != null) {
                message += " but email failed to send: " + emailError;
            }
            
            response.put("message", message);
            response.put("id", savedEstablishment.getId());
            response.put("name", savedEstablishment.getName());
            response.put("email", savedEstablishment.getEmail());
            response.put("type", savedEstablishment.getType().name());
            response.put("ownerCreated", ownerUser != null);
            response.put("tempPassword", tempPassword); // Include the generated password in response
            response.put("emailSent", emailSent);
            if (emailError != null) {
                response.put("emailError", emailError);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error creating establishment: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to create establishment: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    private String generateTemporaryPassword() {
        // Generate a more secure and user-friendly temporary password
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "@#$%";
        
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        // Ensure at least one character from each category
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        
        // Fill remaining positions with random characters
        String allChars = upperCase + lowerCase + numbers + specialChars;
        for (int i = 4; i < 10; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Shuffle the password to randomize character positions
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }

    private User createOwnerAccount(Establishment establishment, String tempPassword) {
        try {
            // Check if user with this email already exists
            User existingUser = userService.findByEmailSafe(establishment.getEmail());
            if (existingUser != null) {
                System.out.println("User with email " + establishment.getEmail() + " already exists, updating to OWNER role");
                existingUser.setRole(UserRole.OWNER);
                existingUser.setIsActive(true);
                existingUser.setEstablishmentType(establishment.getType().toString());
                existingUser.setPassword(passwordEncoder.encode(tempPassword));
                existingUser.setUpdatedAt(LocalDateTime.now());
                return userService.save(existingUser);
            }
            
            User ownerUser = new User();
            ownerUser.setName(establishment.getName() + " Owner");
            ownerUser.setEmail(establishment.getEmail());
            ownerUser.setRole(UserRole.OWNER);
            ownerUser.setIsActive(true);
            ownerUser.setCreatedAt(LocalDateTime.now());
            ownerUser.setUpdatedAt(LocalDateTime.now());
            
            // Hash the temporary password
            String hashedPassword = passwordEncoder.encode(tempPassword);
            ownerUser.setPassword(hashedPassword);
            System.out.println("Setting password for user: " + establishment.getEmail() + " (hashed: " + hashedPassword.substring(0, 10) + "...)");
            
            // Set establishment type for portal redirection
            ownerUser.setEstablishmentType(establishment.getType().toString());
            
            User savedUser = userService.save(ownerUser);
            System.out.println("Owner account created successfully: " + savedUser.getEmail() + " with role: " + savedUser.getRole());
            
            // Verify the user was saved correctly
            User verifyUser = userService.findByEmailSafe(savedUser.getEmail());
            if (verifyUser != null) {
                System.out.println("Verification: User found in database with role: " + verifyUser.getRole() + " and active: " + verifyUser.getIsActive());
            } else {
                System.err.println("ERROR: User not found in database after saving!");
            }
            
            return savedUser;
        } catch (Exception e) {
            System.err.println("Failed to create owner account: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


}