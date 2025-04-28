package com.userapi.models.internal;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseRequest {
    @NonNull
    protected RequestContext requestContext;
}
