package com.userapi.models.external;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PhoneInfo {
    @NotBlank(message = "Phone number is required")
    private String number;

    @NotNull(message = "Country code is required")
    private Integer countryCode;

    @NotBlank(message = "Verification status is required")
    private String verificationStatus;
}
