package com.userapi.models.internal;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public class UpdateUserInternalRequest extends BaseInternalRequest {

    private String username;
    private String firstName;
    private String middleName;
    private String lastName;
    private String status;
    private PhoneInfoDto phoneInfo;
    private EmailInfoDto emailInfo;
    private EmploymentInfoDto employmentInfo;

}
