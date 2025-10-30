package com.opennova.model;

public enum ReviewStatus {
    PENDING,    // Review is waiting for approval
    APPROVED,   // Review has been approved and is visible to users
    REJECTED    // Review has been rejected and is not visible
}