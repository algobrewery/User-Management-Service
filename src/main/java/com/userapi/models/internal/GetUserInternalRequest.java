package com.userapi.models.internal;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public class GetUserInternalRequest extends BaseInternalRequest {

    @NonNull
    private String userId;

}
