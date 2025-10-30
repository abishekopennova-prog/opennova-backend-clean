package com.opennova.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.opennova.model.User;
import com.opennova.model.Booking;
import com.opennova.model.Review;
import com.opennova.model.BookingStatus;

@Service
public class NotificationService {

    @Autowired
    private EmailService emailService;

    public enum NotificationType {
        BOOKING_CONFIRMATION,
        BOOKING_CANCELLATION,
        REVIEW_NOTIFICATION,
        SYSTEM_ALERT
    }

    public void sendBookingConfirmation(Booking booking) {
        try {
            String subject = "Booking Confirmation - " + booking.getEstablishment().getName();
            String message = String.format(
                "Dear %s,\n\nYour booking has been confirmed!\n\n" +
                "Establishment: %s\n" +
                "Date: %s\n" +
                "Time: %s\n" +
                "Booking ID: %s\n\n" +
                "Thank you for choosing OpenNova!",
                booking.getUser().getName(),
                booking.getEstablishment().getName(),
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getId()
            );
            
            emailService.sendEmail(booking.getUser().getEmail(), subject, message);
        } catch (Exception e) {
            // Log error but don't fail the booking process
            System.err.println("Failed to send booking confirmation email: " + e.getMessage());
        }
    }

    public void sendBookingCancellation(Booking booking) {
        try {
            String subject = "Booking Cancelled - " + booking.getEstablishment().getName();
            String message = String.format(
                "Dear %s,\n\nYour booking has been cancelled.\n\n" +
                "Establishment: %s\n" +
                "Date: %s\n" +
                "Time: %s\n" +
                "Booking ID: %s\n\n" +
                "If you have any questions, please contact us.",
                booking.getUser().getName(),
                booking.getEstablishment().getName(),
                booking.getBookingDate(),
                booking.getBookingTime(),
                booking.getId()
            );
            
            emailService.sendEmail(booking.getUser().getEmail(), subject, message);
        } catch (Exception e) {
            System.err.println("Failed to send booking cancellation email: " + e.getMessage());
        }
    }

    public void sendReviewNotification(Review review) {
        try {
            String subject = "New Review Received - " + review.getEstablishment().getName();
            String message = String.format(
                "A new review has been posted for %s\n\n" +
                "Rating: %d/5\n" +
                "Comment: %s\n" +
                "Reviewer: %s",
                review.getEstablishment().getName(),
                review.getRating(),
                review.getComment(),
                review.getUser().getName()
            );
            
            // Send to establishment owner
            User owner = review.getEstablishment().getOwner();
            if (owner != null && owner.getEmail() != null) {
                emailService.sendEmail(owner.getEmail(), subject, message);
            }
        } catch (Exception e) {
            System.err.println("Failed to send review notification email: " + e.getMessage());
        }
    }

    public void sendOwnerNotification(Long ownerId, String title, String message, NotificationType type) {
        try {
            // For now, just log the notification
            // In a real implementation, you might store this in a database or send via email
            System.out.println(String.format("Notification to Owner %d: %s - %s", ownerId, title, message));
        } catch (Exception e) {
            System.err.println("Failed to send owner notification: " + e.getMessage());
        }
    }

    public void sendUserNotification(Long userId, String title, String message, NotificationType type) {
        try {
            // For now, just log the notification
            // In a real implementation, you might store this in a database or send via email
            System.out.println(String.format("Notification to User %d: %s - %s", userId, title, message));
        } catch (Exception e) {
            System.err.println("Failed to send user notification: " + e.getMessage());
        }
    }

    public void notifyBookingDeleted(Booking booking, String deletedBy) {
        try {
            String message = String.format("Your booking for %s has been cancelled by %s", 
                booking.getEstablishment().getName(), deletedBy);
            System.out.println(String.format("Booking deletion notification to User %d: %s", 
                booking.getUser().getId(), message));
        } catch (Exception e) {
            System.err.println("Failed to send booking deletion notification: " + e.getMessage());
        }
    }

    public void notifyBookingStatusChange(Booking booking, BookingStatus oldStatus, BookingStatus newStatus) {
        try {
            String message = String.format("Your booking status changed from %s to %s for %s", 
                oldStatus, newStatus, booking.getEstablishment().getName());
            System.out.println(String.format("Status change notification to User %d: %s", 
                booking.getUser().getId(), message));
        } catch (Exception e) {
            System.err.println("Failed to send booking status change notification: " + e.getMessage());
        }
    }
}