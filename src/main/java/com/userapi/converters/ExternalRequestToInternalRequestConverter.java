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
        return toInternal(
                RequestContext.builder()
                        .appOrgUuid(orgUUID)
                        .appUserUuid(userUUID)
                        .appClientUserSessionUuid(clientUserSessionUUID)
                        .traceId(traceID)
                        .regionId(regionID)
                        .build(),
                external);
    }

    protected abstract O toInternal(RequestContext rc, I external);
}
