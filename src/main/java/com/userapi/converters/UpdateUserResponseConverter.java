package com.userapi.converters;

import com.userapi.models.external.UpdateUserResponse;
import com.userapi.models.internal.UpdateUserInternalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("UpdateUserResponseConverter")
public class UpdateUserResponseConverter
        extends InternalResponseToExternalResponseConverter<UpdateUserInternalResponse, UpdateUserResponse> {

    @Override
    protected UpdateUserResponse convert(UpdateUserInternalResponse internal) {
        if (Objects.isNull(internal)) {
            return null;
        }
        return UpdateUserResponse.builder()
                .userId(internal.getUserId())
                .status(internal.getStatus())
                .message(internal.getMessage())
                .httpStatus(HttpStatus.OK) // Example of using a static value
                .build();
    }

}
