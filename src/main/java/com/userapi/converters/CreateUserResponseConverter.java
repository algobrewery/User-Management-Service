package com.userapi.converters;

import com.userapi.models.external.CreateUserResponse;
import com.userapi.models.internal.CreateUserInternalResponse;
import com.userapi.models.internal.ResponseReasonCode;
import com.userapi.models.internal.ResponseResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("CreateUserResponseConverter")
public class CreateUserResponseConverter
        extends InternalResponseToExternalResponseConverter<CreateUserInternalResponse, CreateUserResponse> {

    @Override
    protected CreateUserResponse convert(CreateUserInternalResponse internal) {
        if (Objects.isNull(internal)) {
            return null;
        }

        HttpStatus httpStatus;
        if (internal.getResponseResult() == ResponseResult.SUCCESS) {
            httpStatus = HttpStatus.CREATED;
        } else if (internal.getResponseReasonCode() == ResponseReasonCode.DUPLICATE_USER) {
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (internal.getResponseReasonCode() == ResponseReasonCode.ENTITY_NOT_FOUND) {
            httpStatus = HttpStatus.NOT_FOUND;
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // Create the inner response
        CreateUserResponse innerResponse = CreateUserResponse.builder()
                .userId(internal.getUserId())
                .username(internal.getUsername())
                .status(internal.getStatus())
                .message(internal.getMessage())
                .httpStatus(httpStatus)
                .build();

        // Create the outer response with the same HTTP status
        return CreateUserResponse.builder()
                .result(innerResponse)
                .httpStatus(httpStatus)
                .message(internal.getMessage())
                .build();
    }
}