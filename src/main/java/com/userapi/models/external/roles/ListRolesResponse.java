package com.userapi.models.external.roles;

import lombok.Data;

import java.util.List;

@Data
public class ListRolesResponse {
    private List<RoleResponse> roles;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
}
