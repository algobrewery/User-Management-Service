package com.userapi.models.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Builder
@Getter
@Setter
public class RequestContext {

    @NonNull
    private String appUserUuid;

    @NonNull
    private String appOrgUuid;

    @NonNull
    private String appClientUserSessionUuid;

    @NonNull
    private String traceId;

    @NonNull
    private String regionId;

}
