package com.userapi.models.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Builder
@Getter
@Setter
public class EmailInfoDto {

    @NonNull
    private String email;

    private String verificationStatus;

}
