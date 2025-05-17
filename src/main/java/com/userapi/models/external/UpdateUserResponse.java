package com.userapi.models.external;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class UpdateUserResponse extends BaseResponse {
    private String userId;
    private String message;
    private String status;
}
