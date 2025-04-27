package com.userapi.models.external;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class PhoneInfo {
    @NotBlank(message = "Phone number is required")
    private String number;

    @NotNull(message = "Country code is required")
    private Integer countryCode;

    private String verificationStatus;

}