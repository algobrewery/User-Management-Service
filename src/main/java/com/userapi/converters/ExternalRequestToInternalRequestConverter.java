package com.userapi.converters;

import com.userapi.models.internal.BaseInternalRequest;
import com.userapi.models.internal.RequestContext;

public abstract class ExternalRequestToInternalRequestConverter<I, O extends BaseInternalRequest> {

    public O toInternal(
            String orgUUID,
            String userUUID,
            String clientUserSessionUUID,
            String traceID,
            String regionID,
            final I external) {
        O internalRequest = toInternal(external);
        internalRequest.setRequestContext(RequestContext.builder()
                .appOrgUuid(orgUUID)
                .appUserUuid(userUUID)
                .appClientUserSessionUuid(clientUserSessionUUID)
                .traceId(traceID)
                .regionId(regionID)
                .build());
        return internalRequest;
    }

    protected abstract O toInternal(I external);
}
