package com.userapi.converters;

import com.userapi.models.internal.GetUserInternalRequest;
import com.userapi.models.internal.RequestContext;
import org.springframework.stereotype.Component;

@Component("GetUserRequestConverter")
public class GetUserRequestConverter
        extends ExternalRequestToInternalRequestConverter<String, GetUserInternalRequest> {

    @Override
    protected GetUserInternalRequest toInternal(RequestContext rc, String external) {
        return GetUserInternalRequest.builder()
                .userId(external)
                .requestContext(rc)
                .build();
    }
}
