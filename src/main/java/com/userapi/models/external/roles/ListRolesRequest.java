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

    @Min(value = 1, message = "Page number must be greater than or equal to 1")
    @Builder.Default
    private Integer page = 1;

    @Min(value = 1, message = "Page size must be greater than 0")
    @Builder.Default
    private Integer size = 10;

    @Builder.Default
    private String sortBy = "roleName";
    
    @Builder.Default
    private String sortDirection = "asc";
}
