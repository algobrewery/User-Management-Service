package com.userapi.models.external.roles;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;

@Data
public class ListRolesRequest {
    @Valid
    private ListRolesFilterCriteria filterCriteria;
    private ListRolesSelector selector;

    @Min(value = 0, message = "Page number must be greater than or equal to 0")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be greater than 0")
    private Integer size = 10;

    private String sortBy = "name";
    private String sortDirection = "asc";
}
