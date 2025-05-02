package com.userapi.models.external;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.Min;

@Data
public class ListUsersRequest {
    @Valid
    private ListUsersFilterCriteria filterCriteria;
    private ListUsersSelector selector;

    @Min(value = 0, message = "Page number must be greater than or equal to 0")
    private Integer page = 0;

    @Min(value = 1, message = "Page size must be greater than 0")
    private Integer size = 10;

    private String sortBy = "username";
    private String sortDirection = "asc";
}
