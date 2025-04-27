package com.userapi.models.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class CreateUserInternalRequest extends BaseRequest {

    @NonNull
    private String username;

    @NonNull
    private String firstName;

    private String middleName;

    @NonNull
    private String lastName;

    @NonNull
    private PhoneInfoDto phoneInfo;

    @NonNull
    private EmailInfoDto emailInfo;

    @NonNull
    private List<EmploymentInfoDto> employmentInfo;

}
