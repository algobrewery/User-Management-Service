package com.userapi.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class EmailInfo {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String verificationStatus;

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}