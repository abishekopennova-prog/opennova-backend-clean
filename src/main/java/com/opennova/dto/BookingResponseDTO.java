package com.opennova.dto;

import com.opennova.model.Booking;
import com.opennova.model.BookingStatus;
import com.opennova.model.PaymentStatus;
import com.opennova.model.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookingResponseDTO {
    private Long id;
    private String customerName;
    private String customerEmail;
    private String establishmentName;
    private Long establishmentId;
    private String visitingDate;
    private String visitingTime;
    private Integer visitingHours;
    private BigDecimal amount;
    private BigDecimal paymentAmount;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private RefundStatus refundStatus;
    private String transactionId;
    private String selectedItems;
    // Payment screenshot removed - using secure UPI verification
    private String qrCode;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;

    // Constructor from Booking entity
    public BookingResponseDTO(Booking booking) {
        this.id = booking.getId();
        this.customerName = booking.getUser() != null ? booking.getUser().getName() : "Unknown";
        this.customerEmail = booking.getUserEmail();
        this.establishmentName = booking.getEstablishment() != null ? booking.getEstablishment().getName() : "Unknown";
        this.establishmentId = booking.getEstablishment() != null ? booking.getEstablishment().getId() : null;
        this.visitingDate = booking.getVisitingDate();
        this.visitingTime = booking.getVisitingTime();
        this.visitingHours = booking.getVisitingHours();
        this.amount = booking.getAmount();
        this.paymentAmount = booking.getPaymentAmount();
        this.status = booking.getStatus();
        this.paymentStatus = booking.getPaymentStatus();
        this.refundStatus = booking.getRefundStatus();
        this.transactionId = booking.getTransactionId();
        this.selectedItems = booking.getSelectedItems();
        // Payment screenshot removed - using secure UPI verification
        this.qrCode = booking.getQrCode();
        this.cancellationReason = booking.getCancellationReason();
        this.createdAt = booking.getCreatedAt();
        this.confirmedAt = booking.getConfirmedAt();
        this.cancelledAt = booking.getCancelledAt();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getEstablishmentName() { return establishmentName; }
    public void setEstablishmentName(String establishmentName) { this.establishmentName = establishmentName; }

    public Long getEstablishmentId() { return establishmentId; }
    public void setEstablishmentId(Long establishmentId) { this.establishmentId = establishmentId; }

    public String getVisitingDate() { return visitingDate; }
    public void setVisitingDate(String visitingDate) { this.visitingDate = visitingDate; }

    public String getVisitingTime() { return visitingTime; }
    public void setVisitingTime(String visitingTime) { this.visitingTime = visitingTime; }

    public Integer getVisitingHours() { return visitingHours; }
    public void setVisitingHours(Integer visitingHours) { this.visitingHours = visitingHours; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public RefundStatus getRefundStatus() { return refundStatus; }
    public void setRefundStatus(RefundStatus refundStatus) { this.refundStatus = refundStatus; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getSelectedItems() { return selectedItems; }
    public void setSelectedItems(String selectedItems) { this.selectedItems = selectedItems; }

    // Payment screenshot methods removed - using secure UPI verification

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}