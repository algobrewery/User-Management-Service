package com.userapi.models.internal;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public abstract class BaseInternalRequest {
    @NonNull
    protected RequestContext requestContext;
}
