package com.userapi.models.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PhoneInfoDto {

    @NonNull
    private String number;

    @NonNull
    private Integer countryCode;

    private String verificationStatus;

}
