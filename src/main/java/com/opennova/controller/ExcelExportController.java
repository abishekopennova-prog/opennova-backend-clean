package com.opennova.controller;

import com.opennova.model.User;
import com.opennova.service.ExcelExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3002", "http://127.0.0.1:3002", "http://localhost:3003", "http://127.0.0.1:3003"})
public class ExcelExportController {

    @Autowired
    private ExcelExportService excelExportService;

    @GetMapping("/admin/export/all-bookings")
    public ResponseEntity<?> exportAllBookings() {
        try {
            byte[] excelData = excelExportService.exportAllBookingsToExcel();
            
            String filename = "all-bookings-" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Excel export error: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to export bookings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/owner/export/bookings")
    public ResponseEntity<?> exportOwnerBookings(Authentication authentication) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User owner = userPrincipal.getUser();
            
            byte[] excelData = excelExportService.exportBookingsToExcel(owner.getId(), "OWNER");
            
            String filename = "owner-bookings-" + owner.getId() + "-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Owner Excel export error: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to export owner bookings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/user/export/bookings")
    public ResponseEntity<?> exportUserBookings(Authentication authentication) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();
            
            byte[] excelData = excelExportService.exportUserBookings(user.getId());
            
            String filename = "my-bookings-" + user.getId() + "-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("User Excel export error: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to export user bookings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/owner/export/establishment/{establishmentId}")
    public ResponseEntity<?> exportEstablishmentBookings(@PathVariable Long establishmentId, Authentication authentication) {
        try {
            com.opennova.security.CustomUserDetailsService.CustomUserPrincipal userPrincipal = 
                (com.opennova.security.CustomUserDetailsService.CustomUserPrincipal) authentication.getPrincipal();
            User owner = userPrincipal.getUser();
            
            byte[] excelData = excelExportService.exportEstablishmentReport(establishmentId);
            
            String filename = "establishment-" + establishmentId + "-bookings-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Establishment Excel export error: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "Failed to export establishment bookings: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}