package com.opennova.service;

import com.opennova.model.Booking;
import com.opennova.model.User;
import com.opennova.repository.BookingRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    @Autowired
    private BookingRepository bookingRepository;

    public byte[] exportBookingsToExcel(Long ownerId, String reportType) throws IOException {
        List<Booking> bookings;
        
        switch (reportType.toUpperCase()) {
            case "OWNER":
                bookings = bookingRepository.findByEstablishmentOwnerIdOrderByCreatedAtDesc(ownerId);
                break;
            case "ALL":
                bookings = bookingRepository.findAllByOrderByCreatedAtDesc();
                break;
            default:
                bookings = bookingRepository.findByEstablishmentOwnerIdOrderByCreatedAtDesc(ownerId);
        }

        return generateExcelFile(bookings, reportType);
    }

    public byte[] exportAllBookingsToExcel() throws IOException {
        List<Booking> bookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        return generateExcelFile(bookings, "ALL");
    }

    private byte[] generateExcelFile(List<Booking> bookings, String reportType) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Booking Report");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Create data style
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setWrapText(true);

        // Create date style
        CellStyle dateStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Booking ID", "Customer Name", "Customer Email", "Establishment", 
            "Visiting Date", "Visiting Time", "Selected Items", "Amount", 
            "Payment Amount", "Status", "Payment Status", "Refund Status",
            "Transaction ID", "Created At", "Confirmed At", "Cancelled At"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Fill data rows
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Booking booking : bookings) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(booking.getId());
            row.createCell(1).setCellValue(booking.getUser() != null ? booking.getUser().getName() : "N/A");
            row.createCell(2).setCellValue(booking.getUserEmail());
            row.createCell(3).setCellValue(booking.getEstablishment() != null ? booking.getEstablishment().getName() : "N/A");
            row.createCell(4).setCellValue(booking.getVisitingDate());
            row.createCell(5).setCellValue(booking.getVisitingTime());
            row.createCell(6).setCellValue(booking.getSelectedItems());
            row.createCell(7).setCellValue(booking.getAmount() != null ? booking.getAmount().doubleValue() : 0.0);
            row.createCell(8).setCellValue(booking.getPaymentAmount() != null ? booking.getPaymentAmount().doubleValue() : 0.0);
            row.createCell(9).setCellValue(booking.getStatus() != null ? booking.getStatus().toString() : "N/A");
            row.createCell(10).setCellValue(booking.getPaymentStatus() != null ? booking.getPaymentStatus().toString() : "N/A");
            row.createCell(11).setCellValue(booking.getRefundStatus() != null ? booking.getRefundStatus().toString() : "N/A");
            row.createCell(12).setCellValue(booking.getTransactionId());
            row.createCell(13).setCellValue(booking.getCreatedAt() != null ? booking.getCreatedAt().format(formatter) : "N/A");
            row.createCell(14).setCellValue(booking.getConfirmedAt() != null ? booking.getConfirmedAt().format(formatter) : "N/A");
            row.createCell(15).setCellValue(booking.getCancelledAt() != null ? booking.getCancelledAt().format(formatter) : "N/A");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Add summary row
        Row summaryRow = sheet.createRow(rowNum + 2);
        Cell summaryCell = summaryRow.createCell(0);
        summaryCell.setCellValue("Total Bookings: " + bookings.size());
        summaryCell.setCellStyle(headerStyle);

        // Calculate total revenue
        double totalRevenue = bookings.stream()
            .filter(b -> b.getPaymentAmount() != null)
            .mapToDouble(b -> b.getPaymentAmount().doubleValue())
            .sum();

        Row revenueRow = sheet.createRow(rowNum + 3);
        Cell revenueCell = revenueRow.createCell(0);
        revenueCell.setCellValue("Total Revenue: ₹" + String.format("%.2f", totalRevenue));
        revenueCell.setCellStyle(headerStyle);

        // Add generation timestamp
        Row timestampRow = sheet.createRow(rowNum + 5);
        Cell timestampCell = timestampRow.createCell(0);
        timestampCell.setCellValue("Generated on: " + LocalDateTime.now().format(formatter));

        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    public byte[] exportEstablishmentReport(Long establishmentId) throws IOException {
        List<Booking> bookings = bookingRepository.findByEstablishmentIdAndStatus(
            establishmentId, null); // Get all statuses
        
        return generateExcelFile(bookings, "ESTABLISHMENT");
    }

    public byte[] exportUserBookings(Long userId) throws IOException {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return generateExcelFile(bookings, "USER");
    }
}