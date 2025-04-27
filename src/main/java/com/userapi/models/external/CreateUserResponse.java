package com.userapi.models.external;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserResponse {

    private String userId;
    private String username;
    private String status;
    private String message;

}