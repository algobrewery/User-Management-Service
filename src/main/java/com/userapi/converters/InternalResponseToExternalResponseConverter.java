package com.userapi.converters;

import com.userapi.models.external.BaseResponse;
import com.userapi.models.internal.BaseInternalResponse;
import com.userapi.models.internal.ResponseReasonCode;
import org.springframework.http.HttpStatus;

import java.util.Objects;

public abstract class InternalResponseToExternalResponseConverter<I extends BaseInternalResponse, O extends com.userapi.models.external.BaseResponse> {

    public O toExternal(I internal) {
        if (Objects.isNull(internal)) {
            return null;
        }
        O external = convert(internal);
        external.setHttpStatus(convertBase(internal).getHttpStatus());
        return external;
    }

    protected abstract O convert(I internal);

    private BaseResponse convertBase(BaseInternalResponse internalResponse) {
        return BaseResponse.builder().httpStatus(mapHttpStatus(internalResponse.getResponseReasonCode())).build();
    }

    private HttpStatus mapHttpStatus(ResponseReasonCode responseReasonCode) {
        return switch (responseReasonCode) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case INTERNAL_SERVER_ERROR, UNKNOWN -> HttpStatus.INTERNAL_SERVER_ERROR;
            case EXTERNAL_ERROR -> HttpStatus.FAILED_DEPENDENCY;
            default -> HttpStatus.OK;
        };
    }

}
