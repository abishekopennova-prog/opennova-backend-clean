package com.opennova.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.opennova.service.UserService;
import com.opennova.service.EstablishmentService;
import com.opennova.service.BookingService;
import com.opennova.service.MenuService;
import com.opennova.service.DoctorService;
import com.opennova.service.CollectionService;
import com.opennova.service.QRCodeService;
import com.opennova.service.EmailService;
import com.opennova.service.ReviewService;
import com.opennova.service.SharedStateService;
import com.opennova.service.RealTimeUpdateService;
import com.opennova.service.FileStorageService;
import com.opennova.model.User;
import com.opennova.model.Establishment;
import com.opennova.model.Booking;
import com.opennova.model.Menu;
import com.opennova.model.Doctor;
import com.opennova.dto.BookingResponseDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3002", "http://127.0.0.1:3002", "http://localhost:3003", "http://127.0.0.1:3003"}, maxAge = 3600)
public class OwnerController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private EstablishmentService establishmentService;
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private QRCodeService qrCodeService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private SharedStateService sharedStateService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private RealTimeUpdateService realTimeUpdateService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private com.opennova.service.ExcelExportService excelExportService;





    @PostMapping("/test-qr")
    public ResponseEntity<?> testQRGeneration(@RequestBody Map<String, Object> testData) {
        try {
            System.out.println("🧪 Testing QR code generation...");
            
            // Create a mock booking for testing
            Booking mockBooking = new Booking();
            mockBooking.setId(999L);
            mockBooking.setUserEmail("test@example.com");
            mockBooking.setVisitingDate("2025-10-15");
            mockBooking.setVisitingTime("14:30");
            mockBooking.setTransactionId("TEST_TXN_123");
            mockBooking.setAmount(new java.math.BigDecimal("1000.00"));
            mockBooking.setPaymentAmount(new java.math.BigDecimal("700.00"));
            mockBooking.setStatus(com.opennova.model.BookingStatus.CONFIRMED);
            
            // Mock user
            User mockUser = new User();
            mockUser.setName("Test User");
            mockUser.setEmail("test@example.com");
            mockBooking.setUser(mockUser);
            
            // Mock establishment
            Establishment mockEstablishment = new Establishment();
            mockEstablishment.setId(1L);
            mockEstablishment.setName("Test Hotel");
            mockEstablishment.setAddress("Test Address");
            mockEstablishment.setContactNumber("1234567890");
            mockEstablishment.setEmail("hotel@test.com");
            mockEstablishment.setType(com.opennova.model.EstablishmentType.HOTEL);
            mockBooking.setEstablishment(mockEstablishment);
            
            // Generate QR code
            String qrCode = qrCodeService.generateBookingQRCode(mockBooking);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "QR code generated successfully");
            response.put("qrCodeLength", qrCode.length());
            response.put("qrCodePreview", qrCode.substring(0, Math.min(100, qrCode.length())) + "...");
            
            // Test email sending if requested
            if (testData.containsKey("testEmail") && (Boolean) testData.get("testEmail")) {
                String testEmail = (String) testData.get("email");
                if (testEmail != null && !testEmail.trim().isEmpty()) {
                    System.out.println("📧 Testing email with QR attachment to: " + testEmail);
                    emailService.sendBookingQRCode(testEmail, mockBooking, qrCode);
                    response.put("emailSent", true);
                    response.put("emailRecipient", testEmail);
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ QR test failed: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "QR test failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getOwnerStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Mock data for now - replace with actual database queries
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBookings", 45);
            stats.put("pendingBookings", 8);
            stats.put("todayBookings", 3);
            stats.put("totalRevenue", 125000);
            stats.put("averageRating", 4.2);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch owner stats");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/recent-bookings")
    public ResponseEntity<?> getRecentBookings() {
        try {
            // Mock data for now - replace with actual database queries
            List<Map<String, Object>> bookings = new ArrayList<>();
            
            Map<String, Object> booking1 = new HashMap<>();
            booking1.put("id", 1);
            booking1.put("customerName", "John Doe");
            booking1.put("customerEmail", "john@example.com");
            booking1.put("customerPhone", "9876543210");
            booking1.put("bookingDate", "2025-10-07");
            booking1.put("bookingTime", "14:30");
            booking1.put("status", "CONFIRMED");
            booking1.put("amount", 2500);
            bookings.add(booking1);
            
            Map<String, Object> booking2 = new HashMap<>();
            booking2.put("id", 2);
            booking2.put("customerName", "Jane Smith");
            booking2.put("customerEmail", "jane@example.com");
            booking2.put("customerPhone", "9876543211");
            booking2.put("bookingDate", "2025-10-08");
            booking2.put("bookingTime", "10:00");
            booking2.put("status", "PENDING");
            booking2.put("amount", 1800);
            bookings.add(booking2);
            
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch recent bookings");
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Establishment Management
    @GetMapping("/establishment")
    public ResponseEntity<?> getEstablishment() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Always get fresh data from database - no cache dependency
            Establishment establishment = establishmentService.findByOwner(owner);
            Map<String, Object> establishmentData = new HashMap<>();
            
            if (establishment != null) {
                // Use actual database data
                establishmentData.put("id", establishment.getId());
                establishmentData.put("name", establishment.getName());
                establishmentData.put("address", establishment.getAddress());
                establishmentData.put("contactNumber", establishment.getContactNumber());
                establishmentData.put("status", establishment.getStatus() != null ? establishment.getStatus().toString() : "OPEN");
                establishmentData.put("upiId", establishment.getUpiId());
                establishmentData.put("operatingHours", establishment.getOperatingHours());
                establishmentData.put("type", establishment.getType() != null ? establishment.getType().toString() : "HOTEL");
                establishmentData.put("email", establishment.getEmail());
                establishmentData.put("latitude", establishment.getLatitude());
                establishmentData.put("longitude", establishment.getLongitude());
                establishmentData.put("profileImagePath", establishment.getProfileImagePath());
                establishmentData.put("upiQrCodePath", establishment.getUpiQrCodePath());
                establishmentData.put("weeklySchedule", establishment.getWeeklySchedule());
                
                System.out.println("✅ Returning fresh database establishment data for: " + email + " - " + establishment.getName());
            } else {
                System.out.println("❌ No establishment found for user: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this user");
                return ResponseEntity.status(404).body(error);
            }
            
            return ResponseEntity.ok(establishmentData);
        } catch (Exception e) {
            System.err.println("Failed to fetch establishment data: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch establishment data: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/establishment")
    public ResponseEntity<?> updateEstablishment(@RequestBody Map<String, Object> updateData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null) {
                System.err.println("❌ No authentication found for establishment update");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            System.out.println("🔍 Updating establishment for user: " + email);
            
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                System.err.println("❌ User not found in database: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found in database");
                return ResponseEntity.status(401).body(error);
            }
            
            if (!owner.getIsActive()) {
                System.err.println("❌ User account is inactive: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("message", "User account is inactive");
                return ResponseEntity.status(401).body(error);
            }
            
            // Check if user has owner-related role
            boolean hasOwnerRole = owner.getRole() == com.opennova.model.UserRole.OWNER ||
                                 owner.getRole() == com.opennova.model.UserRole.HOTEL_OWNER ||
                                 owner.getRole() == com.opennova.model.UserRole.HOSPITAL_OWNER ||
                                 owner.getRole() == com.opennova.model.UserRole.SHOP_OWNER;
            
            if (!hasOwnerRole) {
                System.err.println("❌ User does not have owner role: " + email + " (role: " + owner.getRole() + ")");
                Map<String, String> error = new HashMap<>();
                error.put("message", "Insufficient privileges. Owner role required.");
                return ResponseEntity.status(403).body(error);
            }
            
            System.out.println("✅ User authenticated successfully: " + email + " (role: " + owner.getRole() + ")");
            System.out.println("📝 Update data: " + updateData);
            
            // Get establishment from database - no cache dependency
            Establishment establishment = establishmentService.findByOwner(owner);
            
            if (establishment == null) {
                System.err.println("❌ No establishment found for user: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this user");
                return ResponseEntity.status(404).body(error);
            }
            
            // Create update object with only the fields that are being updated
            Establishment updateEstablishment = new Establishment();
            boolean hasUpdates = false;
            
            if (updateData.containsKey("name") && updateData.get("name") != null) {
                updateEstablishment.setName((String) updateData.get("name"));
                hasUpdates = true;
            }
            if (updateData.containsKey("address") && updateData.get("address") != null) {
                updateEstablishment.setAddress((String) updateData.get("address"));
                hasUpdates = true;
            }
            if (updateData.containsKey("contactNumber") && updateData.get("contactNumber") != null) {
                updateEstablishment.setContactNumber((String) updateData.get("contactNumber"));
                hasUpdates = true;
            }
            if (updateData.containsKey("upiId") && updateData.get("upiId") != null) {
                updateEstablishment.setUpiId((String) updateData.get("upiId"));
                hasUpdates = true;
            }
            if (updateData.containsKey("operatingHours") && updateData.get("operatingHours") != null) {
                updateEstablishment.setOperatingHours((String) updateData.get("operatingHours"));
                hasUpdates = true;
            }
            
            // Handle status update separately using dedicated method
            if (updateData.containsKey("status") && updateData.get("status") != null) {
                try {
                    String statusStr = (String) updateData.get("status");
                    establishmentService.updateStatusByOwner(establishment.getId(), statusStr, owner.getId());
                    System.out.println("✅ Updated establishment status in database: " + statusStr);
                } catch (Exception e) {
                    System.err.println("❌ Failed to update establishment status in database: " + e.getMessage());
                }
            }
            
            // Save all other changes to database
            Establishment updatedEstablishment = establishment;
            if (hasUpdates) {
                try {
                    updatedEstablishment = establishmentService.updateEstablishment(establishment.getId(), updateEstablishment);
                    System.out.println("✅ Updated establishment data in database for: " + updatedEstablishment.getName());
                } catch (Exception e) {
                    System.err.println("❌ Failed to save establishment to database: " + e.getMessage());
                    throw e; // Re-throw to return error to client
                }
            }
            
            // Return fresh data from database
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", updatedEstablishment.getId());
            responseData.put("name", updatedEstablishment.getName());
            responseData.put("address", updatedEstablishment.getAddress());
            responseData.put("contactNumber", updatedEstablishment.getContactNumber());
            responseData.put("status", updatedEstablishment.getStatus() != null ? updatedEstablishment.getStatus().toString() : "OPEN");
            responseData.put("upiId", updatedEstablishment.getUpiId());
            responseData.put("operatingHours", updatedEstablishment.getOperatingHours());
            responseData.put("type", updatedEstablishment.getType() != null ? updatedEstablishment.getType().toString() : "HOTEL");
            responseData.put("email", updatedEstablishment.getEmail());
            responseData.put("latitude", updatedEstablishment.getLatitude());
            responseData.put("longitude", updatedEstablishment.getLongitude());
            responseData.put("profileImagePath", updatedEstablishment.getProfileImagePath());
            responseData.put("upiQrCodePath", updatedEstablishment.getUpiQrCodePath());
            responseData.put("weeklySchedule", updatedEstablishment.getWeeklySchedule());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Establishment updated successfully");
            response.put("success", true);
            response.put("data", responseData);
            
            System.out.println("✅ Establishment update successful for: " + email);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Failed to update establishment: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update establishment: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }



    /**
     * Update establishment status (dedicated endpoint)
     */
    @PutMapping("/establishment/status")
    public ResponseEntity<?> updateEstablishmentStatus(@RequestBody Map<String, String> statusData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            String newStatus = statusData.get("status");
            if (newStatus == null || newStatus.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Status is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Find the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Update status using the proper service method
            Establishment updatedEstablishment = establishmentService.updateStatusByOwner(
                establishment.getId(), 
                newStatus.toUpperCase(), 
                owner.getId()
            );
            
            if (updatedEstablishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Failed to update establishment status");
                return ResponseEntity.status(500).body(error);
            }
            
            // Also update cache for immediate reflection
            String cacheKey = "establishment_data_main";
            Map<String, Object> currentData = sharedStateService.getState(cacheKey, Map.class);
            if (currentData != null) {
                currentData.put("status", newStatus.toUpperCase());
                sharedStateService.setState(cacheKey, currentData);
                updatePublicEstablishmentsList(currentData);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Establishment status updated successfully");
            response.put("success", true);
            response.put("establishmentId", establishment.getId());
            response.put("status", updatedEstablishment.getStatus().toString());
            response.put("timestamp", updatedEstablishment.getUpdatedAt());
            
            System.out.println("Status updated successfully for establishment: " + establishment.getName() + 
                             " to: " + updatedEstablishment.getStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Failed to update establishment status: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update establishment status: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Update establishment location
     */
    @PutMapping("/establishment/location")
    public ResponseEntity<?> updateEstablishmentLocation(@RequestBody Map<String, Object> locationData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            System.out.println("📍 Location update request received for user: " + email);
            System.out.println("📍 Location data: " + locationData);
            
            // Get establishment from database
            Establishment establishment = establishmentService.findByOwner(owner);
            
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this user");
                return ResponseEntity.status(404).body(error);
            }
            
            // Parse and validate location data
            Double latitude = null;
            Double longitude = null;
            String address = null;
            
            if (locationData.containsKey("latitude")) {
                Object latObj = locationData.get("latitude");
                if (latObj != null && !latObj.toString().trim().isEmpty()) {
                    try {
                        latitude = Double.valueOf(latObj.toString());
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid latitude format: " + latObj);
                    }
                }
            }
            
            if (locationData.containsKey("longitude")) {
                Object lngObj = locationData.get("longitude");
                if (lngObj != null && !lngObj.toString().trim().isEmpty()) {
                    try {
                        longitude = Double.valueOf(lngObj.toString());
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid longitude format: " + lngObj);
                    }
                }
            }
            
            if (locationData.containsKey("address")) {
                address = (String) locationData.get("address");
            }
            
            // Update establishment location in database
            Establishment updatedEstablishment = establishmentService.updateLocation(
                establishment.getId(), latitude, longitude, address);
            
            if (updatedEstablishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Failed to update location in database");
                return ResponseEntity.status(500).body(error);
            }
            
            // Return fresh data from database
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", updatedEstablishment.getId());
            responseData.put("name", updatedEstablishment.getName());
            responseData.put("address", updatedEstablishment.getAddress());
            responseData.put("contactNumber", updatedEstablishment.getContactNumber());
            responseData.put("status", updatedEstablishment.getStatus() != null ? updatedEstablishment.getStatus().toString() : "OPEN");
            responseData.put("upiId", updatedEstablishment.getUpiId());
            responseData.put("operatingHours", updatedEstablishment.getOperatingHours());
            responseData.put("type", updatedEstablishment.getType() != null ? updatedEstablishment.getType().toString() : "HOTEL");
            responseData.put("email", updatedEstablishment.getEmail());
            responseData.put("latitude", updatedEstablishment.getLatitude());
            responseData.put("longitude", updatedEstablishment.getLongitude());
            responseData.put("profileImagePath", updatedEstablishment.getProfileImagePath());
            responseData.put("upiQrCodePath", updatedEstablishment.getUpiQrCodePath());
            responseData.put("weeklySchedule", updatedEstablishment.getWeeklySchedule());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Location updated successfully");
            response.put("success", true);
            response.put("data", responseData);
            
            System.out.println("✅ Location update successful for: " + email);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Failed to update location: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update location: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Delete establishment location coordinates
     */
    @DeleteMapping("/establishment/location")
    public ResponseEntity<?> deleteEstablishmentLocation() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            System.out.println("🗑️ Location delete request received for user: " + email);
            
            // Get establishment from database
            Establishment establishment = establishmentService.findByOwner(owner);
            
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this user");
                return ResponseEntity.status(404).body(error);
            }
            
            // Delete location coordinates in database
            Establishment updatedEstablishment = establishmentService.deleteLocation(establishment.getId());
            
            if (updatedEstablishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Failed to delete location coordinates");
                return ResponseEntity.status(500).body(error);
            }
            
            // Return fresh data from database
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", updatedEstablishment.getId());
            responseData.put("name", updatedEstablishment.getName());
            responseData.put("address", updatedEstablishment.getAddress());
            responseData.put("contactNumber", updatedEstablishment.getContactNumber());
            responseData.put("status", updatedEstablishment.getStatus() != null ? updatedEstablishment.getStatus().toString() : "OPEN");
            responseData.put("upiId", updatedEstablishment.getUpiId());
            responseData.put("operatingHours", updatedEstablishment.getOperatingHours());
            responseData.put("type", updatedEstablishment.getType() != null ? updatedEstablishment.getType().toString() : "HOTEL");
            responseData.put("email", updatedEstablishment.getEmail());
            responseData.put("latitude", updatedEstablishment.getLatitude());
            responseData.put("longitude", updatedEstablishment.getLongitude());
            responseData.put("profileImagePath", updatedEstablishment.getProfileImagePath());
            responseData.put("upiQrCodePath", updatedEstablishment.getUpiQrCodePath());
            responseData.put("weeklySchedule", updatedEstablishment.getWeeklySchedule());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Location coordinates deleted successfully");
            response.put("success", true);
            response.put("data", responseData);
            
            System.out.println("✅ Location delete successful for: " + email);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Failed to delete location: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to delete location: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update establishment weekly schedule
     */
    @PutMapping("/establishment/schedule")
    public ResponseEntity<?> updateEstablishmentSchedule(@RequestBody Map<String, String> scheduleData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Find owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Update the weekly schedule in database
            String weeklySchedule = scheduleData.get("weeklySchedule");
            if (weeklySchedule != null && !weeklySchedule.trim().isEmpty()) {
                Establishment updatedEstablishment = establishmentService.updateWeeklySchedule(establishment.getId(), weeklySchedule);
                
                if (updatedEstablishment == null) {
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "Failed to update weekly schedule in database");
                    return ResponseEntity.status(500).body(error);
                }
                
                System.out.println("✅ Weekly schedule updated successfully for: " + establishment.getName());
                
                // Trigger real-time update notification
                realTimeUpdateService.notifyEstablishmentUpdate(updatedEstablishment);
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Weekly schedule updated successfully");
            response.put("success", "true");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Failed to update weekly schedule: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update weekly schedule: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get owner's bookings
     */
    @GetMapping("/bookings")
    public ResponseEntity<?> getOwnerBookings() {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) 
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User owner = userPrincipal.getUser();
            
            // Find owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No establishment found for this owner");
                response.put("bookings", new ArrayList<>());
                return ResponseEntity.ok(response);
            }
            
            // Get bookings for this establishment
            List<Booking> bookings = bookingService.getEstablishmentBookings(establishment.getId());
            
            // Convert to DTOs to avoid circular reference issues
            List<BookingResponseDTO> bookingDTOs = bookings.stream()
                .map(BookingResponseDTO::new)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", bookingDTOs);  // Frontend expects 'orders' key
            response.put("bookings", bookingDTOs); // Keep both for compatibility
            response.put("totalOrders", bookingDTOs.size());
            response.put("message", "Bookings fetched successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch bookings: " + e.getMessage());
            response.put("bookings", new ArrayList<>());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Confirm booking and generate QR code
     */
    @PostMapping("/bookings/{bookingId}/confirm")
    public ResponseEntity<?> confirmBooking(@PathVariable Long bookingId) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) 
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User owner = userPrincipal.getUser();
            
            // Find owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No establishment found for this owner");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Confirm the booking
            System.out.println("🏥 Confirming booking ID: " + bookingId + " for establishment: " + establishment.getName());
            Booking booking = bookingService.confirmBooking(bookingId, establishment.getId());
            
            // Generate QR code
            String qrCode = qrCodeService.generateBookingQRCode(booking);
            booking.setQrCode(qrCode);
            bookingService.updateBooking(booking);
            
            // Send confirmation email with QR code
            System.out.println("📧 Sending confirmation email to: " + booking.getUserEmail());
            try {
                emailService.sendBookingConfirmationWithQR(booking);
                System.out.println("✅ Confirmation email sent successfully to: " + booking.getUserEmail());
            } catch (Exception emailError) {
                System.err.println("❌ Failed to send confirmation email: " + emailError.getMessage());
                emailError.printStackTrace();
                // Don't fail the booking confirmation if email fails
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking confirmed and QR code generated");
            response.put("qrCode", qrCode);
            response.put("booking", new BookingResponseDTO(booking));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to confirm booking: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reject booking
     */
    @PostMapping("/bookings/{bookingId}/reject")
    public ResponseEntity<?> rejectBooking(@PathVariable Long bookingId, @RequestBody Map<String, String> requestData) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) 
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User owner = userPrincipal.getUser();
            
            // Find owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No establishment found for this owner");
                return ResponseEntity.badRequest().body(response);
            }
            
            String reason = requestData.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Rejection reason is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Reject the booking
            Booking booking = bookingService.rejectBooking(bookingId, establishment.getId(), reason);
            
            // Send rejection email
            emailService.sendBookingRejection(booking, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking rejected and customer notified");
            response.put("booking", new BookingResponseDTO(booking));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to reject booking: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Delete booking
     */
    @DeleteMapping("/bookings/{bookingId}")
    public ResponseEntity<?> deleteBooking(@PathVariable Long bookingId) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) 
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User owner = userPrincipal.getUser();
            
            // Find owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No establishment found for this owner");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Delete the booking
            bookingService.deleteBooking(bookingId, establishment.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete booking: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }





    /**
     * Get all reviews for owner (pending, approved, rejected)
     */
    @GetMapping("/reviews")
    public ResponseEntity<?> getOwnerReviews() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            System.out.println("=== Owner Reviews Authentication Debug ===");
            System.out.println("Authentication: " + authentication);
            System.out.println("Is authenticated: " + (authentication != null ? authentication.isAuthenticated() : "null"));
            System.out.println("Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));
            System.out.println("Name: " + (authentication != null ? authentication.getName() : "null"));
            
            if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
                System.err.println("Authentication failed for owner reviews - returning 401");
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
            
            User owner = userService.findByEmailSafe(email);

            if (owner == null) {
                System.err.println("User not found for email: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("error", "Unauthorized");
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }

            System.out.println("User found: " + owner.getName() + " with role: " + owner.getRole());
            
            // Check if user has owner role
            if (!"ADMIN".equals(owner.getRole().toString()) && 
                !"OWNER".equals(owner.getRole().toString()) &&
                !"HOTEL_OWNER".equals(owner.getRole().toString()) &&
                !"HOSPITAL_OWNER".equals(owner.getRole().toString()) &&
                !"SHOP_OWNER".equals(owner.getRole().toString())) {
                System.err.println("User role " + owner.getRole() + " not authorized for owner reviews");
                Map<String, String> error = new HashMap<>();
                error.put("error", "Forbidden");
                error.put("message", "Insufficient privileges");
                return ResponseEntity.status(403).body(error);
            }
            
            List<com.opennova.model.Review> allReviews = reviewService.getAllReviewsForOwner(owner.getId());
            System.out.println("Retrieved " + allReviews.size() + " reviews for owner: " + owner.getName());
            
            // Create a safe response that handles null values
            List<Map<String, Object>> safeReviews = allReviews.stream().map(review -> {
                Map<String, Object> reviewMap = new HashMap<>();
                reviewMap.put("id", review.getId());
                reviewMap.put("rating", review.getRating());
                reviewMap.put("comment", review.getComment());
                reviewMap.put("createdAt", review.getCreatedAt());
                reviewMap.put("updatedAt", review.getUpdatedAt());
                reviewMap.put("isActive", review.getIsActive() != null ? review.getIsActive() : true);
                reviewMap.put("status", review.getStatus() != null ? review.getStatus().toString() : "PENDING");
                reviewMap.put("approvedAt", review.getApprovedAt());
                reviewMap.put("rejectedAt", review.getRejectedAt());
                reviewMap.put("rejectionReason", review.getRejectionReason());
                
                // Add user info safely
                if (review.getUser() != null) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", review.getUser().getId());
                    userMap.put("name", review.getUser().getName());
                    userMap.put("email", review.getUser().getEmail());
                    reviewMap.put("user", userMap);
                }
                
                // Add establishment info safely
                if (review.getEstablishment() != null) {
                    Map<String, Object> establishmentMap = new HashMap<>();
                    establishmentMap.put("id", review.getEstablishment().getId());
                    establishmentMap.put("name", review.getEstablishment().getName());
                    reviewMap.put("establishment", establishmentMap);
                }
                
                return reviewMap;
            }).collect(java.util.stream.Collectors.toList());
            
            System.out.println("Returning " + safeReviews.size() + " safely formatted reviews");
            return ResponseEntity.ok(safeReviews);
        } catch (Exception e) {
            System.err.println("Error fetching owner reviews: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching reviews: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Delete review
     */
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable Long reviewId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Delete review using review service
            boolean deleted = reviewService.deleteReview(reviewId, owner.getId(), "OWNER");
            
            if (!deleted) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Review not found or already deleted");
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Review deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Failed to delete review: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to delete review: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get owner's menus
     */
    @GetMapping("/menus")
    public ResponseEntity<?> getOwnerMenus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Get actual menus from database
            List<Menu> dbMenus = menuService.getMenusByEstablishmentId(establishment.getId());
            List<Map<String, Object>> menus = new ArrayList<>();
            
            for (Menu menu : dbMenus) {
                Map<String, Object> menuData = new HashMap<>();
                menuData.put("id", menu.getId());
                menuData.put("name", menu.getName());
                menuData.put("description", menu.getDescription());
                menuData.put("price", menu.getPrice());
                menuData.put("isAvailable", menu.getIsAvailable());
                menuData.put("category", menu.getCategory());
                menuData.put("availabilityTime", menu.getAvailabilityTime());
                menuData.put("preparationTime", menu.getPreparationTime());
                menuData.put("isVegetarian", menu.getIsVegetarian());
                menuData.put("isSpecial", menu.getIsSpecial());
                menuData.put("imagePath", menu.getImagePath());
                menus.add(menuData);
            }
            
            return ResponseEntity.ok(menus);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch menus: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get owner's doctors
     */
    @GetMapping("/doctors")
    public ResponseEntity<?> getOwnerDoctors() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Get actual doctors from database
            List<Doctor> dbDoctors = doctorService.getDoctorsByEstablishmentId(establishment.getId());
            List<Map<String, Object>> doctors = new ArrayList<>();
            
            for (Doctor doctor : dbDoctors) {
                Map<String, Object> doctorData = new HashMap<>();
                doctorData.put("id", doctor.getId());
                doctorData.put("name", doctor.getName());
                doctorData.put("specialization", doctor.getSpecialization());
                doctorData.put("price", doctor.getPrice());
                doctorData.put("availabilityTime", doctor.getAvailabilityTime());
                doctorData.put("imagePath", doctor.getImagePath());
                doctorData.put("isActive", doctor.getIsActive());
                doctors.add(doctorData);
            }
            
            return ResponseEntity.ok(doctors);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch doctors: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get owner's collections
     */
    @GetMapping("/collections")
    public ResponseEntity<?> getOwnerCollections() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Get actual collections from database using CollectionService
            List<com.opennova.model.Collection> dbCollections = collectionService.getCollectionsByEstablishmentId(establishment.getId());
            List<Map<String, Object>> collections = new ArrayList<>();
            
            for (com.opennova.model.Collection collection : dbCollections) {
                Map<String, Object> collectionData = new HashMap<>();
                collectionData.put("id", collection.getId());
                collectionData.put("itemName", collection.getItemName());
                collectionData.put("description", collection.getDescription());
                collectionData.put("price", collection.getPrice());
                collectionData.put("sizes", collection.getSizes());
                collectionData.put("colors", collection.getColors());
                collectionData.put("fabric", collection.getFabric());
                collectionData.put("brand", collection.getBrand());
                collectionData.put("stock", collection.getStock());
                collectionData.put("imagePath", collection.getImagePath());
                collectionData.put("isSpecialOffer", collection.getIsSpecialOffer());
                collectionData.put("isActive", collection.getIsActive());
                collections.add(collectionData);
            }
            
            return ResponseEntity.ok(collections);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch collections: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get owner's special offers
     */
    @GetMapping("/special-offers")
    public ResponseEntity<?> getOwnerSpecialOffers() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Get special offers from collections where isSpecialOffer = true
            List<com.opennova.model.Collection> dbCollections = collectionService.getCollectionsByEstablishmentId(establishment.getId());
            List<Map<String, Object>> specialOffers = new ArrayList<>();
            
            for (com.opennova.model.Collection collection : dbCollections) {
                if (collection.getIsSpecialOffer() != null && collection.getIsSpecialOffer()) {
                    Map<String, Object> offerData = new HashMap<>();
                    offerData.put("id", collection.getId());
                    offerData.put("itemName", collection.getItemName());
                    offerData.put("price", collection.getPrice());
                    offerData.put("sizes", collection.getSizes());
                    offerData.put("colors", collection.getColors());
                    offerData.put("fabric", collection.getFabric());
                    offerData.put("brand", collection.getBrand());
                    offerData.put("stock", collection.getStock());
                    offerData.put("imagePath", collection.getImagePath());
                    offerData.put("isSpecialOffer", collection.getIsSpecialOffer());
                    offerData.put("isActive", collection.getIsActive());
                    specialOffers.add(offerData);
                }
            }
            
            return ResponseEntity.ok(specialOffers);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch special offers: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }



    /**
     * Test authentication endpoint
     */
    @GetMapping("/test-auth")
    public ResponseEntity<?> testAuthentication() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("🔐 testAuth - Authentication object: " + authentication);
            
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("message", "No authentication"));
            }
            
            String email = authentication.getName();
            System.out.println("📧 testAuth - Email: " + email);
            System.out.println("👤 testAuth - Authorities: " + authentication.getAuthorities());
            
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("message", "User not found"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Authentication successful");
            response.put("email", email);
            response.put("role", user.getRole());
            response.put("authorities", authentication.getAuthorities());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    /**
     * Create a new doctor
     */
    @PostMapping("/doctors/with-image")
    public ResponseEntity<?> createDoctor(
            @RequestParam("name") String name,
            @RequestParam("specialization") String specialization,
            @RequestParam("price") BigDecimal price,
            @RequestParam(value = "availabilityTime", required = false) String availabilityTime,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Authentication required");
                return ResponseEntity.status(401).body(error);
            }
            
            String email = authentication.getName();
            if (email == null || email.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Invalid authentication");
                return ResponseEntity.status(401).body(error);
            }
            
            User owner = userService.findByEmail(email);
            if (owner == null || !owner.getIsActive()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found or inactive");
                return ResponseEntity.status(401).body(error);
            }
            
            // Verify user has appropriate role for doctor management
            boolean hasOwnerRole = owner.getRole() == com.opennova.model.UserRole.OWNER ||
                                 owner.getRole() == com.opennova.model.UserRole.HOSPITAL_OWNER ||
                                 owner.getRole() == com.opennova.model.UserRole.ADMIN;
            
            if (!hasOwnerRole) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Insufficient privileges for doctor management");
                return ResponseEntity.status(403).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Create new doctor
            Doctor doctor = new Doctor();
            doctor.setName(name);
            doctor.setSpecialization(specialization);
            doctor.setPrice(price);
            doctor.setAvailabilityTime(availabilityTime);
            doctor.setEstablishment(establishment);
            
            // Handle image upload
            if (image != null && !image.isEmpty()) {
                try {
                    String imagePath = fileStorageService.storeFile(image, "doctors");
                    doctor.setImagePath(imagePath);
                } catch (Exception e) {
                    System.err.println("Failed to upload doctor image: " + e.getMessage());
                }
            }
            
            Doctor savedDoctor = doctorService.createDoctor(establishment.getId(), doctor);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Doctor created successfully");
            response.put("doctor", savedDoctor);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Failed to create doctor: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to create doctor: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Update an existing doctor
     */
    @PutMapping("/doctors/{doctorId}/with-image")
    public ResponseEntity<?> updateDoctor(
            @PathVariable Long doctorId,
            @RequestParam("name") String name,
            @RequestParam("specialization") String specialization,
            @RequestParam("price") BigDecimal price,
            @RequestParam(value = "availabilityTime", required = false) String availabilityTime,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Create doctor data for update
            Doctor doctorData = new Doctor();
            doctorData.setName(name);
            doctorData.setSpecialization(specialization);
            doctorData.setPrice(price);
            doctorData.setAvailabilityTime(availabilityTime);
            
            // Handle image upload
            if (image != null && !image.isEmpty()) {
                try {
                    String imagePath = fileStorageService.storeFile(image, "doctors");
                    doctorData.setImagePath(imagePath);
                } catch (Exception e) {
                    System.err.println("Failed to upload image: " + e.getMessage());
                }
            }
            
            Doctor updatedDoctor = doctorService.updateDoctor(doctorId, doctorData, establishment.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Doctor updated successfully");
            response.put("doctor", updatedDoctor);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update doctor: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Delete a doctor
     */
    @DeleteMapping("/doctors/{doctorId}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long doctorId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            boolean deleted = doctorService.deleteDoctor(doctorId, establishment.getId());
            
            if (deleted) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Doctor deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Doctor not found or access denied");
                return ResponseEntity.status(404).body(error);
            }
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to delete doctor: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }



    /**
     * Get visit statistics
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getVisitStats() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Get bookings for statistics
            List<Booking> bookings = bookingService.getBookingsByEstablishmentId(establishment.getId());
            
            long totalBookings = bookings.size();
            long confirmedBookings = bookings.stream().filter(b -> "CONFIRMED".equals(b.getStatus().toString())).count();
            long completedVisits = bookings.stream().filter(b -> "COMPLETED".equals(b.getStatus().toString())).count();
            long pendingVisits = confirmedBookings - completedVisits;
            
            double totalRevenue = bookings.stream()
                .filter(b -> "CONFIRMED".equals(b.getStatus().toString()))
                .mapToDouble(b -> b.getPaymentAmount() != null ? b.getPaymentAmount().doubleValue() : 0.0)
                .sum();
            
            double visitCompletionRate = totalBookings > 0 ? (completedVisits * 100.0 / totalBookings) : 0.0;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalBookings", totalBookings);
            stats.put("confirmedBookings", confirmedBookings);
            stats.put("completedVisits", completedVisits);
            stats.put("pendingVisits", pendingVisits);
            stats.put("totalRevenue", totalRevenue);
            stats.put("visitCompletionRate", Math.round(visitCompletionRate * 100.0) / 100.0);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch visit stats: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Mark visit as completed
     */
    @PutMapping("/bookings/{bookingId}/mark-completed")
    public ResponseEntity<?> markVisitCompleted(@PathVariable Long bookingId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Update booking as visited
            boolean updated = bookingService.markVisitCompleted(bookingId, establishment.getId());
            
            if (updated) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Visit marked as completed successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Booking not found or access denied");
                return ResponseEntity.status(404).body(error);
            }
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to mark visit as completed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Create a new collection item
     */
    @PostMapping("/collections")
    public ResponseEntity<?> createCollection(
            @RequestParam("itemName") String itemName,
            @RequestParam("price") BigDecimal price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "stock", required = false, defaultValue = "0") Integer stock,
            @RequestParam(value = "sizes", required = false) String sizes,
            @RequestParam(value = "colors", required = false) String colors,
            @RequestParam(value = "fabric", required = false) String fabric,
            @RequestParam(value = "isSpecialOffer", required = false, defaultValue = "false") Boolean isSpecialOffer,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Create new collection
            com.opennova.model.Collection collection = new com.opennova.model.Collection();
            collection.setItemName(itemName);
            collection.setPrice(price);
            collection.setDescription(description);
            collection.setBrand(brand);
            collection.setStock(stock);
            collection.setSizes(sizes);
            collection.setColors(colors);
            collection.setFabric(fabric);
            collection.setIsSpecialOffer(isSpecialOffer);
            
            // Handle image upload
            if (image != null && !image.isEmpty()) {
                try {
                    String imagePath = fileStorageService.storeFile(image, "collections");
                    collection.setImagePath(imagePath);
                } catch (Exception e) {
                    System.err.println("Failed to upload image: " + e.getMessage());
                }
            }
            
            com.opennova.model.Collection savedCollection = collectionService.createCollection(establishment.getId(), collection);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Collection created successfully");
            response.put("collection", savedCollection);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to create collection: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update an existing collection item
     */
    @PutMapping("/collections/{collectionId}")
    public ResponseEntity<?> updateCollection(
            @PathVariable Long collectionId,
            @RequestParam("itemName") String itemName,
            @RequestParam("price") BigDecimal price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "brand", required = false) String brand,
            @RequestParam(value = "stock", required = false, defaultValue = "0") Integer stock,
            @RequestParam(value = "sizes", required = false) String sizes,
            @RequestParam(value = "colors", required = false) String colors,
            @RequestParam(value = "fabric", required = false) String fabric,
            @RequestParam(value = "isSpecialOffer", required = false, defaultValue = "false") Boolean isSpecialOffer,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            // Create collection data for update
            com.opennova.model.Collection collectionData = new com.opennova.model.Collection();
            collectionData.setItemName(itemName);
            collectionData.setPrice(price);
            collectionData.setDescription(description);
            collectionData.setBrand(brand);
            collectionData.setStock(stock);
            collectionData.setSizes(sizes);
            collectionData.setColors(colors);
            collectionData.setFabric(fabric);
            collectionData.setIsSpecialOffer(isSpecialOffer);
            
            // Handle image upload
            if (image != null && !image.isEmpty()) {
                try {
                    String imagePath = fileStorageService.storeFile(image, "collections");
                    collectionData.setImagePath(imagePath);
                } catch (Exception e) {
                    System.err.println("Failed to upload image: " + e.getMessage());
                }
            }
            
            com.opennova.model.Collection updatedCollection = collectionService.updateCollection(collectionId, collectionData, establishment.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Collection updated successfully");
            response.put("collection", updatedCollection);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update collection: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Delete a collection item
     */
    @DeleteMapping("/collections/{collectionId}")
    public ResponseEntity<?> deleteCollection(@PathVariable Long collectionId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Get the owner's establishment
            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this owner");
                return ResponseEntity.status(404).body(error);
            }
            
            boolean deleted = collectionService.deleteCollection(collectionId, establishment.getId());
            
            if (deleted) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Collection deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Collection not found or access denied");
                return ResponseEntity.status(404).body(error);
            }
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to delete collection: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Create or update menu
     */
    @PostMapping("/menus")
    public ResponseEntity<?> createMenu(@RequestBody Map<String, Object> menuData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Mock menu creation - replace with actual database operations
            Map<String, String> response = new HashMap<>();
            response.put("message", "Menu item created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to create menu: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update menu
     */
    @PutMapping("/menus/{menuId}")
    public ResponseEntity<?> updateMenu(@PathVariable Long menuId, @RequestBody Map<String, Object> menuData) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Mock menu update - replace with actual database operations
            Map<String, String> response = new HashMap<>();
            response.put("message", "Menu item updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update menu: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Delete menu
     */
    @DeleteMapping("/menus/{menuId}")
    public ResponseEntity<?> deleteMenu(@PathVariable Long menuId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            // Mock menu deletion - replace with actual database operations
            Map<String, String> response = new HashMap<>();
            response.put("message", "Menu item deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to delete menu: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Export bookings to Excel
     */
    @GetMapping("/export-bookings")
    public ResponseEntity<?> exportBookings() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            System.out.println("📊 Generating Excel export for owner: " + email);
            
            // Generate Excel file using the service
            byte[] excelData = excelExportService.exportBookingsToExcel(owner.getId(), "OWNER");
            
            String fileName = "bookings-export-" + java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) + ".xlsx";
            
            System.out.println("✅ Excel file generated successfully: " + fileName);
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + fileName)
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelData);
        } catch (Exception e) {
            System.err.println("❌ Failed to export bookings: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to export bookings: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Helper method to update public establishments list for real-time sync
     */
    private void updatePublicEstablishmentsList(Map<String, Object> updatedEstablishment) {
        try {
            // Get current public establishments list
            String publicListKey = "public_establishments_list";
            List<Map<String, Object>> publicList = sharedStateService.getState(publicListKey, List.class);
            
            if (publicList == null) {
                publicList = new ArrayList<>();
            }
            
            // Find and update the establishment in the list
            boolean found = false;
            for (int i = 0; i < publicList.size(); i++) {
                Map<String, Object> est = publicList.get(i);
                if (est.get("id").equals(updatedEstablishment.get("id"))) {
                    publicList.set(i, new HashMap<>(updatedEstablishment));
                    found = true;
                    break;
                }
            }
            
            // If not found, add it to the list
            if (!found) {
                publicList.add(new HashMap<>(updatedEstablishment));
            }
            
            // Save updated list
            sharedStateService.setState(publicListKey, publicList);
            
            System.out.println("Updated public establishments list with: " + updatedEstablishment.get("name"));
        } catch (Exception e) {
            System.err.println("Failed to update public establishments list: " + e.getMessage());
        }
    }



    /**
     * Create menu with image upload
     */
    @PostMapping("/menus/with-image")
    public ResponseEntity<?> createMenuWithImage(
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") String price,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "preparationTime", required = false) String preparationTime,
            @RequestParam(value = "isVegetarian", required = false) String isVegetarian,
            @RequestParam(value = "isSpecial", required = false) String isSpecial) {
        
        try {
            System.out.println("🍽️ Creating menu item...");
            System.out.println("📝 Menu data: name=" + name + ", price=" + price + ", category=" + category);
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            User owner = userService.findByEmail(email);
            if (owner == null) {
                System.err.println("❌ Owner not found: " + email);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Owner not found");
                return ResponseEntity.badRequest().body(response);
            }

            Establishment establishment = establishmentService.findByOwner(owner);
            if (establishment == null) {
                System.err.println("❌ Establishment not found for owner: " + email);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Establishment not found");
                return ResponseEntity.badRequest().body(response);
            }

            System.out.println("✅ Found establishment: " + establishment.getName() + " (ID: " + establishment.getId() + ")");

            // Validate required fields
            if (name == null || name.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Menu name is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (price == null || price.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Menu price is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Create menu object
            com.opennova.model.Menu menu = new com.opennova.model.Menu();
            menu.setName(name.trim());
            menu.setDescription(description != null ? description.trim() : "");
            
            try {
                menu.setPrice(new java.math.BigDecimal(price.trim()));
            } catch (NumberFormatException e) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid price format");
                return ResponseEntity.badRequest().body(response);
            }
            
            menu.setCategory(category != null ? category.trim() : "");
            
            if (preparationTime != null && !preparationTime.trim().isEmpty()) {
                try {
                    menu.setPreparationTime(Integer.parseInt(preparationTime.trim()));
                } catch (NumberFormatException e) {
                    menu.setPreparationTime(15); // Default value
                }
            } else {
                menu.setPreparationTime(15); // Default value
            }
            
            menu.setIsVegetarian(isVegetarian != null && Boolean.parseBoolean(isVegetarian));
            menu.setIsSpecial(isSpecial != null && Boolean.parseBoolean(isSpecial));
            menu.setIsAvailable(true); // Default to available
            menu.setIsActive(true); // Default to active

            System.out.println("📝 Menu object created: " + menu.getName() + " - ₹" + menu.getPrice());

            // Create menu with image
            com.opennova.model.Menu createdMenu = menuService.createMenu(establishment.getId(), menu, imageFile);

            System.out.println("✅ Menu created successfully with ID: " + createdMenu.getId());

            // Create response with menu data
            Map<String, Object> menuData = new HashMap<>();
            menuData.put("id", createdMenu.getId());
            menuData.put("name", createdMenu.getName());
            menuData.put("description", createdMenu.getDescription());
            menuData.put("price", createdMenu.getPrice());
            menuData.put("category", createdMenu.getCategory());
            menuData.put("preparationTime", createdMenu.getPreparationTime());
            menuData.put("isVegetarian", createdMenu.getIsVegetarian());
            menuData.put("isSpecial", createdMenu.getIsSpecial());
            menuData.put("isAvailable", createdMenu.getIsAvailable());
            menuData.put("isActive", createdMenu.getIsActive());
            menuData.put("imagePath", createdMenu.getImagePath());
            menuData.put("createdAt", createdMenu.getCreatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Menu item created successfully");
            response.put("menu", menuData);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error creating menu with image: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating menu: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Update menu with image upload
     */
    @PutMapping("/menus/{menuId}/with-image")
    public ResponseEntity<?> updateMenuWithImage(
            @PathVariable Long menuId,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "price", required = false) String price,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "preparationTime", required = false) String preparationTime,
            @RequestParam(value = "isVegetarian", required = false) String isVegetarian,
            @RequestParam(value = "isSpecial", required = false) String isSpecial,
            @RequestParam(value = "isAvailable", required = false) String isAvailable) {
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            User owner = userService.findByEmail(email);
            if (owner == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Owner not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Get existing menu
            com.opennova.model.Menu existingMenu = menuService.getMenuById(menuId);
            if (existingMenu == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Menu item not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Verify ownership
            if (!existingMenu.getEstablishment().getOwner().getId().equals(owner.getId())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Unauthorized to update this menu item");
                return ResponseEntity.badRequest().body(response);
            }

            // Create updated menu object
            com.opennova.model.Menu menuData = new com.opennova.model.Menu();
            if (name != null) menuData.setName(name);
            if (description != null) menuData.setDescription(description);
            if (price != null) menuData.setPrice(new java.math.BigDecimal(price));
            if (category != null) menuData.setCategory(category);
            
            if (preparationTime != null) {
                menuData.setPreparationTime(Integer.parseInt(preparationTime));
            }
            if (isVegetarian != null) {
                menuData.setIsVegetarian(Boolean.parseBoolean(isVegetarian));
            }
            if (isSpecial != null) {
                menuData.setIsSpecial(Boolean.parseBoolean(isSpecial));
            }
            if (isAvailable != null) {
                menuData.setIsAvailable(Boolean.parseBoolean(isAvailable));
            }

            // Update menu with image
            com.opennova.model.Menu updatedMenu = menuService.updateMenu(menuId, menuData, imageFile);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Menu item updated successfully");
            response.put("menu", updatedMenu);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error updating menu with image: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating menu: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Enhanced Excel export with booking status filtering
     */
    @GetMapping("/export-bookings/status/{status}")
    public ResponseEntity<?> exportBookingsByStatus(@PathVariable String status) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            User owner = userService.findByEmail(email);
            if (owner == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Owner not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Get bookings by status
            List<Booking> bookings;
            if ("ALL".equalsIgnoreCase(status)) {
                bookings = bookingService.getOwnerBookings(owner.getId());
            } else {
                bookings = bookingService.getOwnerBookings(owner.getId()).stream()
                    .filter(booking -> booking.getStatus().toString().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
            }

            // Generate Excel file using injected service
            byte[] excelData = excelExportService.exportBookingsToExcel(owner.getId(), status.toUpperCase());

            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=bookings_" + status.toLowerCase() + ".xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excelData);

        } catch (Exception e) {
            System.err.println("Error exporting bookings by status: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error exporting bookings: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }



    // ==================== REVIEW MANAGEMENT ====================

    /**
     * Get pending reviews for owner approval
     */
    @GetMapping("/reviews/pending")
    public ResponseEntity<?> getPendingReviews() {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) 
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User owner = userPrincipal.getUser();
            
            List<com.opennova.model.Review> pendingReviews = reviewService.getPendingReviewsForOwner(owner.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reviews", pendingReviews);
            response.put("message", "Pending reviews fetched successfully");
            
            return ResponseEntity.ok(pendingReviews);
        } catch (Exception e) {
            System.err.println("Error fetching pending reviews: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching pending reviews: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }



    /**
     * Approve a review
     */
    @PostMapping("/reviews/{reviewId}/approve")
    public ResponseEntity<?> approveReview(@PathVariable Long reviewId) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) 
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User owner = userPrincipal.getUser();
            
            com.opennova.model.Review approvedReview = reviewService.approveReview(reviewId, owner.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review approved successfully");
            response.put("review", approvedReview);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error approving review: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error approving review: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reject a review
     */
    @PostMapping("/reviews/{reviewId}/reject")
    public ResponseEntity<?> rejectReview(@PathVariable Long reviewId, @RequestBody Map<String, String> requestData) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) 
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User owner = userPrincipal.getUser();
            
            String reason = requestData.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Rejection reason is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            com.opennova.model.Review rejectedReview = reviewService.rejectReview(reviewId, owner.getId(), reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review rejected successfully");
            response.put("review", rejectedReview);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error rejecting review: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error rejecting review: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== PROFILE MANAGEMENT ====================

    /**
     * Update establishment profile with image upload
     */
    @PutMapping("/establishment/profile")
    public ResponseEntity<?> updateEstablishmentProfile(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestParam(value = "operatingHours", required = false) String operatingHours,
            @RequestParam(value = "weeklySchedule", required = false) String weeklySchedule,
            @RequestParam(value = "openTime", required = false) String openTime,
            @RequestParam(value = "closeTime", required = false) String closeTime,
            @RequestParam(value = "upiId", required = false) String upiId,
            @RequestParam(value = "latitude", required = false) String latitude,
            @RequestParam(value = "longitude", required = false) String longitude,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "upiQrCode", required = false) MultipartFile upiQrCode) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User owner = userService.findByEmail(email);
            
            if (owner == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "User not found");
                return ResponseEntity.status(401).body(error);
            }
            
            System.out.println("Profile update request received for user: " + email);
            
            // Get establishment from database - no cache dependency
            Establishment establishment = establishmentService.findByOwner(owner);
            
            if (establishment == null) {
                System.err.println("❌ No establishment found for user: " + email);
                Map<String, String> error = new HashMap<>();
                error.put("message", "No establishment found for this user");
                return ResponseEntity.status(404).body(error);
            }
            
            System.out.println("🔍 Found establishment: " + establishment.getName() + " (Type: " + establishment.getType() + ")");
            
            // Create update object with only the fields that are being updated
            Establishment updateData = new Establishment();
            boolean hasUpdates = false;
            
            if (name != null && !name.trim().isEmpty()) {
                updateData.setName(name.trim());
                hasUpdates = true;
            }
            if (address != null && !address.trim().isEmpty()) {
                updateData.setAddress(address.trim());
                hasUpdates = true;
            }
            if (contactNumber != null && !contactNumber.trim().isEmpty()) {
                updateData.setContactNumber(contactNumber.trim());
                hasUpdates = true;
            }
            if (upiId != null && !upiId.trim().isEmpty()) {
                updateData.setUpiId(upiId.trim());
                hasUpdates = true;
            }
            if (operatingHours != null && !operatingHours.trim().isEmpty()) {
                updateData.setOperatingHours(operatingHours.trim());
                hasUpdates = true;
            }
            if (weeklySchedule != null && !weeklySchedule.trim().isEmpty()) {
                updateData.setWeeklySchedule(weeklySchedule.trim());
                hasUpdates = true;
            }
            if (latitude != null && !latitude.trim().isEmpty()) {
                try {
                    Double lat = Double.valueOf(latitude.trim());
                    updateData.setLatitude(lat);
                    hasUpdates = true;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid latitude format: " + latitude);
                }
            }
            if (longitude != null && !longitude.trim().isEmpty()) {
                try {
                    Double lng = Double.valueOf(longitude.trim());
                    updateData.setLongitude(lng);
                    hasUpdates = true;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid longitude format: " + longitude);
                }
            }
            
            // Save all changes to database using proper update method
            Establishment updatedEstablishment = establishment;
            if (hasUpdates || (profileImage != null && !profileImage.isEmpty()) || (upiQrCode != null && !upiQrCode.isEmpty())) {
                try {
                    System.out.println("📝 Saving establishment profile to database...");
                    
                    // Use the updateEstablishmentWithImages method which handles partial updates properly
                    updatedEstablishment = establishmentService.updateEstablishmentWithImages(
                        establishment.getId(), updateData, profileImage, upiQrCode);
                    
                    System.out.println("✅ Successfully saved establishment profile in database for: " + updatedEstablishment.getName());
                    
                    // Trigger real-time update notification for operating hours changes
                    if (operatingHours != null || weeklySchedule != null) {
                        realTimeUpdateService.notifyEstablishmentUpdate(updatedEstablishment);
                        System.out.println("✅ Triggered real-time update notification for operating hours change");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Failed to save establishment profile to database: " + e.getMessage());
                    e.printStackTrace();
                    throw e; // Re-throw to return error to client
                }
            }
            
            // Return fresh data from database
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", updatedEstablishment.getId());
            responseData.put("name", updatedEstablishment.getName());
            responseData.put("address", updatedEstablishment.getAddress());
            responseData.put("contactNumber", updatedEstablishment.getContactNumber());
            responseData.put("status", updatedEstablishment.getStatus() != null ? updatedEstablishment.getStatus().toString() : "OPEN");
            responseData.put("upiId", updatedEstablishment.getUpiId());
            responseData.put("operatingHours", updatedEstablishment.getOperatingHours());
            responseData.put("type", updatedEstablishment.getType() != null ? updatedEstablishment.getType().toString() : "HOTEL");
            responseData.put("email", updatedEstablishment.getEmail());
            responseData.put("latitude", updatedEstablishment.getLatitude());
            responseData.put("longitude", updatedEstablishment.getLongitude());
            responseData.put("profileImagePath", updatedEstablishment.getProfileImagePath());
            responseData.put("upiQrCodePath", updatedEstablishment.getUpiQrCodePath());
            responseData.put("weeklySchedule", updatedEstablishment.getWeeklySchedule());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Establishment profile updated successfully");
            response.put("success", true);
            response.put("data", responseData);
            
            System.out.println("✅ Establishment profile update successful for: " + email);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Failed to update establishment profile: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to update establishment profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }


}