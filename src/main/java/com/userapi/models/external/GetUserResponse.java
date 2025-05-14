package com.userapi.models.external;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class GetUserResponse extends BaseResponse {
    private String userId;
    private String username;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private List<JobProfileInfo> jobProfiles;
}
