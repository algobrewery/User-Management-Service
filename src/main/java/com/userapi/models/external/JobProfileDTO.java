package com.userapi.models.external;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class JobProfileDTO {
    private String jobTitle;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String reportingManager;
    private List<String> reportees;
    private String organizationUnit;
    private Map<String, Object> extensionsData;
}
