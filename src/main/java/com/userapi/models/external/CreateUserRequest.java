package com.userapi.models.external;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Setter
@Getter
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Valid
    @NotNull(message = "Phone information is required")
    private PhoneInfo phoneInfo;

    @Valid
    @NotNull(message = "Email information is required")
    private EmailInfo emailInfo;

    @Valid
    @NotEmpty(message = "At least one employment information is required")
    private List<EmploymentInfo> employmentInfo;

}