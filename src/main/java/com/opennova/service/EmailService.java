package com.opennova.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.activation.DataSource;
import jakarta.mail.util.ByteArrayDataSource;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendEmail(String to, String subject, String body) {
        sendEmailSync(to, subject, body);
    }
    
    /**
     * Test email functionality
     */
    public void sendTestEmail(String to) {
        try {
            String subject = "🧪 OpenNova Email Test - System Working";
            String body = String.format(
                "Dear User,\n\n" +
                "This is a test email to verify that the OpenNova email system is working correctly.\n\n" +
                "✅ Email Configuration: WORKING\n" +
                "✅ SMTP Connection: SUCCESSFUL\n" +
                "✅ Email Delivery: CONFIRMED\n\n" +
                "If you received this email, it means:\n" +
                "• Your email address is valid\n" +
                "• Our email server is configured correctly\n" +
                "• Email notifications should work for bookings\n\n" +
                "Test Details:\n" +
                "• Sent from: %s\n" +
                "• Sent to: %s\n" +
                "• Timestamp: %s\n\n" +
                "If you're experiencing issues with booking emails, please contact our support team.\n\n" +
                "Best regards,\n" +
                "OpenNova Technical Team\n" +
                "📧 abishekopennova@gmail.com",
                fromEmail,
                to,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            
            sendEmailSync(to, subject, body);
            System.out.println("✅ Test email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("❌ Test email failed: " + e.getMessage());
            throw new RuntimeException("Test email failed: " + e.getMessage());
        }
    }

    public void sendEmailSync(String to, String subject, String body) {
        try {
            System.out.println("📧 Attempting to send email to: " + to);
            System.out.println("📧 Subject: " + subject);
            System.out.println("📧 From: " + fromEmail);
            
            if (to == null || to.trim().isEmpty()) {
                throw new RuntimeException("Recipient email address is required");
            }
            
            if (fromEmail == null || fromEmail.trim().isEmpty()) {
                throw new RuntimeException("Sender email address is not configured");
            }
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to.trim());
            message.setSubject(subject);
            message.setText(body);
            
            System.out.println("📤 Sending email via JavaMailSender...");
            mailSender.send(message);
            System.out.println("✅ Email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email to " + to + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    public void sendEmailWithQRAttachment(String to, String subject, String body, String qrCodeBase64, String fileName) {
        try {
            System.out.println("📧 Attempting to send email with QR attachment to: " + to);
            
            if (to == null || to.trim().isEmpty()) {
                throw new RuntimeException("Recipient email address is required");
            }
            
            if (fromEmail == null || fromEmail.trim().isEmpty()) {
                throw new RuntimeException("Sender email address is not configured");
            }

            org.springframework.mail.javamail.MimeMessageHelper helper = 
                new org.springframework.mail.javamail.MimeMessageHelper(mailSender.createMimeMessage(), true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(body);
            
            try {
                // Decode base64 QR code and attach as image
                byte[] qrCodeBytes = java.util.Base64.getDecoder().decode(qrCodeBase64);
                DataSource qrDataSource = new ByteArrayDataSource(qrCodeBytes, "image/png");
                helper.addAttachment(fileName, qrDataSource);
                
                System.out.println("📤 Sending email with QR attachment via JavaMailSender...");
                mailSender.send(helper.getMimeMessage());
                System.out.println("✅ Email with QR attachment sent successfully to: " + to);
            } catch (Exception attachmentError) {
                System.err.println("⚠️ Failed to add QR attachment, sending without attachment: " + attachmentError.getMessage());
                // Send without attachment if attachment fails
                helper = new org.springframework.mail.javamail.MimeMessageHelper(mailSender.createMimeMessage(), false);
                helper.setFrom(fromEmail);
                helper.setTo(to.trim());
                helper.setSubject(subject);
                helper.setText(body + "\n\nQR Code Data: " + qrCodeBase64);
                mailSender.send(helper.getMimeMessage());
                System.out.println("✅ Email sent successfully without attachment to: " + to);
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send email with QR attachment to " + to + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email with QR attachment: " + e.getMessage());
        }
    }



    @Async
    public void sendBookingConfirmation(String to, String establishmentName, String bookingDetails, String qrCodeData) {
        String subject = "Booking Confirmation - " + establishmentName;
        String body = "Dear Customer,\n\n" +
                     "Your booking has been confirmed!\n\n" +
                     "Establishment: " + establishmentName + "\n" +
                     "Booking Details: " + bookingDetails + "\n\n" +
                     "Please show the QR code below at the establishment:\n" +
                     "QR Code: " + qrCodeData + "\n\n" +
                     "Thank you for choosing OpenNova!\n\n" +
                     "Best regards,\n" +
                     "OpenNova Team";
        
        sendEmail(to, subject, body);
    }

    @Async
    public void sendBookingConfirmationWithQR(com.opennova.model.Booking booking) {
        try {
            sendBookingQRCode(booking.getUserEmail(), booking, booking.getQrCode());
        } catch (Exception e) {
            System.err.println("Failed to send booking confirmation with QR: " + e.getMessage());
        }
    }

    @Async
    public void sendBookingRejection(com.opennova.model.Booking booking, String reason) {
        try {
            sendBookingRejection(booking.getUserEmail(), booking.getEstablishment().getName(), reason);
        } catch (Exception e) {
            System.err.println("Failed to send booking rejection: " + e.getMessage());
        }
    }

    @Async
    public void sendBookingRejection(String to, String establishmentName, String reason) {
        String subject = "❌ Booking Rejected - " + establishmentName;
        String body = String.format(
            "Dear Customer,\n\n" +
            "❌ We regret to inform you that your booking at %s has been rejected by the establishment.\n\n" +
            "═══════════════════════════════════════\n" +
            "📋 REJECTION DETAILS\n" +
            "═══════════════════════════════════════\n" +
            "🏢 Establishment: %s\n" +
            "❌ Rejection Reason: %s\n" +
            "📅 Status: REJECTED\n\n" +
            "═══════════════════════════════════════\n" +
            "💰 REFUND INFORMATION\n" +
            "═══════════════════════════════════════\n" +
            "💳 Your full payment will be refunded within 24-48 hours\n" +
            "📧 You'll receive a refund confirmation email\n" +
            "🏦 Amount will be credited to your original payment method\n\n" +
            "═══════════════════════════════════════\n" +
            "🔄 NEXT STEPS\n" +
            "═══════════════════════════════════════\n" +
            "• Browse other similar establishments on OpenNova\n" +
            "• Try booking for a different date/time\n" +
            "• Contact the establishment directly for clarification\n" +
            "• Reach out to our support team for assistance\n\n" +
            "We sincerely apologize for any inconvenience caused. 🙏\n\n" +
            "Our team is here to help you find the perfect booking!\n\n" +
            "Best regards,\n" +
            "OpenNova Team\n" +
            "📧 abishekopennova@gmail.com\n" +
            "🌐 www.opennova.com",
            
            establishmentName,
            establishmentName,
            reason
        );
        
        sendEmail(to, subject, body);
    }

    @Async
    public void sendRefundNotification(String to, String establishmentName, double amount) {
        String subject = "Refund Processed - " + establishmentName;
        String body = "Dear Customer,\n\n" +
                     "Your refund has been processed successfully.\n\n" +
                     "Establishment: " + establishmentName + "\n" +
                     "Refund Amount: ₹" + amount + "\n\n" +
                     "The amount will be credited to your account within 24 hours.\n\n" +
                     "Thank you for your patience.\n\n" +
                     "Best regards,\n" +
                     "OpenNova Team";
        
        sendEmail(to, subject, body);
    }



    @Async
    public void sendBookingQRCode(String to, com.opennova.model.Booking booking, String qrCodeData) {
        try {
            // Try to send with attachment first
            sendBookingQRCodeWithAttachment(to, booking, qrCodeData);
        } catch (Exception e) {
            System.err.println("Failed to send QR code email with attachment, falling back to text: " + e.getMessage());
            // Fallback to text-based QR code
            sendBookingQRCodeAsText(to, booking, qrCodeData);
        }
    }

    private void sendBookingQRCodeWithAttachment(String to, com.opennova.model.Booking booking, String qrCodeData) throws Exception {
        String subject = "🎉 Booking Confirmed - QR Code for " + booking.getEstablishment().getName();
        
        // Parse selected items for detailed display
        String itemsDetails = formatSelectedItems(booking.getSelectedItems(), booking.getEstablishment().getType().toString());
        
        String body = String.format(
            "Dear %s,\n\n" +
            "🎉 GREAT NEWS! Your booking has been confirmed by the establishment.\n\n" +
            "═══════════════════════════════════════\n" +
            "📋 BOOKING DETAILS\n" +
            "═══════════════════════════════════════\n" +
            "🏢 Establishment: %s\n" +
            "📅 Date: %s\n" +
            "🕐 Time: %s\n" +
            "⏱️ Duration: 2 hours\n" +
            "🆔 Booking ID: #%d\n" +
            "💳 Transaction ID: %s\n\n" +
            "═══════════════════════════════════════\n" +
            "🛍️ SELECTED ITEMS/SERVICES\n" +
            "═══════════════════════════════════════\n" +
            "%s\n" +
            "═══════════════════════════════════════\n" +
            "💰 PAYMENT SUMMARY\n" +
            "═══════════════════════════════════════\n" +
            "Total Amount: ₹%.2f\n" +
            "Paid Amount (70%%): ₹%.2f ✅\n" +
            "Remaining Amount (30%%): ₹%.2f\n" +
            "Payment Status: CONFIRMED ✅\n\n" +
            "═══════════════════════════════════════\n" +
            "📱 QR CODE ATTACHMENT\n" +
            "═══════════════════════════════════════\n" +
            "⚠️ IMPORTANT: Please show the attached QR code image at the establishment\n" +
            "📎 Your QR code is attached as an image file to this email\n" +
            "💾 Save the QR code image to your phone for easy access\n\n" +
            "📍 VISIT INSTRUCTIONS:\n" +
            "1. Arrive on time at the establishment\n" +
            "2. Show the QR code image (attached) to the staff\n" +
            "3. Pay the remaining 30%% amount (₹%.2f) at the venue\n" +
            "4. Enjoy your 2-hour visit!\n\n" +
            "═══════════════════════════════════════\n" +
            "📞 ESTABLISHMENT CONTACT\n" +
            "═══════════════════════════════════════\n" +
            "Address: %s\n" +
            "Phone: %s\n" +
            "Email: %s\n\n" +
            "Need help? Contact our support team or reply to this email.\n\n" +
            "Thank you for choosing OpenNova! 🙏\n\n" +
            "Best regards,\n" +
            "OpenNova Team\n" +
            "📧 abishekopennova@gmail.com\n" +
            "🌐 www.opennova.com",
            
            booking.getUser().getName(),
            booking.getEstablishment().getName(),
            booking.getVisitingDate(),
            booking.getVisitingTime(),
            booking.getId(),
            booking.getTransactionId(),
            itemsDetails,
            booking.getAmount().doubleValue(),
            booking.getPaymentAmount().doubleValue(),
            booking.getAmount().doubleValue() - booking.getPaymentAmount().doubleValue(),
            booking.getAmount().doubleValue() - booking.getPaymentAmount().doubleValue(),
            booking.getEstablishment().getAddress() != null ? booking.getEstablishment().getAddress() : "Address not available",
            booking.getEstablishment().getContactNumber() != null ? booking.getEstablishment().getContactNumber() : "Phone not available",
            booking.getEstablishment().getEmail() != null ? booking.getEstablishment().getEmail() : "Email not available"
        );
        
        // Send email with QR code as attachment
        sendEmailWithQRAttachment(to, subject, body, qrCodeData, "booking-qr-" + booking.getId() + ".png");
    }

    private void sendBookingQRCodeAsText(String to, com.opennova.model.Booking booking, String qrCodeData) {
        String subject = "🎉 Booking Confirmed - QR Code for " + booking.getEstablishment().getName();
        
        // Parse selected items for detailed display
        String itemsDetails = formatSelectedItems(booking.getSelectedItems(), booking.getEstablishment().getType().toString());
        
        String body = String.format(
            "Dear %s,\n\n" +
            "🎉 GREAT NEWS! Your booking has been confirmed by the establishment.\n\n" +
            "═══════════════════════════════════════\n" +
            "📋 BOOKING DETAILS\n" +
            "═══════════════════════════════════════\n" +
            "🏢 Establishment: %s\n" +
            "📅 Date: %s\n" +
            "🕐 Time: %s\n" +
            "⏱️ Duration: 2 hours\n" +
            "🆔 Booking ID: #%d\n" +
            "💳 Transaction ID: %s\n\n" +
            "═══════════════════════════════════════\n" +
            "🛍️ SELECTED ITEMS/SERVICES\n" +
            "═══════════════════════════════════════\n" +
            "%s\n" +
            "═══════════════════════════════════════\n" +
            "💰 PAYMENT SUMMARY\n" +
            "═══════════════════════════════════════\n" +
            "Total Amount: ₹%.2f\n" +
            "Paid Amount (70%%): ₹%.2f ✅\n" +
            "Remaining Amount (30%%): ₹%.2f\n" +
            "Payment Status: CONFIRMED ✅\n\n" +
            "═══════════════════════════════════════\n" +
            "📱 QR CODE DATA\n" +
            "═══════════════════════════════════════\n" +
            "⚠️ IMPORTANT: Show this QR code at the establishment\n" +
            "📱 Use any QR code generator app to create a QR code from this data:\n\n" +
            "QR Code Data:\n%s\n\n" +
            "📍 VISIT INSTRUCTIONS:\n" +
            "1. Generate QR code from the data above using any QR app\n" +
            "2. Arrive on time at the establishment\n" +
            "3. Show the QR code to the staff\n" +
            "4. Pay the remaining 30%% amount (₹%.2f) at the venue\n" +
            "5. Enjoy your 2-hour visit!\n\n" +
            "═══════════════════════════════════════\n" +
            "📞 ESTABLISHMENT CONTACT\n" +
            "═══════════════════════════════════════\n" +
            "Address: %s\n" +
            "Phone: %s\n" +
            "Email: %s\n\n" +
            "Need help? Contact our support team or reply to this email.\n\n" +
            "Thank you for choosing OpenNova! 🙏\n\n" +
            "Best regards,\n" +
            "OpenNova Team\n" +
            "📧 abishekopennova@gmail.com\n" +
            "🌐 www.opennova.com",
            
            booking.getUser().getName(),
            booking.getEstablishment().getName(),
            booking.getVisitingDate(),
            booking.getVisitingTime(),
            booking.getId(),
            booking.getTransactionId(),
            itemsDetails,
            booking.getAmount().doubleValue(),
            booking.getPaymentAmount().doubleValue(),
            booking.getAmount().doubleValue() - booking.getPaymentAmount().doubleValue(),
            qrCodeData,
            booking.getAmount().doubleValue() - booking.getPaymentAmount().doubleValue(),
            booking.getEstablishment().getAddress() != null ? booking.getEstablishment().getAddress() : "Address not available",
            booking.getEstablishment().getContactNumber() != null ? booking.getEstablishment().getContactNumber() : "Phone not available",
            booking.getEstablishment().getEmail() != null ? booking.getEstablishment().getEmail() : "Email not available"
        );
        
        sendEmail(to, subject, body);
    }

    @Async
    public void sendOwnerCancellationNotification(String to, com.opennova.model.Booking booking, String reason) {
        String subject = "❌ Booking Cancelled by Owner - " + booking.getEstablishment().getName();
        
        // Parse selected items for detailed display
        String itemsDetails = formatSelectedItems(booking.getSelectedItems(), booking.getEstablishment().getType().toString());
        
        String body = String.format(
            "Dear %s,\n\n" +
            "❌ We regret to inform you that your booking has been cancelled by the establishment owner.\n\n" +
            "═══════════════════════════════════════\n" +
            "📋 CANCELLED BOOKING DETAILS\n" +
            "═══════════════════════════════════════\n" +
            "🏢 Establishment: %s\n" +
            "📅 Date: %s\n" +
            "🕐 Time: %s\n" +
            "🆔 Booking ID: #%d\n" +
            "💳 Transaction ID: %s\n" +
            "❌ Cancellation Reason: %s\n\n" +
            "═══════════════════════════════════════\n" +
            "🛍️ CANCELLED ITEMS/SERVICES\n" +
            "═══════════════════════════════════════\n" +
            "%s\n" +
            "═══════════════════════════════════════\n" +
            "💰 FULL REFUND GUARANTEED\n" +
            "═══════════════════════════════════════\n" +
            "Total Amount: ₹%.2f\n" +
            "Paid Amount: ₹%.2f\n" +
            "Refund Amount: ₹%.2f (100%% REFUND) ✅\n" +
            "Refund Status: APPROVED\n\n" +
            "💳 REFUND TIMELINE:\n" +
            "- Full refund will be processed within 24 hours\n" +
            "- Amount will be credited to your original payment method\n" +
            "- You'll receive a confirmation email once processed\n\n" +
            "═══════════════════════════════════════\n" +
            "📞 ESTABLISHMENT CONTACT\n" +
            "═══════════════════════════════════════\n" +
            "Address: %s\n" +
            "Phone: %s\n" +
            "Email: %s\n\n" +
            "═══════════════════════════════════════\n" +
            "🔄 ALTERNATIVE OPTIONS\n" +
            "═══════════════════════════════════════\n" +
            "• Browse similar establishments on OpenNova\n" +
            "• Try booking for a different date/time\n" +
            "• Contact our support team for personalized recommendations\n" +
            "• Get assistance with finding alternative venues\n\n" +
            "We sincerely apologize for this inconvenience and any disruption to your plans. 🙏\n\n" +
            "Our support team is available 24/7 to help you find alternative options!\n\n" +
            "Best regards,\n" +
            "OpenNova Team\n" +
            "📧 abishekopennova@gmail.com\n" +
            "🌐 www.opennova.com",
            
            booking.getUser().getName(),
            booking.getEstablishment().getName(),
            booking.getVisitingDate(),
            booking.getVisitingTime(),
            booking.getId(),
            booking.getTransactionId(),
            reason,
            itemsDetails,
            booking.getAmount().doubleValue(),
            booking.getPaymentAmount().doubleValue(),
            booking.getPaymentAmount().doubleValue(),
            booking.getEstablishment().getAddress() != null ? booking.getEstablishment().getAddress() : "Address not available",
            booking.getEstablishment().getContactNumber() != null ? booking.getEstablishment().getContactNumber() : "Phone not available",
            booking.getEstablishment().getEmail() != null ? booking.getEstablishment().getEmail() : "Email not available"
        );
        
        sendEmail(to, subject, body);
    }

    @Async
    public void sendEstablishmentDeletionNotification(com.opennova.model.Establishment establishment) {
        String subject = "Establishment Deleted - " + establishment.getName();
        String body = "Dear " + establishment.getName() + " Owner,\n\n" +
                     "Your establishment has been successfully deleted from OpenNova platform.\n\n" +
                     "Establishment Details:\n" +
                     "Name: " + establishment.getName() + "\n" +
                     "Type: " + establishment.getType().toString() + "\n" +
                     "Address: " + establishment.getAddress() + "\n\n" +
                     "All related data including:\n" +
                     "- Bookings\n" +
                     "- Reviews\n" +
                     "- Menu items (if applicable)\n" +
                     "- Doctor profiles (if applicable)\n" +
                     "- Product collections (if applicable)\n" +
                     "- Special offers\n\n" +
                     "Have been permanently removed from the system.\n\n" +
                     "If you wish to re-register your establishment in the future, " +
                     "you can submit a new establishment request.\n\n" +
                     "Thank you for being part of OpenNova.\n\n" +
                     "Best regards,\n" +
                     "OpenNova Team";
        
        sendEmail(establishment.getEmail(), subject, body);
    }

    @Async
    public void sendEstablishmentApprovalWithCredentials(com.opennova.model.Establishment establishment) {
        try {
            // Generate temporary password
            String tempPassword = generateTemporaryPassword();
            
            String subject = "Establishment Approved - OpenNova Platform Access";
            String body = String.format(
                "Dear %s,\n\n" +
                "Congratulations! Your establishment '%s' has been approved and is now live on the OpenNova platform.\n\n" +
                "Your login credentials:\n" +
                "Email: %s\n" +
                "Temporary Password: %s\n\n" +
                "Please log in to your owner portal using these credentials. You will be prompted to change your password on first login.\n\n" +
                "Portal Access: Based on your establishment type (%s), you will be redirected to the appropriate management portal.\n\n" +
                "Best regards,\n" +
                "OpenNova Team",
                establishment.getName(),
                establishment.getName(),
                establishment.getEmail(),
                tempPassword,
                establishment.getType().toString()
            );
            
            sendEmail(establishment.getEmail(), subject, body);
            
            // Store temporary password (in production, hash this)
            System.out.println("Temporary password for " + establishment.getEmail() + ": " + tempPassword);
            
        } catch (Exception e) {
            System.err.println("Failed to send establishment approval email: " + e.getMessage());
        }
    }

    @Async
    public void sendBookingConfirmation(com.opennova.model.Booking booking) {
        try {
            String subject = "✅ Booking Created Successfully - " + booking.getEstablishment().getName();
            
            // Parse selected items for detailed display
            String itemsDetails = formatSelectedItems(booking.getSelectedItems(), booking.getEstablishment().getType().toString());
            
            String body = String.format(
                "Dear %s,\n\n" +
                "✅ Your booking has been created successfully!\n\n" +
                "═══════════════════════════════════════\n" +
                "📋 BOOKING DETAILS\n" +
                "═══════════════════════════════════════\n" +
                "🏢 Establishment: %s\n" +
                "📅 Date: %s\n" +
                "🕐 Time: %s\n" +
                "⏱️ Duration: 2 hours\n" +
                "🆔 Booking ID: #%d\n" +
                "💳 Transaction ID: %s\n" +
                "📊 Status: PENDING (Awaiting owner confirmation)\n\n" +
                "═══════════════════════════════════════\n" +
                "🛍️ SELECTED ITEMS/SERVICES\n" +
                "═══════════════════════════════════════\n" +
                "%s\n" +
                "═══════════════════════════════════════\n" +
                "💰 PAYMENT SUMMARY\n" +
                "═══════════════════════════════════════\n" +
                "Total Amount: ₹%.2f\n" +
                "Paid Amount (70%%): ₹%.2f ✅\n" +
                "Remaining Amount (30%%): ₹%.2f\n" +
                "Payment Status: CONFIRMED ✅\n\n" +
                "═══════════════════════════════════════\n" +
                "📞 ESTABLISHMENT CONTACT\n" +
                "═══════════════════════════════════════\n" +
                "Address: %s\n" +
                "Phone: %s\n" +
                "Email: %s\n\n" +
                "⏳ NEXT STEPS:\n" +
                "1. Wait for the establishment owner to confirm your booking\n" +
                "2. You'll receive a QR code once confirmed\n" +
                "3. Present the QR code at the establishment on your visit date\n" +
                "4. Pay the remaining 30%% amount at the venue\n\n" +
                "📧 You'll receive another email with your QR code once the booking is confirmed by the owner.\n\n" +
                "Need help? Contact our support team or reply to this email.\n\n" +
                "Thank you for choosing OpenNova! 🙏\n\n" +
                "Best regards,\n" +
                "OpenNova Team\n" +
                "📧 abishekopennova@gmail.com\n" +
                "🌐 www.opennova.com",
                
                booking.getUser().getName(),
                booking.getEstablishment().getName(),
                booking.getVisitingDate(),
                booking.getVisitingTime(),
                booking.getId(),
                booking.getTransactionId(),
                itemsDetails,
                booking.getAmount().doubleValue(),
                booking.getPaymentAmount().doubleValue(),
                booking.getAmount().doubleValue() - booking.getPaymentAmount().doubleValue(),
                booking.getEstablishment().getAddress() != null ? booking.getEstablishment().getAddress() : "Address not available",
                booking.getEstablishment().getContactNumber() != null ? booking.getEstablishment().getContactNumber() : "Phone not available",
                booking.getEstablishment().getEmail() != null ? booking.getEstablishment().getEmail() : "Email not available"
            );
            
            sendEmail(booking.getUserEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send booking confirmation: " + e.getMessage());
        }
    }

    @Async
    public void sendBookingCancellation(com.opennova.model.Booking booking) {
        try {
            String subject = "❌ Booking Cancelled - " + booking.getEstablishment().getName();
            
            // Parse selected items for detailed display
            String itemsDetails = formatSelectedItems(booking.getSelectedItems(), booking.getEstablishment().getType().toString());
            
            String body = String.format(
                "Dear %s,\n\n" +
                "❌ We regret to inform you that your booking has been cancelled.\n\n" +
                "═══════════════════════════════════════\n" +
                "📋 CANCELLED BOOKING DETAILS\n" +
                "═══════════════════════════════════════\n" +
                "🏢 Establishment: %s\n" +
                "📅 Date: %s\n" +
                "🕐 Time: %s\n" +
                "🆔 Booking ID: #%d\n" +
                "💳 Transaction ID: %s\n" +
                "❌ Cancellation Reason: %s\n\n" +
                "═══════════════════════════════════════\n" +
                "🛍️ CANCELLED ITEMS/SERVICES\n" +
                "═══════════════════════════════════════\n" +
                "%s\n" +
                "═══════════════════════════════════════\n" +
                "💰 REFUND INFORMATION\n" +
                "═══════════════════════════════════════\n" +
                "Total Amount: ₹%.2f\n" +
                "Paid Amount: ₹%.2f\n" +
                "Refund Status: %s\n" +
                "Refund Amount: ₹%.2f\n\n" +
                "💳 REFUND TIMELINE:\n" +
                "- Refund will be processed within 24-48 hours\n" +
                "- Amount will be credited to your original payment method\n" +
                "- You'll receive a confirmation email once processed\n\n" +
                "═══════════════════════════════════════\n" +
                "📞 ESTABLISHMENT CONTACT\n" +
                "═══════════════════════════════════════\n" +
                "Address: %s\n" +
                "Phone: %s\n" +
                "Email: %s\n\n" +
                "We sincerely apologize for any inconvenience caused. 🙏\n\n" +
                "If you have any questions about your refund or need assistance with a new booking, please contact our support team.\n\n" +
                "Thank you for your understanding.\n\n" +
                "Best regards,\n" +
                "OpenNova Team\n" +
                "📧 abishekopennova@gmail.com\n" +
                "🌐 www.opennova.com",
                
                booking.getUser().getName(),
                booking.getEstablishment().getName(),
                booking.getVisitingDate(),
                booking.getVisitingTime(),
                booking.getId(),
                booking.getTransactionId(),
                booking.getCancellationReason() != null ? booking.getCancellationReason() : "Not specified",
                itemsDetails,
                booking.getAmount().doubleValue(),
                booking.getPaymentAmount().doubleValue(),
                booking.getRefundStatus(),
                booking.getPaymentAmount().doubleValue(),
                booking.getEstablishment().getAddress() != null ? booking.getEstablishment().getAddress() : "Address not available",
                booking.getEstablishment().getContactNumber() != null ? booking.getEstablishment().getContactNumber() : "Phone not available",
                booking.getEstablishment().getEmail() != null ? booking.getEstablishment().getEmail() : "Email not available"
            );
            
            sendEmail(booking.getUserEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send booking cancellation: " + e.getMessage());
        }
    }

    private String formatSelectedItems(String selectedItemsJson, String establishmentType) {
        try {
            if (selectedItemsJson == null || selectedItemsJson.trim().isEmpty()) {
                return "No items selected";
            }
            
            // Parse JSON string to extract items
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, Object>> items = mapper.readValue(selectedItemsJson, 
                mapper.getTypeFactory().constructCollectionType(java.util.List.class, java.util.Map.class));
            
            if (items.isEmpty()) {
                return "No items selected";
            }
            
            StringBuilder itemsText = new StringBuilder();
            double totalAmount = 0.0;
            
            for (int i = 0; i < items.size(); i++) {
                java.util.Map<String, Object> item = items.get(i);
                
                // Extract common fields
                String name = getStringValue(item, "name", "itemName", "doctorName");
                String description = getStringValue(item, "description", "specialization");
                String brand = getStringValue(item, "brand");
                String fabric = getStringValue(item, "fabric");
                String category = getStringValue(item, "category");
                String availability = getStringValue(item, "availability");
                
                double price = getDoubleValue(item, "price", "consultationFee");
                int quantity = getIntValue(item, "quantity", 1);
                double itemTotal = price * quantity;
                totalAmount += itemTotal;
                
                // Format based on establishment type
                itemsText.append(String.format("%d. ", i + 1));
                
                if ("HOSPITAL".equals(establishmentType)) {
                    itemsText.append(String.format("👨‍⚕️ Dr. %s", name != null ? name : "Unknown Doctor"));
                    if (description != null) {
                        itemsText.append(String.format("\n   Specialization: %s", description));
                    }
                    if (availability != null) {
                        itemsText.append(String.format("\n   Available: %s", availability));
                    }
                    itemsText.append(String.format("\n   Consultation Fee: ₹%.2f", price));
                    
                } else if ("SHOP".equals(establishmentType)) {
                    itemsText.append(String.format("🛍️ %s", name != null ? name : "Unknown Item"));
                    if (brand != null) {
                        itemsText.append(String.format("\n   Brand: %s", brand));
                    }
                    if (fabric != null) {
                        itemsText.append(String.format("\n   Material: %s", fabric));
                    }
                    if (category != null) {
                        itemsText.append(String.format("\n   Category: %s", category));
                    }
                    if (quantity > 1) {
                        itemsText.append(String.format("\n   Quantity: %d", quantity));
                    }
                    itemsText.append(String.format("\n   Price: ₹%.2f", price));
                    if (quantity > 1) {
                        itemsText.append(String.format(" x %d = ₹%.2f", quantity, itemTotal));
                    }
                    
                } else { // HOTEL or default
                    itemsText.append(String.format("🍽️ %s", name != null ? name : "Unknown Item"));
                    if (description != null) {
                        itemsText.append(String.format("\n   Description: %s", description));
                    }
                    if (category != null) {
                        itemsText.append(String.format("\n   Category: %s", category));
                    }
                    if (quantity > 1) {
                        itemsText.append(String.format("\n   Quantity: %d", quantity));
                    }
                    itemsText.append(String.format("\n   Price: ₹%.2f", price));
                    if (quantity > 1) {
                        itemsText.append(String.format(" x %d = ₹%.2f", quantity, itemTotal));
                    }
                }
                
                if (i < items.size() - 1) {
                    itemsText.append("\n\n");
                }
            }
            
            // Add total
            itemsText.append(String.format("\n\n💰 TOTAL: ₹%.2f", totalAmount));
            
            return itemsText.toString();
            
        } catch (Exception e) {
            System.err.println("Error formatting selected items: " + e.getMessage());
            return "Items: " + selectedItemsJson;
        }
    }
    
    private String getStringValue(java.util.Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString();
            }
        }
        return null;
    }
    
    private double getDoubleValue(java.util.Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                try {
                    return Double.parseDouble(value.toString());
                } catch (NumberFormatException e) {
                    // Continue to next key
                }
            }
        }
        return 0.0;
    }
    
    private int getIntValue(java.util.Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }



    @Async
    public void sendOwnerNotificationForCustomerCancellation(String ownerEmail, com.opennova.model.Booking booking, long hoursUntilBooking) {
        String subject = "📋 Customer Cancellation - " + booking.getEstablishment().getName();
        
        // Parse selected items for detailed display
        String itemsDetails = formatSelectedItems(booking.getSelectedItems(), booking.getEstablishment().getType().toString());
        
        String refundStatus = hoursUntilBooking >= 2 ? "APPROVED (Full Refund)" : "NOT ELIGIBLE (No Refund)";
        String refundReason = hoursUntilBooking >= 2 ? 
            "Cancelled more than 2 hours before booking time" : 
            "Cancelled within 2 hours of booking time";
        
        String body = String.format(
            "Dear %s Owner,\n\n" +
            "📋 A customer has cancelled their booking at your establishment.\n\n" +
            "═══════════════════════════════════════\n" +
            "📋 CANCELLED BOOKING DETAILS\n" +
            "═══════════════════════════════════════\n" +
            "🏢 Establishment: %s\n" +
            "👤 Customer: %s (%s)\n" +
            "📅 Date: %s\n" +
            "🕐 Time: %s\n" +
            "🆔 Booking ID: #%d\n" +
            "💳 Transaction ID: %s\n" +
            "⏰ Cancelled: %s hours before booking\n" +
            "❌ Cancellation Time: %s\n\n" +
            "═══════════════════════════════════════\n" +
            "🛍️ CANCELLED ITEMS/SERVICES\n" +
            "═══════════════════════════════════════\n" +
            "%s\n" +
            "═══════════════════════════════════════\n" +
            "💰 REFUND INFORMATION\n" +
            "═══════════════════════════════════════\n" +
            "Total Amount: ₹%.2f\n" +
            "Customer Paid: ₹%.2f\n" +
            "Refund Status: %s\n" +
            "Refund Reason: %s\n\n" +
            "═══════════════════════════════════════\n" +
            "📊 BOOKING POLICY APPLIED\n" +
            "═══════════════════════════════════════\n" +
            "• Cancel before 2 hours → Full refund\n" +
            "• Cancel within 2 hours → No refund\n" +
            "• Visiting duration: 2 hours\n\n" +
            "═══════════════════════════════════════\n" +
            "🔄 NEXT STEPS\n" +
            "═══════════════════════════════════════\n" +
            "• This time slot is now available for new bookings\n" +
            "• Customer has been notified about refund status\n" +
            "• No action required from your side\n" +
            "• Update your availability if needed\n\n" +
            "This is an automated notification to keep you informed about booking changes.\n\n" +
            "Best regards,\n" +
            "OpenNova Team\n" +
            "📧 abishekopennova@gmail.com\n" +
            "🌐 www.opennova.com",
            
            booking.getEstablishment().getName(),
            booking.getEstablishment().getName(),
            booking.getUser().getName(),
            booking.getUserEmail(),
            booking.getVisitingDate(),
            booking.getVisitingTime(),
            booking.getId(),
            booking.getTransactionId(),
            hoursUntilBooking,
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            itemsDetails,
            booking.getAmount().doubleValue(),
            booking.getPaymentAmount().doubleValue(),
            refundStatus,
            refundReason
        );
        
        sendEmail(ownerEmail, subject, body);
    }

    private String generateTemporaryPassword() {
        // Generate a secure temporary password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }

    public void sendOwnerCredentials(com.opennova.model.User ownerUser, String tempPassword) {
        try {
            System.out.println("📧 Preparing to send owner credentials email...");
            System.out.println("Owner: " + ownerUser.getName() + " (" + ownerUser.getEmail() + ")");
            System.out.println("Password: " + tempPassword);
            
            String subject = "Welcome to OpenNova - Owner Account Created";
            String body = String.format(
                "Dear %s Owner,\n\n" +
                "Welcome to OpenNova! Your owner account has been created successfully by our admin team.\n\n" +
                "═══════════════════════════════════════\n" +
                "YOUR LOGIN CREDENTIALS\n" +
                "═══════════════════════════════════════\n" +
                "Email: %s\n" +
                "Temporary Password: %s\n\n" +
                "═══════════════════════════════════════\n" +
                "ACCESS YOUR PORTAL\n" +
                "═══════════════════════════════════════\n" +
                "Portal URL: http://localhost:3000/login\n\n" +
                "NEXT STEPS:\n" +
                "1. Click the link above or visit the portal\n" +
                "2. Log in using your credentials\n" +
                "3. Change your password immediately for security\n" +
                "4. Complete your establishment profile\n" +
                "5. Set up your services/menu\n" +
                "6. Start accepting bookings!\n\n" +
                "SECURITY REMINDER:\n" +
                "Please change your password after your first login for security purposes.\n\n" +
                "If you have any questions or need assistance setting up your establishment, please contact our support team.\n\n" +
                "Welcome to the OpenNova family!\n\n" +
                "Best regards,\n" +
                "OpenNova Admin Team\n" +
                "admin@opennova.com\n" +
                "www.opennova.com",
                ownerUser.getName(),
                ownerUser.getEmail(),
                tempPassword
            );
            
            System.out.println("📤 Sending email synchronously to: " + ownerUser.getEmail());
            sendEmailSync(ownerUser.getEmail(), subject, body);
            System.out.println("✅ Owner credentials email sent successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send owner credentials email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send owner credentials email: " + e.getMessage());
        }
    }

    @Async
    public void sendEstablishmentRequestApproval(String to, String establishmentName, String email, String password) {
        try {
            String subject = "🎉 Establishment Approved - Welcome to OpenNova!";
            String body = String.format(
                "Dear %s,\n\n" +
                "Congratulations! Your establishment request has been approved and your account is now active.\n\n" +
                "📋 ESTABLISHMENT DETAILS:\n" +
                "• Name: %s\n" +
                "• Email: %s\n\n" +
                "🔐 LOGIN CREDENTIALS:\n" +
                "• Email: %s\n" +
                "• Password: %s\n\n" +
                "🌐 ACCESS YOUR PORTAL:\n" +
                "You can now log in to your owner portal at: http://localhost:3000/login\n\n" +
                "📱 NEXT STEPS:\n" +
                "1. Log in to your owner portal\n" +
                "2. Update your establishment profile\n" +
                "3. Set up your menu/services\n" +
                "4. Configure your operating hours\n" +
                "5. Start accepting bookings!\n\n" +
                "🔒 SECURITY NOTE:\n" +
                "Please change your password after your first login for security purposes.\n\n" +
                "If you have any questions or need assistance, please contact our support team.\n\n" +
                "Welcome to the OpenNova family!\n\n" +
                "Best regards,\n" +
                "OpenNova Admin Team\n" +
                "📧 admin@opennova.com\n" +
                "🌐 www.opennova.com",
                establishmentName,
                establishmentName,
                email,
                email,
                password
            );
            
            sendEmail(to, subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send establishment approval email: " + e.getMessage());
            throw new RuntimeException("Failed to send establishment approval email: " + e.getMessage());
        }
    }

    @Async
    public void sendBookingApprovalWithDetails(com.opennova.model.Booking booking) {
        try {
            String subject = "🎉 Booking APPROVED - " + booking.getEstablishment().getName();
            
            // Parse selected items for detailed display
            String itemsDetails = formatSelectedItems(booking.getSelectedItems(), booking.getEstablishment().getType().toString());
            
            String body = String.format(
                "Dear %s,\n\n" +
                "🎉 EXCELLENT NEWS! Your booking has been APPROVED by %s!\n\n" +
                "═══════════════════════════════════════\n" +
                "📋 APPROVED BOOKING DETAILS\n" +
                "═══════════════════════════════════════\n" +
                "🏢 Establishment: %s\n" +
                "📅 Visit Date: %s\n" +
                "🕐 Visit Time: %s\n" +
                "⏱️ Duration: 2 hours\n" +
                "🆔 Booking ID: #%d\n" +
                "💳 Transaction ID: %s\n" +
                "✅ Status: CONFIRMED\n\n" +
                "═══════════════════════════════════════\n" +
                "🛍️ CONFIRMED ITEMS/SERVICES\n" +
                "═══════════════════════════════════════\n" +
                "%s\n" +
                "═══════════════════════════════════════\n" +
                "💰 PAYMENT SUMMARY\n" +
                "═══════════════════════════════════════\n" +
                "Total Amount: ₹%.2f\n" +
                "✅ Paid Amount (70%%): ₹%.2f (CONFIRMED)\n" +
                "💳 Remaining Amount (30%%): ₹%.2f (Pay at venue)\n" +
                "Payment Status: APPROVED ✅\n\n" +
                "═══════════════════════════════════════\n" +
                "📱 YOUR QR CODE\n" +
                "═══════════════════════════════════════\n" +
                "⚠️ IMPORTANT: Show this QR code at the establishment\n" +
                "QR Code: %s\n\n" +
                "📍 VISIT INSTRUCTIONS:\n" +
                "1. ✅ Arrive on time at %s\n" +
                "2. 📱 Present this QR code to the staff\n" +
                "3. 💳 Pay the remaining ₹%.2f at the venue\n" +
                "4. 🎉 Enjoy your 2-hour experience!\n\n" +
                "═══════════════════════════════════════\n" +
                "📞 ESTABLISHMENT CONTACT\n" +
                "═══════════════════════════════════════\n" +
                "📍 Address: %s\n" +
                "📞 Phone: %s\n" +
                "📧 Email: %s\n\n" +
                "═══════════════════════════════════════\n" +
                "🎯 IMPORTANT REMINDERS\n" +
                "═══════════════════════════════════════\n" +
                "• Save this email for your records\n" +
                "• Arrive 10 minutes early\n" +
                "• Bring a valid ID\n" +
                "• Keep your phone charged for the QR code\n" +
                "• Contact the establishment if you need to reschedule\n\n" +
                "We're excited for your visit! 🌟\n\n" +
                "Need assistance? Our support team is here to help!\n\n" +
                "Best regards,\n" +
                "OpenNova Team\n" +
                "📧 abishekopennova@gmail.com\n" +
                "🌐 www.opennova.com",
                
                booking.getUser().getName(),
                booking.getEstablishment().getName(),
                booking.getEstablishment().getName(),
                booking.getVisitingDate(),
                booking.getVisitingTime(),
                booking.getId(),
                booking.getTransactionId(),
                itemsDetails,
                booking.getAmount().doubleValue(),
                booking.getPaymentAmount().doubleValue(),
                booking.getAmount().doubleValue() - booking.getPaymentAmount().doubleValue(),
                booking.getQrCode() != null ? booking.getQrCode() : "QR Code will be generated",
                booking.getEstablishment().getName(),
                booking.getAmount().doubleValue() - booking.getPaymentAmount().doubleValue(),
                booking.getEstablishment().getAddress() != null ? booking.getEstablishment().getAddress() : "Address not available",
                booking.getEstablishment().getContactNumber() != null ? booking.getEstablishment().getContactNumber() : "Phone not available",
                booking.getEstablishment().getEmail() != null ? booking.getEstablishment().getEmail() : "Email not available"
            );
            
            sendEmail(booking.getUserEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send booking approval email: " + e.getMessage());
        }
    }

    @Async
    public void sendBookingRejectionWithDetails(com.opennova.model.Booking booking, String reason) {
        try {
            String subject = "❌ Booking REJECTED - " + booking.getEstablishment().getName();
            
            // Parse selected items for detailed display
            String itemsDetails = formatSelectedItems(booking.getSelectedItems(), booking.getEstablishment().getType().toString());
            
            String body = String.format(
                "Dear %s,\n\n" +
                "❌ We regret to inform you that your booking has been REJECTED by %s.\n\n" +
                "═══════════════════════════════════════\n" +
                "📋 REJECTED BOOKING DETAILS\n" +
                "═══════════════════════════════════════\n" +
                "🏢 Establishment: %s\n" +
                "📅 Requested Date: %s\n" +
                "🕐 Requested Time: %s\n" +
                "🆔 Booking ID: #%d\n" +
                "💳 Transaction ID: %s\n" +
                "❌ Status: REJECTED\n" +
                "📝 Rejection Reason: %s\n\n" +
                "═══════════════════════════════════════\n" +
                "🛍️ REJECTED ITEMS/SERVICES\n" +
                "═══════════════════════════════════════\n" +
                "%s\n" +
                "═══════════════════════════════════════\n" +
                "💰 FULL REFUND GUARANTEED\n" +
                "═══════════════════════════════════════\n" +
                "Total Amount: ₹%.2f\n" +
                "Paid Amount: ₹%.2f\n" +
                "💳 Refund Amount: ₹%.2f (100%% REFUND) ✅\n" +
                "Refund Status: APPROVED\n\n" +
                "💳 REFUND PROCESS:\n" +
                "• Full refund will be processed within 24 hours\n" +
                "• Amount will be credited to your original payment method\n" +
                "• You'll receive a confirmation email once processed\n" +
                "• No additional action required from your side\n\n" +
                "═══════════════════════════════════════\n" +
                "📞 ESTABLISHMENT CONTACT\n" +
                "═══════════════════════════════════════\n" +
                "📍 Address: %s\n" +
                "📞 Phone: %s\n" +
                "📧 Email: %s\n\n" +
                "═══════════════════════════════════════\n" +
                "🔄 ALTERNATIVE OPTIONS\n" +
                "═══════════════════════════════════════\n" +
                "• Browse similar establishments on OpenNova\n" +
                "• Try booking for a different date/time\n" +
                "• Contact the establishment directly for clarification\n" +
                "• Reach out to our support team for assistance\n" +
                "• Get personalized recommendations from our team\n\n" +
                "═══════════════════════════════════════\n" +
                "💬 NEED HELP?\n" +
                "═══════════════════════════════════════\n" +
                "Our support team is available 24/7 to:\n" +
                "• Help you find alternative venues\n" +
                "• Assist with rebooking\n" +
                "• Answer any questions about the rejection\n" +
                "• Provide personalized recommendations\n\n" +
                "We sincerely apologize for this inconvenience and any disruption to your plans. 🙏\n\n" +
                "We're committed to helping you find the perfect booking!\n\n" +
                "Best regards,\n" +
                "OpenNova Team\n" +
                "📧 abishekopennova@gmail.com\n" +
                "🌐 www.opennova.com",
                
                booking.getUser().getName(),
                booking.getEstablishment().getName(),
                booking.getEstablishment().getName(),
                booking.getVisitingDate(),
                booking.getVisitingTime(),
                booking.getId(),
                booking.getTransactionId(),
                reason != null ? reason : "No specific reason provided",
                itemsDetails,
                booking.getAmount().doubleValue(),
                booking.getPaymentAmount().doubleValue(),
                booking.getPaymentAmount().doubleValue(),
                booking.getEstablishment().getAddress() != null ? booking.getEstablishment().getAddress() : "Address not available",
                booking.getEstablishment().getContactNumber() != null ? booking.getEstablishment().getContactNumber() : "Phone not available",
                booking.getEstablishment().getEmail() != null ? booking.getEstablishment().getEmail() : "Email not available"
            );
            
            sendEmail(booking.getUserEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send booking rejection email: " + e.getMessage());
        }
    }

    @Async
    public void sendNewBookingNotificationToOwner(com.opennova.model.Booking booking) {
        try {
            String subject = "🔔 New Booking Received - " + booking.getEstablishment().getName();
            
            // Parse selected items for detailed display
            String itemsDetails = formatSelectedItems(booking.getSelectedItems(), booking.getEstablishment().getType().toString());
            
            String body = String.format(
                "Dear %s,\n\n" +
                "🔔 You have received a new booking!\n\n" +
                "═══════════════════════════════════════\n" +
                "📋 BOOKING DETAILS\n" +
                "═══════════════════════════════════════\n" +
                "👤 Customer: %s\n" +
                "📧 Customer Email: %s\n" +
                "📅 Date: %s\n" +
                "🕐 Time: %s\n" +
                "⏱️ Duration: 2 hours\n" +
                "🆔 Booking ID: #%d\n" +
                "💳 Transaction ID: %s\n" +
                "📊 Status: PENDING (Requires your confirmation)\n\n" +
                "═══════════════════════════════════════\n" +
                "🛍️ SELECTED ITEMS/SERVICES\n" +
                "═══════════════════════════════════════\n" +
                "%s\n" +
                "═══════════════════════════════════════\n" +
                "💰 PAYMENT SUMMARY\n" +
                "═══════════════════════════════════════\n" +
                "Total Amount: ₹%.2f\n" +
                "Received Amount (70%%): ₹%.2f ✅\n" +
                "Remaining Amount (30%%): ₹%.2f (To be collected at venue)\n" +
                "Payment Status: CONFIRMED ✅\n\n" +
                "⚡ ACTION REQUIRED:\n" +
                "1. Log in to your owner portal to review the booking\n" +
                "2. Confirm or reject the booking\n" +
                "3. Customer will receive QR code once confirmed\n\n" +
                "🔗 Owner Portal: %s/owner/bookings\n\n" +
                "⏰ Please respond within 24 hours to maintain good customer service.\n\n" +
                "Best regards,\n" +
                "OpenNova Team",
                
                booking.getEstablishment().getOwner().getName(),
                booking.getUser().getName(),
                booking.getUserEmail(),
                booking.getVisitingDate(),
                booking.getVisitingTime(),
                booking.getId(),
                booking.getTransactionId(),
                itemsDetails,
                booking.getAmount().doubleValue(),
                booking.getPaymentAmount().doubleValue(),
                booking.getAmount().doubleValue() - booking.getPaymentAmount().doubleValue(),
                "http://localhost:3000" // You can make this configurable
            );
            
            sendEmail(booking.getEstablishment().getOwner().getEmail(), subject, body);
            System.out.println("✅ Sent new booking notification to owner: " + booking.getEstablishment().getOwner().getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send new booking notification to owner: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
