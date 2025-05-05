// UpdateUserRequest.java
package com.userapi.models.external;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String phone; // e.g. "+9876543210"
    private String status;

    private CurrentJobProfile currentJobProfile;

    @Data
    public static class CurrentJobProfile {
        private String jobTitle;
        private LocalDateTime startDate;
        private LocalDateTime endDate; // nullable
        private String reportingManager;
        private List<String> reportees;
        private String organizationUnit;
        private Map<String, Object> extensionsData;
    }
}
