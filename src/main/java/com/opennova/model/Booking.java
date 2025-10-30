package com.opennova.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "bookings"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establishment_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "bookings", "owner"})
    private Establishment establishment;

    @Column(name = "booking_date")
    private LocalDateTime bookingDate;

    @Column(name = "booking_time")
    private String bookingTime;

    @Column(name = "visiting_hours")
    private Integer visitingHours = 2;

    @NotNull
    @Positive
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "paid_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal paymentAmount; // 70% of total amount

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus = RefundStatus.NOT_APPLICABLE;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "item_details", columnDefinition = "TEXT")
    private String itemDetails; // JSON string for menu/doctor/collection details

    // Payment screenshot removed - now using secure UPI transaction verification

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "visiting_date")
    private String visitingDate;

    @Column(name = "visiting_time")
    private String visitingTime;

    @Column(name = "selected_items", columnDefinition = "TEXT")
    private String selectedItems;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    // Constructors
    public Booking() {}

    public Booking(User user, Establishment establishment, LocalDateTime bookingDate, BigDecimal amount) {
        this.user = user;
        this.establishment = establishment;
        this.bookingDate = bookingDate;
        this.amount = amount;
        this.paymentAmount = amount != null ? amount.multiply(new BigDecimal("0.7")) : BigDecimal.ZERO; // 70% payment
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Establishment getEstablishment() { return establishment; }
    public void setEstablishment(Establishment establishment) { this.establishment = establishment; }

    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }

    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }

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

    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public String getItemDetails() { return itemDetails; }
    public void setItemDetails(String itemDetails) { this.itemDetails = itemDetails; }

    // Payment screenshot methods removed - using secure UPI verification instead

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getVisitingDate() { return visitingDate; }
    public void setVisitingDate(String visitingDate) { this.visitingDate = visitingDate; }

    public String getVisitingTime() { return visitingTime; }
    public void setVisitingTime(String visitingTime) { this.visitingTime = visitingTime; }

    public String getSelectedItems() { return selectedItems; }
    public void setSelectedItems(String selectedItems) { this.selectedItems = selectedItems; }

    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    // Helper methods for frontend
    public String getCustomerName() {
        return user != null ? user.getName() : null;
    }

    public String getCustomerEmail() {
        return user != null ? user.getEmail() : null;
    }

    public String getEstablishmentName() {
        return establishment != null ? establishment.getName() : null;
    }

    public String getEstablishmentType() {
        return establishment != null ? establishment.getType().toString() : null;
    }

    public String getEstablishmentAddress() {
        return establishment != null ? establishment.getAddress() : null;
    }

    // Convert BigDecimal to Double for frontend compatibility
    @Transient
    public Double getTotalAmount() {
        return amount != null ? amount.doubleValue() : null;
    }

    public void setTotalAmount(Double totalAmount) {
        this.amount = totalAmount != null ? BigDecimal.valueOf(totalAmount) : null;
    }

    @Transient
    public Double getPaidAmount() {
        return paymentAmount != null ? paymentAmount.doubleValue() : null;
    }

    public void setPaidAmount(Double paidAmount) {
        this.paymentAmount = paidAmount != null ? BigDecimal.valueOf(paidAmount) : null;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Booking{" +
                "id=" + id +
                ", userEmail='" + userEmail + '\'' +
                ", visitingDate='" + visitingDate + '\'' +
                ", visitingTime='" + visitingTime + '\'' +
                ", amount=" + amount +
                ", paymentAmount=" + paymentAmount +
                ", transactionId='" + transactionId + '\'' +
                ", status=" + status +
                ", paymentStatus=" + paymentStatus +
                ", refundStatus=" + refundStatus +
                '}';
    }
}