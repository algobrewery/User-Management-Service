package com.userapi.converters;

import com.userapi.models.internal.BaseInternalResponse;
import com.userapi.models.internal.ResponseReasonCode;
import org.springframework.http.HttpStatus;

public interface InternalResponseToExternalResponseConverter<I extends BaseInternalResponse, O extends com.userapi.models.external.BaseResponse> {

    O toExternal(I internal);

    default HttpStatus mapHttpStatus(ResponseReasonCode responseReasonCode) {
        return switch (responseReasonCode) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case INTERNAL_SERVER_ERROR, UNKNOWN -> HttpStatus.INTERNAL_SERVER_ERROR;
            case EXTERNAL_ERROR -> HttpStatus.FAILED_DEPENDENCY;
            default -> HttpStatus.OK;
        };
    }

}
