package com.userapi.models.external;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public class CreateUserResponse extends BaseResponse {

    private String userId;
    private String username;
    private String status;
    private String message;

}
