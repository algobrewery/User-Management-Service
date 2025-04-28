package com.userapi.models.external;

import lombok.Data;
import java.util.List;

@Data
public class ListUsersFilterCriteriaAttribute {
    private String name;
    private List<String> values;
}
