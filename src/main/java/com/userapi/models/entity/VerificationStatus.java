package com.userapi.models.entity;

import lombok.Getter;

/**
 * Enum representing the verification status of contact information like email and phone.
 */
@Getter
public enum VerificationStatus {
    VERIFIED("VERIFIED"),
    UNVERIFIED("UNVERIFIED"),
    PENDING("PENDING");

    private final String value;

    VerificationStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Convert a string to the corresponding VerificationStatus enum value.
     * Case-insensitive matching.
     *
     * @param status the string representation of the status
     * @return the corresponding VerificationStatus enum value, or UNVERIFIED if not found
     */
    public static VerificationStatus fromString(String status) {
        if (status == null) {
            return UNVERIFIED;
        }

        String upperCaseStatus = status.toUpperCase();
        for (VerificationStatus verificationStatus : values()) {
            if (verificationStatus.value.equals(upperCaseStatus)) {
                return verificationStatus;
            }
        }

        // Default to UNVERIFIED if the status is not recognized
        return UNVERIFIED;
    }
}
