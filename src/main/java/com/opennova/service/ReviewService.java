package com.opennova.service;

import com.opennova.model.Review;
import com.opennova.model.Establishment;
import com.opennova.model.User;
import com.opennova.repository.ReviewRepository;
import com.opennova.repository.EstablishmentRepository;
import com.opennova.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private EstablishmentRepository establishmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    public Review createReview(Long userId, Long establishmentId, int rating, String comment) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new RuntimeException("Establishment not found"));

            Review review = new Review();
            review.setUser(user);
            review.setEstablishment(establishment);
            review.setRating(rating);
            review.setComment(comment);
            review.setStatus(com.opennova.model.ReviewStatus.PENDING);
            review.setCreatedAt(LocalDateTime.now());

            Review savedReview = reviewRepository.save(review);

            // Send notification to owner for approval
            notificationService.sendOwnerNotification(
                establishment.getOwner().getId(),
                "New Review Pending Approval",
                String.format("New %d-star review from %s requires your approval", rating, user.getName()),
                NotificationService.NotificationType.SYSTEM_ALERT
            );

            return savedReview;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create review: " + e.getMessage(), e);
        }
    }

    public List<Review> getEstablishmentReviews(Long establishmentId) {
        // Only return approved reviews for public display
        return reviewRepository.findByEstablishmentIdAndStatusOrderByCreatedAtDesc(
            establishmentId, com.opennova.model.ReviewStatus.APPROVED);
    }

    public List<Review> getPendingReviewsForOwner(Long ownerId) {
        // Get pending reviews for owner approval
        return reviewRepository.findByEstablishmentOwnerIdAndStatusOrderByCreatedAtDesc(
            ownerId, com.opennova.model.ReviewStatus.PENDING);
    }

    public List<Review> getAllReviewsForOwner(Long ownerId) {
        // Get all reviews (pending, approved, rejected) for owner
        return reviewRepository.findByEstablishmentOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    public List<Review> getOwnerReviews(Long ownerId) {
        return reviewRepository.findByEstablishmentOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    public boolean deleteReview(Long reviewId, Long requesterId, String requesterRole) {
        try {
            Optional<Review> reviewOpt = reviewRepository.findById(reviewId);
            if (!reviewOpt.isPresent()) {
                return false;
            }

            Review review = reviewOpt.get();

            // Check permissions
            if ("ADMIN".equals(requesterRole)) {
                // Admin can delete any review
            } else if ("OWNER".equals(requesterRole)) {
                // Owner can delete reviews for their establishments
                if (!review.getEstablishment().getOwner().getId().equals(requesterId)) {
                    throw new RuntimeException("Unauthorized to delete this review");
                }
            } else {
                // Users can only delete their own reviews
                if (!review.getUser().getId().equals(requesterId)) {
                    throw new RuntimeException("Unauthorized to delete this review");
                }
            }

            reviewRepository.delete(review);

            // Send notification
            if ("OWNER".equals(requesterRole) || "ADMIN".equals(requesterRole)) {
                notificationService.sendUserNotification(
                    review.getUser().getId(),
                    "Review Deleted",
                    "Your review has been deleted by " + requesterRole.toLowerCase(),
                    NotificationService.NotificationType.SYSTEM_ALERT
                );
            }

            return true;
        } catch (Exception e) {
            System.err.println("Failed to delete review: " + e.getMessage());
            return false;
        }
    }

    public Review updateReview(Long reviewId, Long userId, int rating, String comment) {
        try {
            Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

            if (!review.getUser().getId().equals(userId)) {
                throw new RuntimeException("Unauthorized to update this review");
            }

            review.setRating(rating);
            review.setComment(comment);
            review.setUpdatedAt(LocalDateTime.now());

            return reviewRepository.save(review);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update review: " + e.getMessage(), e);
        }
    }

    public double getAverageRating(Long establishmentId) {
        Double average = reviewRepository.getAverageRatingByEstablishmentId(establishmentId);
        return average != null ? average : 0.0;
    }

    public long getReviewCount(Long establishmentId) {
        return reviewRepository.countByEstablishmentId(establishmentId);
    }

    public long getTotalReviews() {
        return reviewRepository.count();
    }

    public Review approveReview(Long reviewId, Long ownerId) {
        try {
            Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

            // Verify owner has permission to approve this review
            if (!review.getEstablishment().getOwner().getId().equals(ownerId)) {
                throw new RuntimeException("Unauthorized to approve this review");
            }

            if (review.getStatus() != com.opennova.model.ReviewStatus.PENDING) {
                throw new RuntimeException("Review is not pending approval");
            }

            review.setStatus(com.opennova.model.ReviewStatus.APPROVED);
            review.setApprovedAt(LocalDateTime.now());

            Review savedReview = reviewRepository.save(review);

            // Send notification to user
            notificationService.sendUserNotification(
                review.getUser().getId(),
                "Review Approved",
                "Your review for " + review.getEstablishment().getName() + " has been approved and is now visible to other users",
                NotificationService.NotificationType.SYSTEM_ALERT
            );

            return savedReview;
        } catch (Exception e) {
            throw new RuntimeException("Failed to approve review: " + e.getMessage(), e);
        }
    }

    public Review rejectReview(Long reviewId, Long ownerId, String reason) {
        try {
            Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

            // Verify owner has permission to reject this review
            if (!review.getEstablishment().getOwner().getId().equals(ownerId)) {
                throw new RuntimeException("Unauthorized to reject this review");
            }

            if (review.getStatus() != com.opennova.model.ReviewStatus.PENDING) {
                throw new RuntimeException("Review is not pending approval");
            }

            review.setStatus(com.opennova.model.ReviewStatus.REJECTED);
            review.setRejectedAt(LocalDateTime.now());
            review.setRejectionReason(reason);

            Review savedReview = reviewRepository.save(review);

            // Send notification to user
            notificationService.sendUserNotification(
                review.getUser().getId(),
                "Review Rejected",
                "Your review for " + review.getEstablishment().getName() + " was not approved. Reason: " + reason,
                NotificationService.NotificationType.SYSTEM_ALERT
            );

            return savedReview;
        } catch (Exception e) {
            throw new RuntimeException("Failed to reject review: " + e.getMessage(), e);
        }
    }

    public List<Review> getUserReviews(Long userId) {
        try {
            return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } catch (Exception e) {
            System.err.println("Failed to get user reviews: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    public List<Review> getUserRecentReviews(Long userId, int limit) {
        try {
            List<Review> allReviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return allReviews.size() > limit ? allReviews.subList(0, limit) : allReviews;
        } catch (Exception e) {
            System.err.println("Failed to get user recent reviews: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    public List<java.util.Map<String, Object>> getAllReviewsForAdmin() {
        try {
            List<Review> reviews = reviewRepository.findAllByOrderByCreatedAtDesc();
            List<java.util.Map<String, Object>> reviewMaps = new java.util.ArrayList<>();
            
            for (Review review : reviews) {
                java.util.Map<String, Object> reviewMap = new java.util.HashMap<>();
                reviewMap.put("id", review.getId());
                reviewMap.put("customerName", review.getUser().getName());
                reviewMap.put("rating", review.getRating());
                reviewMap.put("comment", review.getComment());
                reviewMap.put("establishmentName", review.getEstablishment().getName());
                reviewMap.put("status", review.getStatus().toString());
                reviewMap.put("createdAt", review.getCreatedAt().toString());
                reviewMap.put("updatedAt", review.getUpdatedAt() != null ? review.getUpdatedAt().toString() : null);
                reviewMap.put("approvedAt", review.getApprovedAt() != null ? review.getApprovedAt().toString() : null);
                reviewMap.put("rejectedAt", review.getRejectedAt() != null ? review.getRejectedAt().toString() : null);
                reviewMap.put("rejectionReason", review.getRejectionReason());
                reviewMaps.add(reviewMap);
            }
            
            return reviewMaps;
        } catch (Exception e) {
            System.err.println("Failed to get reviews for admin: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}