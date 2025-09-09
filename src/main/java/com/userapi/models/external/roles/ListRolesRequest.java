package com.userapi.models.external.roles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.Min;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListRolesRequest {
    @Valid
    private ListRolesFilterCriteria filterCriteria;
    private ListRolesSelector selector;

    @Min(value = 0, message = "Page number must be greater than or equal to 0")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be greater than 0")
    private Integer size = 10;

    private String sortBy = "roleName";
    private String sortDirection = "asc";
}
