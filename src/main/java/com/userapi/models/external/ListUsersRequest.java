package com.userapi.models.external;

import lombok.Data;

@Data
public class ListUsersRequest {
    private ListUsersFilterCriteria filterCriteria;
    private ListUsersSelector selector;
}