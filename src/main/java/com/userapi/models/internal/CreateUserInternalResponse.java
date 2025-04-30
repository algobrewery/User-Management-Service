package com.userapi.models.internal;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public class CreateUserInternalResponse extends BaseInternalResponse {

    private String userId;
    private String username;
    private String status;

}
