package com.userapi.models.external;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class EmailInfo {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String verificationStatus;

}