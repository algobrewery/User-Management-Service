package com.userapi.models.internal;

import com.userapi.models.entity.JobProfile;
import com.userapi.models.entity.UserProfile;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@SuperBuilder
@Getter
@Setter
public class GetUserInternalResponse extends BaseInternalResponse {

    private UserProfile userProfile;
    private Map<String, JobProfile> jobProfilesByUuid;
    private Map<String, List<String>> reporteesByJobProfileUuid;
}
