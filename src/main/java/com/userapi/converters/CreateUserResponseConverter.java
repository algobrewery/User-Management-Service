package com.userapi.converters;

import com.userapi.models.external.CreateUserResponse;
import com.userapi.models.internal.CreateUserInternalResponse;
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
        return CreateUserResponse.builder()
                .userId(internal.getUserId())
                .username(internal.getUsername())
                .status(internal.getStatus())
                .message(internal.getMessage())
                .httpStatus(HttpStatus.OK) // Example of using a static value
                .build();
    }


}
