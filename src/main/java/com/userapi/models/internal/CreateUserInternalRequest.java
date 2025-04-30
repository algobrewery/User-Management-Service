package com.userapi.models.internal;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
@Setter
public class CreateUserInternalRequest extends BaseInternalRequest {

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
    private List<EmploymentInfoDto> employmentInfoList;

}
