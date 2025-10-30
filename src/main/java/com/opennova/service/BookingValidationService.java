package com.opennova.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.opennova.model.Booking;
import com.opennova.model.Establishment;
import com.opennova.repository.BookingRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class BookingValidationService {

    @Autowired
    private BookingRepository bookingRepository;

    public static class ValidationResult {
        private boolean valid;
        private String message;
        private java.util.List<String> errors;
        private java.util.Map<String, Object> metadata;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
            this.errors = new java.util.ArrayList<>();
            this.metadata = new java.util.HashMap<>();
            if (!valid && message != null) {
                this.errors.add(message);
            }
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public java.util.List<String> getErrors() { return errors; }
        public java.util.Map<String, Object> getMetadata() { return metadata; }
        
        public void addError(String error) { this.errors.add(error); }
        public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
    }

    public boolean isTimeSlotAvailable(Long establishmentId, String visitingTime) {
        List<Booking> existingBookings = bookingRepository.findByEstablishmentIdAndVisitingTime(
            establishmentId, visitingTime);
        return existingBookings.isEmpty();
    }

    public boolean isValidBookingTime(String time, Establishment establishment) {
        // Basic validation - can be extended based on establishment operating hours
        if (time == null || time.trim().isEmpty()) {
            return false;
        }
        
        try {
            // Try to parse the time to validate format
            LocalTime.parse(time);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isValidBookingDate(String date) {
        // Cannot book for past dates
        if (date == null || date.trim().isEmpty()) {
            return false;
        }
        
        try {
            LocalDate bookingDate = LocalDate.parse(date);
            return !bookingDate.isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean canUserBook(Long userId, Long establishmentId, String date) {
        // For now, allow multiple bookings - can be restricted later if needed
        return true;
    }

    public String validateBooking(Booking booking) {
        if (booking.getEstablishment() == null) {
            return "Establishment is required";
        }
        
        if (booking.getUser() == null) {
            return "User is required";
        }
        
        if (booking.getVisitingDate() == null) {
            return "Visiting date is required";
        }
        
        if (booking.getVisitingTime() == null) {
            return "Visiting time is required";
        }
        
        if (!isValidBookingDate(booking.getVisitingDate())) {
            return "Cannot book for past dates";
        }
        
        if (!isValidBookingTime(booking.getVisitingTime(), booking.getEstablishment())) {
            return "Invalid booking time";
        }
        
        if (!isTimeSlotAvailable(booking.getEstablishment().getId(), 
                                booking.getVisitingTime())) {
            return "Time slot is not available";
        }
        
        if (!canUserBook(booking.getUser().getId(), 
                        booking.getEstablishment().getId(), 
                        booking.getVisitingDate())) {
            return "You already have a booking for this establishment on this date";
        }
        
        return null; // No validation errors
    }

    public ValidationResult validateBookingAgainstMenu(Long establishmentId, String selectedItems, String visitingTime) {
        try {
            // Basic validation - can be extended based on business logic
            if (selectedItems == null || selectedItems.trim().isEmpty()) {
                return new ValidationResult(false, "Selected items cannot be empty");
            }
            
            if (visitingTime == null || visitingTime.trim().isEmpty()) {
                return new ValidationResult(false, "Visiting time is required");
            }
            
            // Additional validation logic can be added here
            return new ValidationResult(true, "Validation successful");
        } catch (Exception e) {
            return new ValidationResult(false, "Validation failed: " + e.getMessage());
        }
    }
}