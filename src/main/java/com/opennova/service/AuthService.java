package com.opennova.service;

import com.opennova.dto.RegisterRequest;
import com.opennova.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EstablishmentService establishmentService;

    public User registerUser(RegisterRequest registerRequest) {
        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(registerRequest.getRole());
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userService.save(user);
        
        // Auto-assign establishment for owner roles
        if (savedUser.getRole() == com.opennova.model.UserRole.HOSPITAL_OWNER ||
            savedUser.getRole() == com.opennova.model.UserRole.SHOP_OWNER ||
            savedUser.getRole() == com.opennova.model.UserRole.HOTEL_OWNER) {
            
            try {
                establishmentService.assignEstablishmentToUser(savedUser);
                System.out.println("✅ Auto-assigned establishment to new user: " + savedUser.getEmail());
            } catch (Exception e) {
                System.err.println("⚠️ Failed to auto-assign establishment to user: " + e.getMessage());
                // Don't fail registration if establishment assignment fails
            }
        }

        return savedUser;
    }

    public void sendPasswordResetEmail(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("No user found with email: " + email);
        }
        
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1)); // Token expires in 1 hour
        
        userService.save(user);
        
        String resetLink = "http://localhost:3000/reset-password?token=" + resetToken;
        String subject = "Password Reset Request - OpenNova";
        String body = "Dear " + user.getName() + ",\n\n" +
                     "You have requested to reset your password. Please click the link below to reset your password:\n\n" +
                     resetLink + "\n\n" +
                     "This link will expire in 1 hour.\n\n" +
                     "If you did not request this password reset, please ignore this email.\n\n" +
                     "Best regards,\n" +
                     "OpenNova Team";
        
        try {
            emailService.sendEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    public void resetPassword(String token, String newPassword) {
        User user = userService.findByResetToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
        
        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());
        
        userService.save(user);
        
        // Send confirmation email
        String subject = "Password Reset Successful - OpenNova";
        String body = "Dear " + user.getName() + ",\n\n" +
                     "Your password has been successfully reset.\n\n" +
                     "If you did not make this change, please contact our support team immediately.\n\n" +
                     "Best regards,\n" +
                     "OpenNova Team";
        
        emailService.sendEmail(user.getEmail(), subject, body);
    }
}