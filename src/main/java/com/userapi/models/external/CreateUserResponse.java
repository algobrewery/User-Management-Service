package com.userapi.models.external;

import com.userapi.models.internal.ResponseReasonCode;
import com.userapi.models.internal.ResponseResult;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;

@SuperBuilder
@Getter
@Setter
public class CreateUserResponse extends BaseResponse {

    private String userId;
    private String username;
    private String status;
    private String message;
}
