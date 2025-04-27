package com.userapi.models.external;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class JobProfileInfo {
    private String jobTitle;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String reportingManager;
    private List<String> reportees;
    private String organizationUnit;
    private JsonNode extensionsData;

}