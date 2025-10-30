package com.opennova.controller;

import com.opennova.model.Booking;
import com.opennova.model.User;
import com.opennova.service.BookingService;
import com.opennova.service.BookingValidationService;
import com.opennova.service.EmailService;
import com.opennova.service.QRCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3002", "http://127.0.0.1:3002", "http://localhost:3003", "http://127.0.0.1:3003"})
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private QRCodeService qrCodeService;

    // Validation endpoint
    @PostMapping("/bookings/validate")
    public ResponseEntity<?> validateBooking(@RequestBody Map<String, Object> validationData) {
        try {
            Long establishmentId = Long.valueOf(validationData.get("establishmentId").toString());
            String selectedItems = (String) validationData.get("selectedItems");
            String visitingTime = (String) validationData.get("visitingTime");

            BookingValidationService.ValidationResult result = 
                bookingService.validateBooking(establishmentId, selectedItems, visitingTime);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", result.isValid());
            response.put("errors", result.getErrors());
            response.put("metadata", result.getMetadata());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Validation failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // User endpoints
    @PostMapping("/user/bookings")
    public ResponseEntity<?> createBooking(
            @RequestParam("establishmentId") Long establishmentId,
            @RequestParam("visitingDate") String visitingDate,
            @RequestParam("visitingTime") String visitingTime,
            @RequestParam("selectedItems") String selectedItems,
            @RequestParam("totalAmount") Double totalAmount,
            @RequestParam("paymentAmount") Double paymentAmount,
            @RequestParam("transactionId") String transactionId,
            @RequestParam(value = "transactionRef", required = false) String transactionRef,
            @RequestParam(value = "paymentVerified", required = false) String paymentVerified,
            @RequestParam(value = "paymentScreenshot", required = false) MultipartFile paymentScreenshot,
            Authentication authentication) {
        
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();
            
            // CRITICAL SECURITY CHECK: Verify payment was properly verified
            if (!"true".equals(paymentVerified) || transactionRef == null || transactionRef.trim().isEmpty()) {
                System.err.println("❌ BOOKING REJECTED: Payment not verified for user " + user.getEmail());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "❌ PAYMENT VERIFICATION REQUIRED: You must complete payment verification before creating a booking. Please go back and verify your payment first.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            System.out.println("✅ PAYMENT VERIFIED - Proceeding with booking creation for user: " + user.getEmail());
            System.out.println("📝 Transaction Ref: " + transactionRef + ", Transaction ID: " + transactionId);
            
            Booking booking = bookingService.createBooking(
                user.getId(),
                establishmentId,
                visitingDate,
                visitingTime,
                selectedItems,
                totalAmount,
                paymentAmount,
                transactionId,
                paymentScreenshot
            );

            // Send confirmation email
            emailService.sendBookingConfirmation(booking);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("bookingId", booking.getId());
            response.put("message", "Booking created successfully with verified payment");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the full stack trace for debugging
            e.printStackTrace();
            System.err.println("Booking creation failed: " + e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create booking: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/user/bookings")
    public ResponseEntity<?> getUserBookings(Authentication authentication) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();
            List<Booking> bookings = bookingService.getUserBookings(user.getId());
            
            // Convert to DTOs to avoid circular reference issues
            List<com.opennova.dto.BookingResponseDTO> bookingDTOs = bookings.stream()
                .map(com.opennova.dto.BookingResponseDTO::new)
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(bookingDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch user bookings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/user/bookings/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId, Authentication authentication) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();
            Booking booking = bookingService.cancelBooking(bookingId, user.getId());
            
            // Send cancellation email
            emailService.sendBookingCancellation(booking);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Booking cancelled successfully");
            response.put("refundStatus", booking.getRefundStatus());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to cancel booking: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }



    // Admin endpoints
    @GetMapping("/admin/bookings")
    public ResponseEntity<?> getAllBookings() {
        try {
            List<Booking> bookings = bookingService.getAllBookings();
            
            // Convert to DTOs to avoid circular reference issues
            List<com.opennova.dto.BookingResponseDTO> bookingDTOs = bookings.stream()
                .map(com.opennova.dto.BookingResponseDTO::new)
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(bookingDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to fetch all bookings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/admin/bookings/stats")
    public ResponseEntity<Map<String, Object>> getBookingStats() {
        Map<String, Object> stats = bookingService.getBookingStatistics();
        return ResponseEntity.ok(stats);
    }

    // Get order list with visit requirements for establishments
    @GetMapping("/owner/orders")
    public ResponseEntity<?> getOwnerOrders(Authentication authentication) {
        try {
            String email = authentication.getName();
            List<Map<String, Object>> orders = bookingService.getOwnerOrdersWithVisitRequirements(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("orders", orders);
            response.put("totalOrders", orders.size());
            response.put("paidOrders", orders.stream().mapToLong(o -> 
                "CONFIRMED".equals(o.get("status")) ? 1 : 0).sum());
            response.put("pendingVisits", orders.stream().mapToLong(o -> 
                "CONFIRMED".equals(o.get("status")) && !(Boolean)o.get("visited") ? 1 : 0).sum());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch orders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Mark visit as completed
    @PutMapping("/owner/orders/{bookingId}/visit-completed")
    public ResponseEntity<?> markVisitCompleted(@PathVariable Long bookingId, Authentication authentication) {
        try {
            String email = authentication.getName();
            bookingService.markVisitCompleted(bookingId, email);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Visit marked as completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to mark visit as completed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // Get establishment visit statistics
    @GetMapping("/owner/visit-stats")
    public ResponseEntity<?> getVisitStatistics(Authentication authentication) {
        try {
            String email = authentication.getName();
            Map<String, Object> stats = bookingService.getEstablishmentVisitStats(email);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to fetch visit statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // QR Code scanning and validation endpoints
    @PostMapping("/owner/scan-qr")
    public ResponseEntity<?> scanQRCode(@RequestBody Map<String, String> qrData, Authentication authentication) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User owner = userPrincipal.getUser();
            
            String qrCodeData = qrData.get("qrData");
            if (qrCodeData == null || qrCodeData.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "QR code data is required");
                return ResponseEntity.badRequest().body(error);
            }

            // Parse and validate QR code
            Map<String, Object> qrInfo = qrCodeService.parseQRCode(qrCodeData);
            
            // Validate QR code structure
            if (!qrInfo.containsKey("id") || !qrInfo.containsKey("type") || !"BOOKING".equals(qrInfo.get("type"))) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Invalid QR code format");
                return ResponseEntity.badRequest().body(error);
            }

            Long bookingId = Long.valueOf(qrInfo.get("id").toString());
            Booking booking = bookingService.getBookingById(bookingId);
            
            if (booking == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Booking not found");
                return ResponseEntity.badRequest().body(error);
            }

            // Verify owner has access to this booking
            if (!booking.getEstablishment().getOwner().getId().equals(owner.getId())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "QR code not valid for your establishment");
                return ResponseEntity.badRequest().body(error);
            }

            // Verify booking status
            if (booking.getStatus() != com.opennova.model.BookingStatus.CONFIRMED) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Booking is not confirmed. Status: " + booking.getStatus());
                return ResponseEntity.badRequest().body(error);
            }

            // Return booking details for verification
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("booking", new com.opennova.dto.BookingResponseDTO(booking));
            response.put("message", "QR code is valid");
            response.put("customerName", booking.getUser().getName());
            response.put("visitingDate", booking.getVisitingDate());
            response.put("visitingTime", booking.getVisitingTime());
            response.put("remainingAmount", booking.getAmount().doubleValue() - booking.getPaymentAmount().doubleValue());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("QR scan error: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "QR code validation failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/owner/confirm-visit")
    public ResponseEntity<?> confirmVisit(@RequestBody Map<String, Object> visitData, Authentication authentication) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User owner = userPrincipal.getUser();
            
            Long bookingId = Long.valueOf(visitData.get("bookingId").toString());
            Double remainingPayment = visitData.get("remainingPayment") != null ? 
                Double.valueOf(visitData.get("remainingPayment").toString()) : 0.0;

            Booking booking = bookingService.getBookingById(bookingId);
            
            if (booking == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Booking not found");
                return ResponseEntity.badRequest().body(error);
            }

            // Verify owner has access to this booking
            if (!booking.getEstablishment().getOwner().getId().equals(owner.getId())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Unauthorized to confirm this visit");
                return ResponseEntity.badRequest().body(error);
            }

            // Mark visit as completed
            bookingService.markVisitCompleted(bookingId, owner.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Visit confirmed successfully");
            response.put("bookingId", bookingId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Visit confirmation error: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to confirm visit: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Resend QR code email
    @PostMapping("/user/bookings/{bookingId}/resend-qr")
    public ResponseEntity<?> resendQRCode(@PathVariable Long bookingId, Authentication authentication) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();
            
            Booking booking = bookingService.getBookingById(bookingId);
            
            if (booking == null) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Booking not found");
                return ResponseEntity.badRequest().body(error);
            }

            // Verify user owns this booking
            if (!booking.getUser().getId().equals(user.getId())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Unauthorized to access this booking");
                return ResponseEntity.badRequest().body(error);
            }

            // Check if booking is confirmed and has QR code
            if (booking.getStatus() != com.opennova.model.BookingStatus.CONFIRMED) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "QR code is only available for confirmed bookings");
                return ResponseEntity.badRequest().body(error);
            }

            if (booking.getQrCode() == null || booking.getQrCode().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "QR code not available for this booking");
                return ResponseEntity.badRequest().body(error);
            }

            // Resend QR code email
            emailService.sendBookingQRCode(booking.getUserEmail(), booking, booking.getQrCode());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "QR code email sent successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Resend QR error: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to resend QR code: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }}
