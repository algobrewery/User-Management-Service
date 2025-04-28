package com.userapi.models.internal;

import lombok.Getter;
import lombok.NonNull;

import java.util.Collections;
import java.util.Map;

@Getter
public class RequestContext {
    @NonNull
    private final Map<String, String> headers;

    public RequestContext(@NonNull final Map<String, String> headers) {
        this.headers = Collections.unmodifiableMap(headers);
    }
}
