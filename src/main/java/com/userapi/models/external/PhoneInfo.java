package com.userapi.models.external;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class PhoneInfo {
    @NotBlank(message = "Phone number is required")
    private String number;

    @NotNull(message = "Country code is required")
    private Integer countryCode;

    private String verificationStatus;

    // Getters and Setters
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Integer getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(Integer countryCode) {
        this.countryCode = countryCode;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}