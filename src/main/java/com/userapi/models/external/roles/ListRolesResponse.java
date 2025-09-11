package com.userapi.models.external.roles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListRolesResponse {
    private List<RoleResponse> roles;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
