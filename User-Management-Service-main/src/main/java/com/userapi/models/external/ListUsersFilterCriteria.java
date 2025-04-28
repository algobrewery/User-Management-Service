package com.userapi.models.external;

import lombok.Data;
import java.util.List;

@Data
public class ListUsersFilterCriteria {
    private List<ListUsersFilterCriteriaAttribute> attributes;
}
