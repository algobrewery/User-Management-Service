package com.userapi.models.external;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;

@SuperBuilder
@Getter
@Setter
public class BaseResponse {

    @NonNull
    private HttpStatus httpStatus;

}
