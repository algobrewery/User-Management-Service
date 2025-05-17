package com.userapi.models.external;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String username;
    private String firstName;
    private String middleName;
    private String lastName;
    private String status;
    private PhoneInfo phoneInfo;
    private EmailInfo emailInfo;
    private EmploymentInfo employmentInfo;
}
