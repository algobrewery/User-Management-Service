package com.userapi.models.external;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private String status;
    private String phone; // <-- Add this line
    private Integer phoneCountryCode; // <-- Add this if you want to update country code

    private CurrentJobProfile currentJobProfile;

    @Data
    public static class CurrentJobProfile {
        private String jobTitle;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String reportingManager; // <-- manager's user UUID
        private String organizationUnit;
        private Map<String, Object> extensionsData;
        private List<String> reportees; // <-- list of reportee user UUIDs
    }
}
