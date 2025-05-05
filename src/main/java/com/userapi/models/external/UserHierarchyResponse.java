package com.userapi.models.external;

import lombok.Data;

import java.util.List;

@Data
public class UserHierarchyResponse {
    private String userId;
    private UserInfo reportingManager;
    private List<UserInfo> reportees;

    @Data
    public static class UserInfo {
        private String userId;
        private String name;
    }
}
