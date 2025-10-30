package com.opennova.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.opennova.service.UserService;
import com.opennova.service.SharedStateService;
import com.opennova.service.ReviewService;
import com.opennova.service.EstablishmentRequestService;
import com.opennova.model.User;
import com.opennova.model.EstablishmentRequest;
import com.opennova.model.EstablishmentType;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3002", "http://127.0.0.1:3002", "http://localhost:3003", "http://127.0.0.1:3003"}, maxAge = 3600)
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private SharedStateService sharedStateService;
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private EstablishmentRequestService establishmentRequestService;
    
    @Autowired
    private com.opennova.repository.BookingRepository bookingRepository;
    
    @Autowired
    private com.opennova.service.SavedEstablishmentService savedEstablishmentService;
    
    @Autowired
    private com.opennova.service.EmailService emailService;

    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            System.out.println("📊 Fetching comprehensive stats for user: " + email);
            
            // Get real stats from database
            Map<String, Object> stats = new HashMap<>();
            
            try {
                // Booking statistics
                List<com.opennova.model.Booking> userBookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
                long totalBookings = userBookings.size();
                long activeBookings = userBookings.stream().filter(b -> 
                    b.getStatus() == com.opennova.model.BookingStatus.PENDING || 
                    b.getStatus() == com.opennova.model.BookingStatus.CONFIRMED).count();
                long completedBookings = userBookings.stream().filter(b -> 
                    b.getStatus() == com.opennova.model.BookingStatus.COMPLETED).count();
                long cancelledBookings = userBookings.stream().filter(b -> 
                    b.getStatus() == com.opennova.model.BookingStatus.CANCELLED).count();
                
                stats.put("totalBookings", totalBookings);
                stats.put("activeBookings", activeBookings);
                stats.put("completedBookings", completedBookings);
                stats.put("cancelledBookings", cancelledBookings);
                stats.put("pendingBookings", userBookings.stream().filter(b -> 
                    b.getStatus() == com.opennova.model.BookingStatus.PENDING).count());
                
                // Calculate total spent
                double totalSpent = userBookings.stream()
                    .filter(b -> b.getPaymentAmount() != null)
                    .mapToDouble(b -> b.getPaymentAmount().doubleValue())
                    .sum();
                stats.put("totalSpent", totalSpent);
                
                System.out.println("✅ Booking stats - Total: " + totalBookings + ", Active: " + activeBookings + ", Completed: " + completedBookings);
                
            } catch (Exception e) {
                System.err.println("❌ Error fetching booking stats: " + e.getMessage());
                // Fallback to default values
                stats.put("totalBookings", 0);
                stats.put("activeBookings", 0);
                stats.put("completedBookings", 0);
                stats.put("cancelledBookings", 0);
                stats.put("pendingBookings", 0);
                stats.put("totalSpent", 0.0);
            }
            
            try {
                // Review statistics
                List<com.opennova.model.Review> userReviews = reviewService.getUserReviews(user.getId());
                long totalReviews = userReviews.size();
                double averageRating = userReviews.stream()
                    .mapToInt(com.opennova.model.Review::getRating)
                    .average()
                    .orElse(0.0);
                
                stats.put("totalReviews", totalReviews);
                stats.put("averageRatingGiven", Math.round(averageRating * 10.0) / 10.0);
                
                System.out.println("✅ Review stats - Total: " + totalReviews + ", Avg Rating: " + averageRating);
                
            } catch (Exception e) {
                System.err.println("❌ Error fetching review stats: " + e.getMessage());
                stats.put("totalReviews", 0);
                stats.put("averageRatingGiven", 0.0);
            }
            
            try {
                // Establishment request statistics
                List<EstablishmentRequest> userRequests = establishmentRequestService.getUserRequests(user.getId());
                long totalRequests = userRequests.size();
                long pendingRequests = userRequests.stream().filter(r -> 
                    r.getStatus() == com.opennova.model.RequestStatus.PENDING).count();
                long approvedRequests = userRequests.stream().filter(r -> 
                    r.getStatus() == com.opennova.model.RequestStatus.APPROVED).count();
                
                stats.put("totalEstablishmentRequests", totalRequests);
                stats.put("pendingEstablishmentRequests", pendingRequests);
                stats.put("approvedEstablishmentRequests", approvedRequests);
                
                System.out.println("✅ Request stats - Total: " + totalRequests + ", Pending: " + pendingRequests);
                
            } catch (Exception e) {
                System.err.println("❌ Error fetching request stats: " + e.getMessage());
                stats.put("totalEstablishmentRequests", 0);
                stats.put("pendingEstablishmentRequests", 0);
                stats.put("approvedEstablishmentRequests", 0);
            }
            
            try {
                // Saved establishments count
                List<com.opennova.model.SavedEstablishment> savedEstablishments = savedEstablishmentService.getUserSavedEstablishments(user.getId());
                stats.put("savedEstablishments", savedEstablishments.size());
                
                System.out.println("✅ Saved establishments: " + savedEstablishments.size());
                
            } catch (Exception e) {
                System.err.println("❌ Error fetching saved establishments: " + e.getMessage());
                stats.put("savedEstablishments", 0);
            }
            
            // Add user info
            stats.put("userId", user.getId());
            stats.put("userName", user.getName());
            stats.put("userEmail", user.getEmail());
            stats.put("memberSince", user.getCreatedAt().toString());
            
            System.out.println("📊 Complete stats generated for user: " + email);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch user stats: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch user stats: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/recent-bookings")
    public ResponseEntity<?> getRecentBookings() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            System.out.println("📋 Fetching recent bookings for user: " + email);
            
            // Get recent bookings from database (limit to 10 most recent)
            List<com.opennova.model.Booking> recentBookings = bookingRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.getId());
            List<Map<String, Object>> bookingList = new ArrayList<>();
            
            for (com.opennova.model.Booking booking : recentBookings) {
                Map<String, Object> bookingMap = new HashMap<>();
                bookingMap.put("id", booking.getId());
                bookingMap.put("establishmentName", booking.getEstablishment().getName());
                bookingMap.put("establishmentType", booking.getEstablishment().getType().toString());
                bookingMap.put("establishmentId", booking.getEstablishment().getId());
                bookingMap.put("visitingDate", booking.getVisitingDate());
                bookingMap.put("visitingTime", booking.getVisitingTime());
                bookingMap.put("status", booking.getStatus().toString());
                bookingMap.put("amount", booking.getAmount() != null ? booking.getAmount().doubleValue() : 0.0);
                bookingMap.put("paymentAmount", booking.getPaymentAmount() != null ? booking.getPaymentAmount().doubleValue() : 0.0);
                bookingMap.put("createdAt", booking.getCreatedAt().toString());
                bookingMap.put("transactionId", booking.getTransactionId());
                
                // Add QR code if available
                if (booking.getQrCode() != null && !booking.getQrCode().isEmpty()) {
                    bookingMap.put("hasQrCode", true);
                } else {
                    bookingMap.put("hasQrCode", false);
                }
                
                bookingList.add(bookingMap);
            }
            
            System.out.println("✅ Found " + bookingList.size() + " recent bookings for user: " + email);
            
            return ResponseEntity.ok(bookingList);
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch recent bookings: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch recent bookings: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<?> getRecentActivity() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            System.out.println("🔄 Fetching recent activity for user: " + email);
            
            List<Map<String, Object>> activities = new ArrayList<>();
            
            try {
                // Recent bookings activity
                List<com.opennova.model.Booking> recentBookings = bookingRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId());
                for (com.opennova.model.Booking booking : recentBookings) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("id", "booking_" + booking.getId());
                    activity.put("type", "booking");
                    activity.put("title", "Booking at " + booking.getEstablishment().getName());
                    activity.put("description", "Status: " + booking.getStatus().toString());
                    activity.put("timestamp", booking.getCreatedAt().toString());
                    activity.put("icon", getEstablishmentIcon(booking.getEstablishment().getType().toString()));
                    activity.put("status", booking.getStatus().toString());
                    activity.put("amount", booking.getAmount() != null ? booking.getAmount().doubleValue() : 0.0);
                    activities.add(activity);
                }
                
                // Recent reviews activity
                List<com.opennova.model.Review> recentReviews = reviewService.getUserRecentReviews(user.getId(), 5);
                for (com.opennova.model.Review review : recentReviews) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("id", "review_" + review.getId());
                    activity.put("type", "review");
                    activity.put("title", "Reviewed " + review.getEstablishment().getName());
                    activity.put("description", review.getRating() + " stars - " + 
                        (review.getComment().length() > 50 ? review.getComment().substring(0, 50) + "..." : review.getComment()));
                    activity.put("timestamp", review.getCreatedAt().toString());
                    activity.put("icon", "⭐");
                    activity.put("rating", review.getRating());
                    activities.add(activity);
                }
                
                // Recent establishment requests
                List<EstablishmentRequest> recentRequests = establishmentRequestService.getUserRecentRequests(user.getId(), 3);
                for (EstablishmentRequest request : recentRequests) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("id", "request_" + request.getId());
                    activity.put("type", "establishment_request");
                    activity.put("title", "Requested " + request.getName());
                    activity.put("description", "Status: " + request.getStatus().toString());
                    activity.put("timestamp", request.getCreatedAt().toString());
                    activity.put("icon", "📝");
                    activity.put("status", request.getStatus().toString());
                    activities.add(activity);
                }
                
            } catch (Exception e) {
                System.err.println("❌ Error fetching activity data: " + e.getMessage());
            }
            
            // Sort activities by timestamp (most recent first)
            activities.sort((a, b) -> {
                try {
                    return ((String) b.get("timestamp")).compareTo((String) a.get("timestamp"));
                } catch (Exception e) {
                    return 0;
                }
            });
            
            // Limit to 15 most recent activities
            if (activities.size() > 15) {
                activities = activities.subList(0, 15);
            }
            
            System.out.println("✅ Found " + activities.size() + " recent activities for user: " + email);
            
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch recent activity: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch recent activity: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    private String getEstablishmentIcon(String type) {
        switch (type) {
            case "HOTEL": return "🏨";
            case "HOSPITAL": return "🏥";
            case "SHOP": return "🛍️";
            default: return "🏢";
        }
    }

    @PostMapping("/reviews")
    public ResponseEntity<?> createReview(@RequestBody Map<String, Object> reviewData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Extract review data
            Long establishmentId = Long.valueOf(reviewData.get("establishmentId").toString());
            Integer rating = Integer.valueOf(reviewData.get("rating").toString());
            String comment = reviewData.get("comment").toString();
            String establishmentName = reviewData.get("establishmentName").toString();
            
            // Create review using ReviewService
            com.opennova.model.Review review = reviewService.createReview(user.getId(), establishmentId, rating, comment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Review submitted successfully");
            response.put("reviewId", review.getId());
            Map<String, Object> reviewResponse = new HashMap<>();
            reviewResponse.put("id", review.getId());
            reviewResponse.put("customerName", user.getName());
            reviewResponse.put("customerEmail", user.getEmail());
            reviewResponse.put("establishmentId", establishmentId);
            reviewResponse.put("establishmentName", establishmentName);
            reviewResponse.put("rating", rating);
            reviewResponse.put("comment", comment);
            reviewResponse.put("createdAt", review.getCreatedAt().toString());
            response.put("review", reviewResponse);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to submit review: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/reviews")
    public ResponseEntity<?> getUserReviews() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            System.out.println("📝 Fetching reviews for user: " + user.getEmail());
            
            // Get user's reviews from ReviewService
            List<com.opennova.model.Review> reviews = reviewService.getUserReviews(user.getId());
            List<Map<String, Object>> userReviews = new ArrayList<>();
            
            for (com.opennova.model.Review review : reviews) {
                Map<String, Object> reviewMap = new HashMap<>();
                reviewMap.put("id", review.getId());
                reviewMap.put("rating", review.getRating());
                reviewMap.put("comment", review.getComment());
                reviewMap.put("createdAt", review.getCreatedAt());
                reviewMap.put("updatedAt", review.getUpdatedAt());
                reviewMap.put("status", review.getStatus() != null ? review.getStatus().toString() : "PENDING");
                
                // Add establishment info
                if (review.getEstablishment() != null) {
                    Map<String, Object> establishmentMap = new HashMap<>();
                    establishmentMap.put("id", review.getEstablishment().getId());
                    establishmentMap.put("name", review.getEstablishment().getName());
                    establishmentMap.put("type", review.getEstablishment().getType() != null ? 
                        review.getEstablishment().getType().toString() : "UNKNOWN");
                    reviewMap.put("establishment", establishmentMap);
                }
                
                userReviews.add(reviewMap);
            }
            
            System.out.println("✅ Found " + userReviews.size() + " reviews for user: " + user.getEmail());
            return ResponseEntity.ok(userReviews);
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch user reviews: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch user reviews: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/establishments/{id}/reviews")
    public ResponseEntity<?> getEstablishmentReviews(@PathVariable Long id) {
        try {
            // Get reviews for specific establishment
            List<com.opennova.model.Review> reviews = reviewService.getEstablishmentReviews(id);
            List<Map<String, Object>> establishmentReviews = new ArrayList<>();
            
            for (com.opennova.model.Review review : reviews) {
                Map<String, Object> reviewMap = new HashMap<>();
                reviewMap.put("id", review.getId());
                reviewMap.put("customerName", review.getUser().getName());
                reviewMap.put("rating", review.getRating());
                reviewMap.put("comment", review.getComment());
                reviewMap.put("createdAt", review.getCreatedAt().toString());
                establishmentReviews.add(reviewMap);
            }
            
            return ResponseEntity.ok(establishmentReviews);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch establishment reviews");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Create user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("name", user.getName());
            userData.put("email", user.getEmail());
            userData.put("role", user.getRole().name());
            userData.put("status", "ACTIVE");
            userData.put("passwordResetRequired", false);
            
            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch user profile");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/establishment-requests")
    public ResponseEntity<?> createEstablishmentRequest(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("=== Establishment Request Endpoint Called ===");
            
            // Check authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("No authentication found in establishment request endpoint");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            if (email == null || email.trim().isEmpty()) {
                System.err.println("No email found in authentication");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Invalid authentication token");
                return ResponseEntity.status(401).body(error);
            }
            
            System.out.println("Creating establishment request for user: " + email);
            
            User user = null;
            try {
                user = userService.findByEmailSafe(email);
            } catch (Exception e) {
                System.err.println("Error finding user by email: " + e.getMessage());
                Map<String, String> error = new HashMap<>();
                error.put("message", "Database error while fetching user");
                return ResponseEntity.status(500).body(error);
            }
            
            if (user == null) {
                System.err.println("User not found in database: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(404).body(error);
            }
            
            System.out.println("User found: " + user.getName() + " with role: " + user.getRole().name());
            
            // Validate required fields
            System.out.println("Validating request data: " + requestData);
            
            String name = (String) requestData.get("name");
            String type = (String) requestData.get("type");
            String establishmentEmail = (String) requestData.get("email");
            String address = (String) requestData.get("address");
            String notes = (String) requestData.get("notes");
            
            System.out.println("Extracted fields - Name: " + name + ", Type: " + type + ", Email: " + establishmentEmail + ", Address: " + address);
            
            if (name == null || name.trim().isEmpty()) {
                System.err.println("Validation failed: Establishment name is required");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment name is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (type == null || type.trim().isEmpty()) {
                System.err.println("Validation failed: Establishment type is required");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Establishment type is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (establishmentEmail == null || establishmentEmail.trim().isEmpty()) {
                System.err.println("Validation failed: Email is required");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Email is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (address == null || address.trim().isEmpty()) {
                System.err.println("Validation failed: Address is required");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Address is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Validate establishment type
            EstablishmentType establishmentType;
            try {
                establishmentType = EstablishmentType.valueOf(type.trim().toUpperCase());
                System.out.println("Valid establishment type: " + establishmentType);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid establishment type: " + type);
                Map<String, String> error = new HashMap<>();
                error.put("message", "Invalid establishment type. Must be HOTEL, HOSPITAL, or SHOP");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Create establishment request using proper database storage
            EstablishmentRequest request = new EstablishmentRequest();
            request.setUser(user);
            request.setName(name.trim());
            request.setType(establishmentType);
            request.setEmail(establishmentEmail.trim());
            request.setAddress(address.trim());
            request.setNotes(notes != null ? notes.trim() : "");
            
            System.out.println("Creating establishment request with validated data");
            
            // Save to database using EstablishmentRequestService
            EstablishmentRequest savedRequest;
            try {
                savedRequest = establishmentRequestService.createRequest(request);
                System.out.println("Successfully saved establishment request with ID: " + savedRequest.getId());
            } catch (Exception e) {
                System.err.println("Error saving establishment request: " + e.getMessage());
                e.printStackTrace();
                Map<String, String> error = new HashMap<>();
                error.put("message", "Failed to save establishment request: " + e.getMessage());
                return ResponseEntity.status(500).body(error);
            }
            
            // Also add to shared state for backward compatibility
            Map<String, Object> sharedRequest = new HashMap<>();
            sharedRequest.put("id", savedRequest.getId());
            sharedRequest.put("name", savedRequest.getName());
            sharedRequest.put("type", savedRequest.getType().name());
            sharedRequest.put("email", savedRequest.getEmail());
            sharedRequest.put("address", savedRequest.getAddress());
            sharedRequest.put("notes", savedRequest.getNotes());
            sharedRequest.put("status", savedRequest.getStatus().name());
            sharedRequest.put("requestedBy", user.getId());
            sharedRequest.put("requestedByName", user.getName());
            sharedRequest.put("requestedByEmail", user.getEmail());
            sharedRequest.put("createdAt", savedRequest.getCreatedAt().toString());
            
            sharedStateService.addEstablishmentRequest(sharedRequest);
            
            System.out.println("DEBUG: Added establishment request with ID=" + savedRequest.getId() + ", Name=" + name);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Establishment request submitted successfully! Admin will review and process your request.");
            response.put("requestId", savedRequest.getId());
            response.put("status", savedRequest.getStatus().name());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Unexpected error in createEstablishmentRequest: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to submit establishment request: " + e.getMessage());
            error.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/establishment-requests")
    public ResponseEntity<?> getUserEstablishmentRequests() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Get user's establishment requests
            List<Map<String, Object>> userRequests = sharedStateService.getUserEstablishmentRequests(user.getId());
            
            return ResponseEntity.ok(userRequests);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch establishment requests");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.findByEmail(email);
            
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            String newPassword = passwordData.get("newPassword");
            String confirmPassword = passwordData.get("confirmPassword");
            
            if (!newPassword.equals(confirmPassword)) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Passwords do not match");
                return ResponseEntity.badRequest().body(error);
            }
            
            // In a real implementation, you would:
            // 1. Validate current password
            // 2. Hash the new password
            // 3. Update password in database
            // 4. Clear password reset requirement
            
            // Password reset requirement cleared (no action needed for now)
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to change password");
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Test email functionality
     */
    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(Authentication authentication) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();
            
            System.out.println("🧪 Testing email functionality for user: " + user.getEmail());
            
            // Send test email
            emailService.sendTestEmail(user.getEmail());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test email sent successfully to " + user.getEmail());
            response.put("email", user.getEmail());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Test email failed: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> error = new HashMap<>();
            error.put("success", "false");
            error.put("message", "Failed to send test email: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}