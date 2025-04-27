package com.userapi.models.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class CreateUserInternalResponse extends BaseResponse {

    private String userId;
    private String username;
    private String status;

}
