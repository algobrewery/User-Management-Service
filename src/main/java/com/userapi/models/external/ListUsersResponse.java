package com.userapi.models.external;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ListUsersResponse {
    private List<Map<String, Object>> users;
}

