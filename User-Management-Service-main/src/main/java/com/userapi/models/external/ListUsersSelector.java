package com.userapi.models.external;

import lombok.Data;
import java.util.List;

@Data
public class ListUsersSelector {
    private List<String> base_attributes;
    private List<String> extensions;
}
