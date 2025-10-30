package com.opennova.service;

import com.opennova.model.Booking;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QRCodeService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateBookingQRCode(Booking booking) {
        try {
            // Create comprehensive QR code data with all booking details
            Map<String, Object> qrData = new HashMap<>();
            qrData.put("bookingId", booking.getId());
            qrData.put("establishmentId", booking.getEstablishment().getId());
            qrData.put("customerName", booking.getUser() != null ? booking.getUser().getName() : "Guest");
            qrData.put("customerEmail", booking.getUserEmail());
            qrData.put("establishmentName", booking.getEstablishment().getName());
            qrData.put("visitingDate", booking.getVisitingDate() != null ? booking.getVisitingDate().toString() : "");
            qrData.put("visitingTime", booking.getVisitingTime() != null ? booking.getVisitingTime() : "");
            qrData.put("totalAmount", booking.getAmount() != null ? booking.getAmount().doubleValue() : 0.0);
            qrData.put("paidAmount", booking.getPaymentAmount() != null ? booking.getPaymentAmount().doubleValue() : 0.0);
            qrData.put("status", booking.getStatus().toString());
            qrData.put("transactionId", booking.getTransactionId());
            qrData.put("selectedItems", booking.getSelectedItems());
            qrData.put("itemDetails", booking.getItemDetails());
            qrData.put("bookingDate", booking.getBookingDate() != null ? booking.getBookingDate().toString() : "");
            qrData.put("type", "OPENNOVA_BOOKING");
            qrData.put("version", "3.0");

            String qrText = objectMapper.writeValueAsString(qrData);
            
            System.out.println("🔄 Generating optimized QR code for booking: " + booking.getId());
            System.out.println("📱 QR data length: " + qrText.length() + " characters");
            
            // Generate QR code image with optimized settings
            return generateQRCodeImage(qrText);
        } catch (Exception e) {
            System.err.println("❌ Failed to generate QR code: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage());
        }
    }

    private String generateQRCodeImage(String text) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        
        // Optimized settings for faster scanning and smaller size
        Map<com.google.zxing.EncodeHintType, Object> hints = new HashMap<>();
        hints.put(com.google.zxing.EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M); // Medium error correction for balance
        hints.put(com.google.zxing.EncodeHintType.MARGIN, 1); // Smaller margin for compact size
        hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");
        
        // Optimized size - smaller for faster generation and scanning
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300, hints);
        
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        // Convert to Base64 with optimized compression
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();
        
        String base64QR = Base64.getEncoder().encodeToString(imageBytes);
        System.out.println("✅ Optimized QR code generated (size: " + imageBytes.length + " bytes, " + 
                          (imageBytes.length < 10000 ? "FAST" : "LARGE") + " scan speed)");
        
        return base64QR;
    }

    public Map<String, Object> parseQRCode(String qrData) {
        try {
            return objectMapper.readValue(qrData, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse QR code data: " + e.getMessage());
        }
    }

    public boolean validateQRCode(String qrData, Long bookingId) {
        try {
            Map<String, Object> data = parseQRCode(qrData);
            
            // Check if it's a valid OpenNova booking QR code
            String type = (String) data.get("type");
            if (!"OPENNOVA_BOOKING".equals(type)) {
                return false;
            }
            
            Long qrBookingId = Long.valueOf(data.get("bookingId").toString());
            return qrBookingId.equals(bookingId);
        } catch (Exception e) {
            System.err.println("QR validation error: " + e.getMessage());
            return false;
        }
    }
}