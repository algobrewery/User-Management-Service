package com.userapi.models.internal;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public abstract class BaseInternalResponse {

    @NonNull
    protected ResponseResult responseResult;

    @NonNull
    protected ResponseReasonCode responseReasonCode;

    protected String message;
}
