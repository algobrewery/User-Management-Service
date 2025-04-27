package com.userapi.models.internal;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseResponse {

    @NonNull
    protected ResponseResult responseResult;

    @NonNull
    protected ResponseReasonCode responseReasonCode;

    protected String message;
}
