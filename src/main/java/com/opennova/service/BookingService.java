package com.opennova.service;

import com.opennova.model.Booking;
import com.opennova.model.BookingStatus;
import com.opennova.model.PaymentStatus;
import com.opennova.model.RefundStatus;
import com.opennova.model.Establishment;
import com.opennova.model.User;
import com.opennova.repository.BookingRepository;
import com.opennova.repository.EstablishmentRepository;
import com.opennova.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EstablishmentRepository establishmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BookingValidationService validationService;
    
    @Autowired
    private RealTimeUpdateService realTimeUpdateService;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private EmailService emailService;

    public Booking createBooking(Long userId, Long establishmentId, String visitingDate, 
                               String visitingTime, String selectedItems, Double totalAmount, 
                               Double paymentAmount, String transactionId, MultipartFile paymentScreenshot) {
        
        try {
            System.out.println("Creating booking for userId: " + userId + ", establishmentId: " + establishmentId);
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            
            System.out.println("Found user: " + user.getEmail());
            
            Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new RuntimeException("Establishment not found with ID: " + establishmentId));
            
            System.out.println("Found establishment: " + establishment.getName() + ", Owner ID: " + 
                (establishment.getOwner() != null ? establishment.getOwner().getId() : "NULL"));
            


            // STRICT AMOUNT VALIDATION - Must pay exact amount
            Double expectedPaymentAmount = totalAmount * 0.7; // 70% advance payment
            if (paymentAmount == null || Math.abs(paymentAmount - expectedPaymentAmount) > 1.0) {
                throw new RuntimeException(String.format(
                    "Invalid payment amount. Expected: ₹%.2f, Received: ₹%.2f. You must pay the exact amount to proceed.", 
                    expectedPaymentAmount, paymentAmount != null ? paymentAmount : 0.0));
            }
            
            System.out.println("✅ Payment amount validated: Expected ₹" + expectedPaymentAmount + 
                             ", Received ₹" + paymentAmount);

            Booking booking = new Booking();
            
            // Set all required fields
            booking.setUser(user);
            
            // Ensure user email is not null
            String userEmail = user.getEmail();
            if (userEmail == null || userEmail.trim().isEmpty()) {
                throw new RuntimeException("User email is required but not found for user ID: " + userId);
            }
            booking.setUserEmail(userEmail);
            booking.setEstablishment(establishment);
            booking.setVisitingDate(visitingDate);
            booking.setVisitingTime(visitingTime);
            booking.setSelectedItems(selectedItems);
            
            // Convert Double to BigDecimal for proper database storage
            booking.setAmount(BigDecimal.valueOf(totalAmount != null ? totalAmount : 0.0));
            
            // Ensure paymentAmount is never null - use paymentAmount if provided, otherwise calculate 70% of total
            BigDecimal paidAmount;
            if (paymentAmount != null && paymentAmount > 0) {
                paidAmount = BigDecimal.valueOf(paymentAmount);
            } else if (totalAmount != null && totalAmount > 0) {
                paidAmount = BigDecimal.valueOf(totalAmount * 0.7); // 70% of total
            } else {
                paidAmount = BigDecimal.ZERO;
            }
            booking.setPaymentAmount(paidAmount);
            
            booking.setTransactionId(transactionId);
            booking.setStatus(BookingStatus.PENDING);
            booking.setPaymentStatus(PaymentStatus.PAID);
            booking.setRefundStatus(RefundStatus.NOT_APPLICABLE);
            
            // Set timestamps
            LocalDateTime now = LocalDateTime.now();
            booking.setCreatedAt(now);
            booking.setUpdatedAt(now);
            
            // Set default visiting hours
            booking.setVisitingHours(2);

            // Payment screenshots removed for security - using UPI transaction verification instead
            if (paymentScreenshot != null && !paymentScreenshot.isEmpty()) {
                System.out.println("Payment screenshot provided but not stored for security reasons. Using UPI verification instead.");
            }

            System.out.println("Creating booking with data: " + booking.toString());

            
            Booking savedBooking = bookingRepository.save(booking);
            
            System.out.println("Booking created successfully with ID: " + savedBooking.getId());
            
            // Notify real-time updates
            realTimeUpdateService.notifyBookingUpdate(savedBooking);
            
            // Send booking confirmation email to customer IMMEDIATELY
            try {
                emailService.sendBookingConfirmation(savedBooking);
                System.out.println("✅ Sent booking confirmation to customer: " + savedBooking.getUserEmail());
            } catch (Exception e) {
                System.err.println("❌ Failed to send booking confirmation to customer: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Send notification to establishment owner
            if (establishment.getOwner() != null && establishment.getOwner().getEmail() != null) {
                try {
                    emailService.sendNewBookingNotificationToOwner(savedBooking);
                    System.out.println("✅ Sent booking notification to owner: " + establishment.getOwner().getEmail());
                } catch (Exception e) {
                    System.err.println("❌ Failed to send booking notification to owner: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("⚠️ Cannot send owner notification - owner or owner email is null");
            }
            
            return savedBooking;
            
        } catch (Exception e) {
            System.err.println("Error creating booking: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create booking: " + e.getMessage(), e);
        }
    }

    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Booking> getEstablishmentBookings(Long establishmentId) {
        // Get bookings for a specific establishment
        return bookingRepository.findByEstablishmentIdOrderByCreatedAtDesc(establishmentId);
    }

    public List<Booking> getBookingsByEstablishmentId(Long establishmentId) {
        // Alias method for consistency with controller usage
        return getEstablishmentBookings(establishmentId);
    }

    public List<Booking> getOwnerBookings(Long ownerId) {
        // Use the correct repository method that directly queries by owner ID
        return bookingRepository.findByEstablishmentOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc();
    }

    public Booking cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to cancel this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Cannot cancel booking with status: " + booking.getStatus());
        }

        // Check if cancellation is within 2 hours of booking time
        LocalDateTime bookingDateTime;
        try {
            bookingDateTime = LocalDateTime.parse(booking.getVisitingDate() + "T" + booking.getVisitingTime());
        } catch (Exception e) {
            // Fallback parsing if format is different
            bookingDateTime = LocalDateTime.now().plusHours(3); // Assume future booking
        }
        
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilBooking = java.time.Duration.between(now, bookingDateTime).toHours();

        // Set cancellation details
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason("Cancelled by customer");

        // Apply refund policy: Cancel before 2 hours → Refund, Cancel within 2 hours → No refund
        if (hoursUntilBooking >= 2) {
            booking.setRefundStatus(RefundStatus.APPROVED);
        } else {
            booking.setRefundStatus(RefundStatus.NOT_ELIGIBLE);
        }

        Booking savedBooking = bookingRepository.save(booking);

        // Send email notifications
        try {
            // 1. Send cancellation email to user
            emailService.sendBookingCancellation(savedBooking);
            
            // 2. Send notification to owner about customer cancellation
            emailService.sendOwnerNotificationForCustomerCancellation(
                savedBooking.getEstablishment().getEmail(),
                savedBooking,
                hoursUntilBooking
            );
        } catch (Exception e) {
            System.err.println("Failed to send cancellation emails: " + e.getMessage());
            // Don't fail the cancellation if email fails
        }

        // Notify real-time updates
        try {
            realTimeUpdateService.notifyBookingUpdate(savedBooking);
        } catch (Exception e) {
            System.err.println("Failed to send real-time update: " + e.getMessage());
        }

        return savedBooking;
    }

    public Booking confirmBooking(Long bookingId, Long establishmentId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getEstablishment().getId().equals(establishmentId)) {
            throw new RuntimeException("Unauthorized to confirm this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Cannot confirm booking with status: " + booking.getStatus());
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        
        // Generate QR code for the booking
        try {
            String qrCodeData = qrCodeService.generateBookingQRCode(booking);
            booking.setQrCode(qrCodeData);
        } catch (Exception e) {
            System.err.println("Failed to generate QR code: " + e.getMessage());
            // Continue without QR code
        }

        Booking savedBooking = bookingRepository.save(booking);
        
        // Send detailed confirmation email with all booking details and QR code
        try {
            if (savedBooking.getQrCode() != null && !savedBooking.getQrCode().trim().isEmpty()) {
                System.out.println("📧 Sending booking confirmation with QR code to: " + savedBooking.getUserEmail());
                emailService.sendBookingConfirmationWithQR(savedBooking);
                System.out.println("✅ Booking confirmation with QR sent successfully");
            } else {
                System.err.println("⚠️ QR code is null or empty, sending basic confirmation");
                emailService.sendBookingConfirmation(savedBooking);
                System.out.println("✅ Basic booking confirmation sent successfully");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send confirmation email: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the booking confirmation if email fails
        }
        
        // Notify real-time updates
        realTimeUpdateService.notifyBookingUpdate(savedBooking);
        
        return savedBooking;
    }
    
    private String generateQRCodeData(Booking booking) {
        // Generate QR code data containing booking information
        return String.format("BOOKING_%d_%s_%s", 
            booking.getId(), 
            booking.getTransactionId(),
            booking.getEstablishment().getId());
    }
    


    public Booking ownerCancelBooking(Long bookingId, Long ownerId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getEstablishment().getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel booking with status: " + booking.getStatus());
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(reason);
        
        // Owner cancellation always results in full refund
        booking.setRefundStatus(RefundStatus.APPROVED);

        Booking savedBooking = bookingRepository.save(booking);
        
        // Notifications can be added here if needed
        
        return savedBooking;
    }

    public void deleteBooking(Long bookingId, Long establishmentId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getEstablishment().getId().equals(establishmentId)) {
            throw new RuntimeException("Unauthorized to delete this booking");
        }

        // Send notifications before deletion (if service is available)
        if (notificationService != null) {
            try {
                notificationService.notifyBookingDeleted(booking, "Owner");
            } catch (Exception e) {
                System.err.println("Notification service error: " + e.getMessage());
            }
        }
        
        bookingRepository.delete(booking);
    }

    public Booking updateBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId).orElse(null);
    }

    public BookingValidationService.ValidationResult validateBooking(Long establishmentId, String selectedItems, String visitingTime) {
        return validationService.validateBookingAgainstMenu(establishmentId, selectedItems, visitingTime);
    }

    public void adminDeleteBooking(Long bookingId, Long adminId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Send notifications before deletion (if service is available)
        if (notificationService != null) {
            try {
                notificationService.notifyBookingDeleted(booking, "Admin");
            } catch (Exception e) {
                System.err.println("Notification service error: " + e.getMessage());
            }
        }
        
        bookingRepository.delete(booking);
    }

    public Booking rejectBooking(Long bookingId, Long establishmentId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getEstablishment().getId().equals(establishmentId)) {
            throw new RuntimeException("Unauthorized to reject this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Cannot reject booking with status: " + booking.getStatus());
        }

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(reason);
        
        // Rejected bookings get full refund
        booking.setRefundStatus(RefundStatus.APPROVED);

        Booking savedBooking = bookingRepository.save(booking);
        
        // Send detailed rejection email with reason and refund information
        try {
            emailService.sendBookingRejectionWithDetails(savedBooking, reason);
        } catch (Exception e) {
            System.err.println("Failed to send rejection email: " + e.getMessage());
        }
        
        // Send notifications
        notificationService.notifyBookingStatusChange(savedBooking, oldStatus, BookingStatus.CANCELLED);
        
        return savedBooking;
    }
    


    public Booking validateQRCode(String qrData, Long ownerId) {
        try {
            // Parse QR data using QRCodeService
            Map<String, Object> qrInfo = qrCodeService.parseQRCode(qrData);
            Long bookingId = Long.valueOf(qrInfo.get("bookingId").toString());
            Long establishmentId = Long.valueOf(qrInfo.get("establishmentId").toString());
            
            Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!booking.getEstablishment().getOwner().getId().equals(ownerId)) {
                throw new RuntimeException("QR code not valid for this establishment");
            }

            if (!booking.getEstablishment().getId().equals(establishmentId)) {
                throw new RuntimeException("QR code establishment mismatch");
            }

            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                throw new RuntimeException("Booking is not confirmed. Current status: " + booking.getStatus());
            }

            return booking;
        } catch (Exception e) {
            throw new RuntimeException("Invalid QR code: " + e.getMessage());
        }
    }



    public Booking findBookingByIdAndOwner(Long bookingId, Long ownerId) {
        Optional<Booking> bookingOpt = bookingRepository.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            return null;
        }
        
        Booking booking = bookingOpt.get();
        if (booking.getEstablishment() == null || 
            booking.getEstablishment().getOwner() == null ||
            !booking.getEstablishment().getOwner().getId().equals(ownerId)) {
            return null;
        }
        
        return booking;
    }

    public Map<String, Object> getBookingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalBookings = bookingRepository.count();
        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING);
        long confirmedBookings = bookingRepository.countByStatus(BookingStatus.CONFIRMED);
        long cancelledBookings = bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long completedBookings = bookingRepository.countByStatus(BookingStatus.COMPLETED);
        
        BigDecimal totalRevenueBD = bookingRepository.sumPaidAmountByStatus(BookingStatus.CONFIRMED);
        Double totalRevenue = totalRevenueBD != null ? totalRevenueBD.doubleValue() : 0.0;

        stats.put("totalBookings", totalBookings);
        stats.put("pendingBookings", pendingBookings);
        stats.put("confirmedBookings", confirmedBookings);
        stats.put("cancelledBookings", cancelledBookings);
        stats.put("completedBookings", completedBookings);
        stats.put("totalRevenue", totalRevenue);

        return stats;
    }

    public List<Map<String, Object>> getOwnerOrdersWithVisitRequirements(String ownerEmail) {
        try {
            Optional<User> ownerOpt = userRepository.findByEmail(ownerEmail);
            if (ownerOpt.isEmpty()) {
                throw new RuntimeException("Owner not found");
            }
            User owner = ownerOpt.get();

            List<Booking> bookings = bookingRepository.findByEstablishmentOwnerIdOrderByCreatedAtDesc(owner.getId());
            List<Map<String, Object>> orders = new ArrayList<>();

            for (Booking booking : bookings) {
                Map<String, Object> order = new HashMap<>();
                order.put("id", booking.getId());
                order.put("customerName", booking.getUser() != null ? booking.getUser().getName() : "Unknown");
                order.put("customerEmail", booking.getUserEmail());
                order.put("establishmentName", booking.getEstablishment().getName());
                order.put("visitingDate", booking.getVisitingDate());
                order.put("visitingTime", booking.getVisitingTime());
                order.put("totalAmount", booking.getAmount() != null ? booking.getAmount().doubleValue() : 0.0);
                order.put("paidAmount", booking.getPaymentAmount() != null ? booking.getPaymentAmount().doubleValue() : 0.0);
                order.put("status", booking.getStatus().toString());
                order.put("paymentStatus", booking.getPaymentStatus() != null ? booking.getPaymentStatus().toString() : "PENDING");
                order.put("transactionId", booking.getTransactionId());
                order.put("createdAt", booking.getCreatedAt());
                order.put("visited", booking.getStatus() == BookingStatus.COMPLETED);
                order.put("qrCode", booking.getQrCode());
                
                // Calculate visit requirements
                boolean isPaid = booking.getPaymentStatus() == PaymentStatus.PAID || 
                               booking.getStatus() == BookingStatus.CONFIRMED;
                boolean visitRequired = isPaid && booking.getStatus() != BookingStatus.COMPLETED;
                
                order.put("isPaid", isPaid);
                order.put("visitRequired", visitRequired);
                
                orders.add(order);
            }

            return orders;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch owner orders: " + e.getMessage());
        }
    }

    public boolean markVisitCompleted(Long bookingId, Long establishmentId) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!booking.getEstablishment().getId().equals(establishmentId)) {
                return false;
            }

            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                return false;
            }

            // Mark as completed - no setVisited method needed
            booking.setStatus(BookingStatus.COMPLETED);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            // Send notification to customer
            try {
                if (booking.getUser() != null) {
                    notificationService.sendUserNotification(
                        booking.getUser().getId(), 
                        "Visit Completed", 
                        "Your visit to " + booking.getEstablishment().getName() + " has been marked as completed.", 
                        NotificationService.NotificationType.BOOKING_CONFIRMATION
                    );
                }
            } catch (Exception e) {
                System.err.println("Failed to send visit completion notification: " + e.getMessage());
            }

            return true;
        } catch (Exception e) {
            System.err.println("Failed to mark visit as completed: " + e.getMessage());
            return false;
        }
    }

    public void markVisitCompleted(Long bookingId, String ownerEmail) {
        try {
            Optional<User> ownerOpt = userRepository.findByEmail(ownerEmail);
            if (ownerOpt.isEmpty()) {
                throw new RuntimeException("Owner not found");
            }
            User owner = ownerOpt.get();

            Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

            if (!booking.getEstablishment().getOwner().getId().equals(owner.getId())) {
                throw new RuntimeException("Unauthorized to mark this booking as visited");
            }

            if (booking.getStatus() != BookingStatus.CONFIRMED) {
                throw new RuntimeException("Only confirmed bookings can be marked as visited");
            }

            booking.setStatus(BookingStatus.COMPLETED);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            // Send notification to customer
            try {
                notificationService.sendUserNotification(
                    booking.getUser().getId(), 
                    "Visit Completed", 
                    "Your visit to " + booking.getEstablishment().getName() + " has been marked as completed.", 
                    NotificationService.NotificationType.BOOKING_CONFIRMATION
                );
            } catch (Exception e) {
                System.err.println("Failed to send visit completion notification: " + e.getMessage());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to mark visit as completed: " + e.getMessage());
        }
    }

    public Map<String, Object> getEstablishmentVisitStats(String ownerEmail) {
        try {
            Optional<User> ownerOpt = userRepository.findByEmail(ownerEmail);
            if (ownerOpt.isEmpty()) {
                throw new RuntimeException("Owner not found");
            }
            User owner = ownerOpt.get();

            List<Booking> allBookings = bookingRepository.findByEstablishmentOwnerIdOrderByCreatedAtDesc(owner.getId());
            
            Map<String, Object> stats = new HashMap<>();
            
            long totalBookings = allBookings.size();
            long confirmedBookings = allBookings.stream().mapToLong(b -> 
                b.getStatus() == BookingStatus.CONFIRMED ? 1 : 0).sum();
            long completedVisits = allBookings.stream().mapToLong(b -> 
                b.getStatus() == BookingStatus.COMPLETED ? 1 : 0).sum();
            long pendingVisits = confirmedBookings - completedVisits;
            
            double totalRevenue = allBookings.stream()
                .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(b -> b.getPaymentAmount() != null ? b.getPaymentAmount().doubleValue() : 0.0)
                .sum();
            
            stats.put("totalBookings", totalBookings);
            stats.put("confirmedBookings", confirmedBookings);
            stats.put("completedVisits", completedVisits);
            stats.put("pendingVisits", pendingVisits);
            stats.put("totalRevenue", totalRevenue);
            stats.put("visitCompletionRate", confirmedBookings > 0 ? 
                (double) completedVisits / confirmedBookings * 100 : 0.0);
            
            return stats;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch visit statistics: " + e.getMessage());
        }
    }
}